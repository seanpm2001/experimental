/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import cx.ath.matthew.unix.UnixSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;

public class LocalSocketCommChannel extends StreamingCommChannel implements PollableCommChannel
{
	private static class PreCachedInputStream extends InputStream
	{
		private int cachePosition = 0;
		private int[] cache;
		final private InputStream istream;
		private PreCachedInputStream( int[] cache, InputStream istream )
		{
			this.cache = cache;
			this.istream = istream;
		}
		
		public int read()
			throws IOException
		{
			int ret = -1;
			if ( cache == null ) {
				ret = istream.read();
			} else if ( cachePosition < cache.length ) {
				ret = cache[ cachePosition ];
				if ( ++cachePosition >= cache.length ) {
					cache = null; // Free memory
				}
			}
			return ret;
		}
	}
	
	final private UnixSocket socket;
	private InputStream istream;
	
	public LocalSocketCommChannel( UnixSocket socket, URI location, CommProtocol protocol )
		throws IOException
	{
		super( location, protocol );
		
		this.socket = socket;
		this.istream = socket.getInputStream();
		
		setToBeClosed( false ); // LocalSocket connections are kept open by default
	}

	protected void sendImpl( CommMessage message )
		throws IOException
	{
		protocol().send( socket.getOutputStream(), message, socket.getInputStream() );
	}
	
	protected CommMessage recvImpl()
		throws IOException
	{
		CommMessage ret = null;
		ret = protocol().recv( istream, socket.getOutputStream() );
		if ( istream instanceof PreCachedInputStream ) {
			istream = socket.getInputStream();
		}
		return ret;
	}
	
	protected void closeImpl()
		throws IOException
	{
		socket.close();
	}
	
	public synchronized boolean isReady()
	{
		boolean ret = false;
		try {
			int[] cache = new int[1];
			InputStream is = socket.getInputStream();
			if ( (cache[0]=is.read()) != -1 ) {
				istream = new PreCachedInputStream( cache, is );
				ret = true;
			}
		} catch( IOException e ) {
			e.printStackTrace();
		}
		return ret;
	}
	
	@Override
	public void disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForPolling( this );
	}
}
