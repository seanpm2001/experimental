/***************************************************************************
 *   Copyright (C) 2006-2009 by Fabrizio Montesi <famontesi@gmail.com>     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.net;

import jolie.net.ports.OutputPort;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.JolieThread;
import jolie.lang.Constants;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.CommProtocol;
import jolie.process.Process;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.TimeoutHandler;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.correlation.CorrelationError;
import jolie.runtime.typing.TypeCheckingException;

/** 
 * Handles the communications mechanisms for an Interpreter instance.
 * 
 * Each CommCore is related to an Interpreter, and each Interpreter owns one and only CommCore instance.
 *
 * @author Fabrizio Montesi
 */
public class CommCore
{
	private final Map< String, CommListener > listenersMap = new HashMap< String, CommListener >();
	private final static int CHANNEL_HANDLER_TIMEOUT = 5;
	private final ThreadGroup threadGroup;

	private static final Logger logger = Logger.getLogger( "JOLIE" );

	private final int connectionsLimit;
	// private final int connectionCacheSize;
	private final Interpreter interpreter;
	
	private final ReadWriteLock channelHandlersLock = new ReentrantReadWriteLock( true );

	// Location URI -> Protocol name -> Persistent CommChannel object
	private final Map< URI, Map< String, CommChannel > > persistentChannels =
			new HashMap< URI, Map< String, CommChannel > >();

	private void removePersistentChannel( URI location, String protocol, Map< String, CommChannel > protocolChannels )
	{		
		protocolChannels.remove( protocol );
		if ( protocolChannels.isEmpty() ) {
			persistentChannels.remove( location );
		}
	}
	
	private void removePersistentChannel( URI location, String protocol, CommChannel channel )
	{
		if ( persistentChannels.containsKey( location ) ) {
			if ( persistentChannels.get( location ).get( protocol ) == channel ) {
				removePersistentChannel( location, protocol, persistentChannels.get( location ) );
			}
		}
	}

	public CommChannel getPersistentChannel( URI location, String protocol )
	{
		CommChannel ret = null;
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if ( protocolChannels != null ) {
				ret = protocolChannels.get( protocol );
				if ( ret != null ) {
					if ( ret.lock.tryLock() ) {
						if ( ret.isOpen() ) {
							/*
							 * We are going to return this channel, but first
							 * check if it supports concurrent use.
							 * If not, then others should not access this until
							 * the caller is finished using it.
							 */
							//if ( ret.isThreadSafe() == false ) {
							removePersistentChannel( location, protocol, protocolChannels );
							//} else {
							// If we return a channel, make sure it will not timeout!
							ret.setTimeoutHandler( null );
							//if ( ret.timeoutHandler() != null ) {
								//interpreter.removeTimeoutHandler( ret.timeoutHandler() );
								// ret.setTimeoutHandler( null );
							//}
							//}
							ret.lock.unlock();
						} else { // Channel is closed
							removePersistentChannel( location, protocol, protocolChannels );
							ret.lock.unlock();
							ret = null;
						}
					} else { // Channel is busy
						removePersistentChannel( location, protocol, protocolChannels );
						ret = null;
					}
				}
			}
		}

		return ret;
	}

	private void setTimeoutHandler( final CommChannel channel, final URI location, final String protocol )
	{
		/*if ( channel.timeoutHandler() != null ) {
			interpreter.removeTimeoutHandler( channel.timeoutHandler() );
		}*/

		final TimeoutHandler handler = new TimeoutHandler( interpreter.persistentConnectionTimeout() ) {
			@Override
			public void onTimeout()
			{
				try {
					synchronized( persistentChannels ) {
						if ( channel.timeoutHandler() == this ) {
							removePersistentChannel( location, protocol, channel );
							channel.close();
							channel.setTimeoutHandler( null );
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		};
		channel.setTimeoutHandler( handler );
		interpreter.addTimeoutHandler( handler );
	}

	public void putPersistentChannel( URI location, String protocol, final CommChannel channel )
	{
		synchronized( persistentChannels ) {
			Map< String, CommChannel > protocolChannels = persistentChannels.get( location );
			if ( protocolChannels == null ) {
				protocolChannels = new HashMap< String, CommChannel >();
				persistentChannels.put( location, protocolChannels );
			}
			// Set the timeout
			setTimeoutHandler( channel, location, protocol );
			// Put the protocol in the cache (may overwrite another one)
			protocolChannels.put( protocol, channel );
			/*if ( protocolChannels.size() <= connectionCacheSize && protocolChannels.containsKey( protocol ) == false ) {
				// Set the timeout
				setTimeoutHandler( channel );
				// Put the protocol in the cache
				protocolChannels.put( protocol, channel );
			} else {
				try {
					if ( protocolChannels.get( protocol ) != channel ) {
						channel.close();
					} else {
						setTimeoutHandler( channel );
					}
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			}*/
		}
	}

	/**
	 * Returns the Interpreter instance this CommCore refers to.
	 * @return the Interpreter instance this CommCore refers to
	 */
	public Interpreter interpreter()
	{
		return interpreter;
	}

	/**
	 * Constructor.
	 * @param interpreter the Interpreter to refer to for this CommCore operations
	 * @param connectionsLimit if more than zero, specifies an upper bound to the connections handled in parallel.
	 * @param connectionsCacheSize specifies an upper bound to the persistent output connection cache.
	 * @throws java.io.IOException
	 */
	public CommCore( Interpreter interpreter, int connectionsLimit /*, int connectionsCacheSize */ )
		throws IOException
	{
		this.interpreter = interpreter;
		this.localListener = new LocalListener( interpreter );
		this.connectionsLimit = connectionsLimit;
		// this.connectionCacheSize = connectionsCacheSize;
		this.threadGroup = new ThreadGroup( "CommCore-" + interpreter.hashCode() );
		if ( connectionsLimit > 0 ) {
			executorService = Executors.newFixedThreadPool( connectionsLimit, new CommThreadFactory() );
		} else {
			executorService = Executors.newCachedThreadPool( new CommThreadFactory() );
		}

		//TODO make socket an extension, too?
		CommListenerFactory listenerFactory = new SocketListenerFactory( this );
		listenerFactories.put( "socket", listenerFactory );
		CommChannelFactory channelFactory = new SocketCommChannelFactory( this );
		channelFactories.put( "socket", channelFactory );
	}
	
	/**
	 * Returns the Logger used by this CommCore.
	 * @return the Logger used by this CommCore
	 */
	public Logger logger()
	{
		return logger;
	}
	
	/**
	 * Returns the connectionsLimit of this CommCore.
	 * @return the connectionsLimit of this CommCore
	 */
	public int connectionsLimit()
	{
		return connectionsLimit;
	}

	public ThreadGroup threadGroup()
	{
		return threadGroup;
	}
	
	private final Collection< Process > protocolConfigurations = new LinkedList< Process > ();
	
	public Collection< Process > protocolConfigurations()
	{
		return protocolConfigurations;
	}
	
	public CommListener getListenerByInputPortName( String serviceName )
	{
		return listenersMap.get( serviceName );
	}
	
	private final Map< String, CommChannelFactory > channelFactories =
						new HashMap< String, CommChannelFactory > ();

	private CommChannelFactory getCommChannelFactory( String name )
		throws IOException
	{
		CommChannelFactory factory = channelFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommChannelFactory( name, this );
			if ( factory != null ) {
				channelFactories.put( name, factory );
			}
		}
		return factory;
	}

	public CommChannel createCommChannel( URI uri, OutputPort port )
		throws IOException
	{
		String medium = uri.getScheme();
		CommChannelFactory factory = getCommChannelFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}
		
		return factory.createChannel( uri, port );
	}
	
	private final Map< String, CommProtocolFactory > protocolFactories =
						new HashMap< String, CommProtocolFactory > ();
	
	public CommProtocolFactory getCommProtocolFactory( String name )
		throws IOException
	{
		CommProtocolFactory factory = protocolFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommProtocolFactory( name, this );
			if ( factory != null ) {
				protocolFactories.put( name, factory );
			}
		}
		return factory;
	}
	
	public CommProtocol createOutputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException
	{
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if ( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}
		
		return factory.createOutputProtocol( configurationPath, uri );
	}

	public CommProtocol createInputCommProtocol( String protocolId, VariablePath configurationPath, URI uri )
		throws IOException
	{
		CommProtocolFactory factory = getCommProtocolFactory( protocolId );
		if ( factory == null ) {
			throw new UnsupportedCommProtocolException( protocolId );
		}

		return factory.createInputProtocol( configurationPath, uri );
	}
	
	private final Map< String, CommListenerFactory > listenerFactories =
						new HashMap< String, CommListenerFactory > ();

	private final LocalListener localListener;

	public LocalCommChannel getLocalCommChannel()
	{
		return new LocalCommChannel( interpreter, localListener );
	}
	
	public LocalCommChannel getLocalCommChannel( CommListener listener )
	{
		return new LocalCommChannel( interpreter, listener );
	}

	private CommListenerFactory getCommListenerFactory( String name )
		throws IOException
	{
		CommListenerFactory factory = listenerFactories.get( name );
		if ( factory == null ) {
			factory = interpreter.getClassLoader().createCommListenerFactory( name, this );
			if ( factory != null ) {
				listenerFactories.put( name, factory );
			}
		}
		return factory;
	}
	
	public LocalListener localListener()
	{
		return localListener;
	}
	
	public void addLocalInputPort( InputPort inputPort )
		throws IOException
	{
		localListener.mergeInterface( inputPort.getInterface() );
		localListener.addAggregations( inputPort.aggregationMap() );
		localListener.addRedirections( inputPort.redirectionMap() );
		listenersMap.put( inputPort.name(), localListener );
	}
	
	/**
	 * Adds an input port to this <code>CommCore</code>.
	 * This method is not thread-safe.
	 * @param inputPortName the name of the input port to add
	 * @param uri the <code>URI</code> of the input port to add
	 * @param protocolFactory the <code>CommProtocolFactory</code> to use for the input port
	 * @param protocolConfigurationPath the protocol configuration variable path to use for the input port
	 * @param protocolConfigurationProcess the protocol configuration process to execute for configuring the created protocols
	 * @param operationNames the operation names the input port can handle
	 * @param aggregationMap the aggregation mapping of the input port
	 * @param redirectionMap the redirection mapping of the input port
	 * @throws java.io.IOException in case of some underlying implementation error
	 * @see URI
	 * @see CommProtocolFactory
	 */
	public void addInputPort(
				InputPort inputPort,
				CommProtocolFactory protocolFactory,
				Process protocolConfigurationProcess
			)
		throws IOException
	{
		protocolConfigurations.add( protocolConfigurationProcess );

		CommListener listener = null;
		String medium = inputPort.location().getScheme();
		CommListenerFactory factory = getCommListenerFactory( medium );
		if ( factory == null ) {
			throw new UnsupportedCommMediumException( medium );
		}

		listener = factory.createListener(
			interpreter,
			protocolFactory,
			inputPort
		);
		listenersMap.put( inputPort.name(), listener );
	}
	
	private final ExecutorService executorService;
	
	private class CommThreadFactory implements ThreadFactory {
		public Thread newThread( Runnable r )
		{
			return new CommChannelHandler( interpreter, r );
		}
	}

	private static Pattern pathSplitPattern = Pattern.compile( "/" );

	private class CommChannelHandlerRunnable implements Runnable {
		private final CommChannel channel;
		private final InputPort port;
		
		public CommChannelHandlerRunnable( CommChannel channel, InputPort port )
		{
			this.channel = channel;
			this.port = port;
		}
		
		private void forwardResponse( CommMessage message )
			throws IOException
		{
			message = new CommMessage(
				channel.redirectionMessageId(),
				message.operationName(),
				message.resourcePath(),
				message.value(),
				message.fault()
			);
			try {
				try {
					channel.redirectionChannel().send( message );
				} finally {
					try {
						if ( channel.redirectionChannel().toBeClosed() ) {
							channel.redirectionChannel().close();
						} else {
							channel.redirectionChannel().disposeForInput();
						}
					} finally {
						channel.setRedirectionChannel( null );
					}
				}
			} finally {
				channel.closeImpl();
			}
		}

		private void handleRedirectionInput( CommMessage message, String[] ss )
			throws IOException, URISyntaxException
		{
			// Redirection
			String rPath = "";
			if ( ss.length <= 2 ) {
				rPath = "/";
			} else {
				StringBuilder builder = new StringBuilder();
				for( int i = 2; i < ss.length; i++ ) {
					builder.append( '/' );
					builder.append( ss[ i ] );
				}
				rPath = builder.toString();
			}
			OutputPort oPort = port.redirectionMap().get( ss[1] );
			if ( oPort == null ) {
				String error = "Discarded a message for resource " + ss[1] +
						", not specified in the appropriate redirection table.";
				interpreter.logWarning( error );
				throw new IOException( error );
			}
			try {
				CommChannel oChannel = oPort.getNewCommChannel();
				CommMessage rMessage =
							new CommMessage(
									message.id(),
									message.operationName(),
									rPath,
									message.value(),
									message.fault()
							);
				oChannel.setRedirectionChannel( channel );
				oChannel.setRedirectionMessageId( rMessage.id() );
				oChannel.send( rMessage );
				oChannel.setToBeClosed( false );
				oChannel.disposeForInput();
			} catch( IOException e ) {
				channel.send( CommMessage.createFaultResponse( message, new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ) ) );
				channel.disposeForInput();
				throw e;
			}
		}

		private void handleAggregatedInput( CommMessage message, AggregatedOperation operation )
			throws IOException, URISyntaxException
		{
			operation.runAggregationBehaviour( message, channel );
		}

		private void handleDirectMessage( CommMessage message )
			throws IOException
		{
			try {
				InputOperation operation =
					interpreter.getInputOperation( message.operationName() );
				try {
					operation.requestType().check( message.value() );
					interpreter.correlationEngine().onMessageReceive( message, channel );
					if ( operation instanceof OneWayOperation ) {
						// We need to send the acknowledgement
						channel.send( CommMessage.createEmptyResponse( message ) );
						//channel.release();
					}
				} catch( TypeCheckingException e ) {
					interpreter.logWarning( "Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
					try {
						channel.send( CommMessage.createFaultResponse( message, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
					} catch( IOException ioe ) {
						Interpreter.getInstance().logSevere( ioe );
					}
				} catch( CorrelationError e ) {
					interpreter.logWarning( "Received a non correlating message for operation " + message.operationName() + ". Sending CorrelationError to the caller." );
					channel.send( CommMessage.createFaultResponse( message, new FaultException( "CorrelationError", "The message you sent can not be correlated with any session and can not be used to start a new session." ) ) );
				}
			} catch( InvalidIdException e ) {
				interpreter.logWarning( "Received a message for undefined operation " + message.operationName() + ". Sending IOException to the caller." );
				channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
			} finally {
				channel.disposeForInput();
			}
		}

		private void handleMessage( CommMessage message )
			throws IOException
		{
			try {
				String[] ss = pathSplitPattern.split( message.resourcePath() );
				if ( ss.length > 1 ) {
					handleRedirectionInput( message, ss );
				} else {
					if ( port.canHandleInputOperationDirectly( message.operationName() ) ) {
						handleDirectMessage( message );
					} else {
						AggregatedOperation operation = port.getAggregatedOperation( message.operationName() );
						if ( operation == null ) {
							interpreter.logWarning(
								"Received a message for operation " + message.operationName() +
									", not specified in the input port at the receiving service. Sending IOException to the caller."
							);
							channel.send( CommMessage.createFaultResponse( message, new FaultException( "IOException", "Invalid operation: " + message.operationName() ) ) );
							channel.disposeForInput();
						} else {
							handleAggregatedInput( message, operation );
						}
					}
				}
			} catch( URISyntaxException e ) {
				interpreter.logSevere( e );
			}
		}
		
		public void run()
		{
			CommChannelHandler thread = CommChannelHandler.currentThread();
			thread.setExecutionThread( interpreter().initThread() );
			channel.lock.lock();
			channelHandlersLock.readLock().lock();
			try {
				if ( channel.redirectionChannel() == null ) {
					assert( port != null );
					CommMessage message = channel.recv();
					if ( message != null ) {
						handleMessage( message );
					}
				} else {
					channel.lock.unlock();
					CommMessage response = channel.recvResponseFor( new CommMessage( channel.redirectionMessageId(), "", "/", Value.UNDEFINED_VALUE, null ) );
					if ( response != null ) {
						forwardResponse( response );
					}
				}
			} catch( IOException e ) {
				interpreter.logSevere( e );
			} finally {
				channelHandlersLock.readLock().unlock();
				if ( channel.lock.isHeldByCurrentThread() ) {
					channel.lock.unlock();
				}
				thread.setExecutionThread( null );
			}
		}
	}

	/**
	 * Schedules the receiving of a message on this <code>CommCore</code> instance.
	 * @param channel the <code>CommChannel</code> to use for receiving the message
	 * @param port the <code>Port</code> responsible for the message receiving
	 */
	public void scheduleReceive( CommChannel channel, InputPort port )
	{
		executorService.execute( new CommChannelHandlerRunnable( channel, port ) );
	}

	/**
	 * Runs an asynchronous task in this CommCore internal thread pool.
	 * @param r the Runnable object to execute
	 */
	public void execute( Runnable r )
	{
		executorService.execute( r );
	}

	protected void startCommChannelHandler( Runnable r )
	{
		executorService.execute( r );
	}
	
	/**
	 * Initializes the communication core, starting its communication listeners.
	 * This method is asynchronous. When it returns, every communication listener has
	 * been issued to start, but they are not guaranteed to be ready to receive messages.
	 * This method throws an exception if some listener can not be issued to start;
	 * other errors will be logged by the listener through the interpreter logger.
	 *
	 * @throws IOException in case of some underlying <code>CommListener</code> initialization error
	 * @see CommListener
	 */
	public void init()
		throws IOException
	{
		for( Entry< String, CommListener > entry : listenersMap.entrySet() ) {
			entry.getValue().start();
		}
		active = true;
	}
	
	private PollingThread pollingThread = null;
	
	private PollingThread pollingThread()
	{
		synchronized( this ) {
			if ( pollingThread == null ) {
				pollingThread = new PollingThread();
				pollingThread.start();
			}
		}
		return pollingThread;
	}
	
	private class PollingThread extends Thread {
		private final Set< CommChannel > channels = new HashSet< CommChannel >();

		private PollingThread()
		{
			super( threadGroup, interpreter.programFilename() + "-PollingThread" );
		}

		@Override
		public void run()
		{
			Iterator< CommChannel > it;
			CommChannel channel;
			while( active ) {
				synchronized( this ) {
					if ( channels.isEmpty() ) {
						// Do not busy-wait for no reason
						try {
							this.wait();
						} catch( InterruptedException e ) {}
					}
					it = channels.iterator();
					while( it.hasNext() ) {
						channel = it.next();
						try {
							if ( ((PollableCommChannel)channel).isReady() ) {
								it.remove();
								scheduleReceive( channel, channel.parentInputPort() );
							}
						} catch( IOException e ) {
							e.printStackTrace();
						}
					}
				}
				try {
					Thread.sleep( 50 ); // msecs
				} catch( InterruptedException e ) {}
			}

			for( CommChannel c : channels ) {
				try {
					c.closeImpl();
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			}
		}
		
		public void register( CommChannel channel )
			throws IOException
		{
			if ( !(channel instanceof PollableCommChannel) ) {
				throw new IOException( "Channels registering for polling must implement PollableCommChannel interface");
			}
			
			synchronized( this ) {
				channels.add( channel );
				if ( channels.size() == 1 ) { // set was empty
					this.notify();
				}
			}
		}
	}

	/**
	 * Registers a <code>CommChannel</code> for input polling.
	 * The registered channel must implement the {@link PollableCommChannel <code>PollableCommChannel</code>} interface.
	 * @param channel the channel to register for polling
	 * @throws java.io.IOException in case the channel could not be registered for polling
	 * @see CommChannel
	 * @see PollableCommChannel
	 */
	public void registerForPolling( CommChannel channel )
		throws IOException
	{
		pollingThread().register( channel );
	}
	
	private SelectorThread selectorThread = null;
	private Object selectorThreadMonitor = new Object();
	
	private SelectorThread selectorThread()
		throws IOException
	{
		synchronized( selectorThreadMonitor ) {
			if ( selectorThread == null ) {
				selectorThread = new SelectorThread( interpreter );
				selectorThread.start();
			}
		}
		return selectorThread;
	}

	private class SelectorThread extends JolieThread {
		private final Selector selector;
		private final Object selectingMutex = new Object();
		public SelectorThread( Interpreter interpreter )
			throws IOException
		{
			super( interpreter, threadGroup, interpreter.programFilename() + "-SelectorThread" );
			this.selector = Selector.open();
		}
		
		@Override
		public void run()
		{
			SelectableStreamingCommChannel channel;
			while( active ) {
				try {
					synchronized( selectingMutex ) {
						selector.select();
					}
					synchronized( this ) {
						for( SelectionKey key : selector.selectedKeys() ) {
							if ( key.isValid() ) {
								channel = (SelectableStreamingCommChannel)key.attachment();
								try {
									if ( channel.lock.tryLock() ) {
										try {
											key.cancel();
											key.channel().configureBlocking( true );
											if ( channel.isOpen() ) {
												/*if ( channel.selectionTimeoutHandler() != null ) {
													interpreter.removeTimeoutHandler( channel.selectionTimeoutHandler() );
												}*/
												scheduleReceive( channel, channel.parentInputPort() );
											} else {
												channel.closeImpl();
											}
										} catch( IOException e ) {
											throw e;
										} finally {
											channel.lock.unlock();
										}
									}
								} catch( IOException e ) {
									if ( channel.lock.isHeldByCurrentThread() ) {
										channel.lock.unlock();
									}
									if ( interpreter.verbose() ) {
										interpreter.logSevere( e );
									}
								}
							}
						}
						synchronized( selectingMutex ) {
							selector.selectNow(); // Clean up the cancelled keys
						}
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}

			for( SelectionKey key : selector.keys() ) {
				try {
					((SelectableStreamingCommChannel)key.attachment()).closeImpl();
				} catch( IOException e ) {
					interpreter.logWarning( e );
				}
			}
		}
		
		public boolean register( SelectableStreamingCommChannel channel )
		{
			try {
				if ( channel.inputStream().available() > 0 ) {
					scheduleReceive( channel, channel.parentInputPort() );
					return false;
				}

				synchronized( this ) {
					if ( isSelecting( channel ) == false ) {
						SelectableChannel c = channel.selectableChannel();
						c.configureBlocking( false );
						selector.wakeup();
						synchronized( selectingMutex ) {
							c.register( selector, SelectionKey.OP_READ, channel );
							selector.selectNow();
						}
					}
				}
				return true;
			} catch( ClosedChannelException e ) {
				interpreter.logWarning( e );
				return false;
			} catch( IOException e ) {
				interpreter.logSevere( e );
				return false;
			}
		}

		public void unregister( SelectableStreamingCommChannel channel )
			throws IOException
		{
			synchronized( this ) {
				if ( isSelecting( channel ) ) {
					selector.wakeup();
					synchronized( selectingMutex ) {
						SelectionKey key = channel.selectableChannel().keyFor( selector );
						if ( key != null ) {
							key.cancel();
						}
						selector.selectNow();
					}
					channel.selectableChannel().configureBlocking( true );
				}
			}
		}

		private boolean isSelecting( SelectableStreamingCommChannel channel )
		{
			SelectableChannel c = channel.selectableChannel();
			if ( c == null ) {
				return false;
			}
			return c.keyFor( selector ) != null;
		}
	}

	protected boolean isSelecting( SelectableStreamingCommChannel channel )
	{
		synchronized( this ) {
			if ( selectorThread == null ) {
				return false;
			}
		}
		final SelectorThread t = selectorThread;
		synchronized( t ) {
			return t.isSelecting( channel );
		}
	}

	protected void unregisterForSelection( SelectableStreamingCommChannel channel )
		throws IOException
	{
		selectorThread().unregister( channel );
	}
	
	protected void registerForSelection( final SelectableStreamingCommChannel channel )
		throws IOException
	{
		selectorThread().register( channel );
		/*final TimeoutHandler handler = new TimeoutHandler( interpreter.persistentConnectionTimeout() ) {
			@Override
			public void onTimeout()
			{
				try {
					if ( isSelecting( channel ) ) {
						selectorThread().unregister( channel );
						channel.setToBeClosed( true );
						channel.close();
					}
				} catch( IOException e ) {
					interpreter.logSevere( e );
				}
			}
		};
		channel.setSelectionTimeoutHandler( handler );
		if ( selectorThread().register( channel ) ) {
			interpreter.addTimeoutHandler( handler );
		} else {
			channel.setSelectionTimeoutHandler( null );
		}*/
	}

	/** Shutdowns the communication core, interrupting every communication-related thread. */
	public synchronized void shutdown()
	{
		if ( active ) {
			active = false;
			for( Entry< String, CommListener > entry : listenersMap.entrySet() ) {
				entry.getValue().shutdown();
			}
			if ( selectorThread != null ) {
				selectorThread.selector.wakeup();
				try {
					selectorThread.join();
				} catch( InterruptedException e ) {}
			}
			try {
				channelHandlersLock.writeLock().tryLock( CHANNEL_HANDLER_TIMEOUT, TimeUnit.SECONDS );
			} catch( InterruptedException e ) {}
			executorService.shutdown();
			try {
				executorService.awaitTermination( interpreter.persistentConnectionTimeout(), TimeUnit.MILLISECONDS );
			} catch( InterruptedException e ) {}
			threadGroup.interrupt();
		}
	}

	private boolean active = false;
}
