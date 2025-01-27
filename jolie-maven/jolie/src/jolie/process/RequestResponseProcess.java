/***************************************************************************
 *   Copyright (C) 2006-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.process;

import java.io.IOException;
import java.util.concurrent.Future;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class RequestResponseProcess implements InputOperationProcess
{
	private final RequestResponseOperation operation;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Process process;
	
	public RequestResponseProcess(
			RequestResponseOperation operation,
			VariablePath inputVarPath,
			Expression outputExpression,
			Process process )
	{
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputExpression = outputExpression;
	}

	public InputOperation inputOperation()
	{
		return operation;
	}

	private void log( String message )
	{
		if ( Interpreter.getInstance().verbose() ) {
			Interpreter.getInstance().logInfo( "[RequestResponse operation " + operation.id() + "]: " + message );
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}

	public Process clone( TransformationReason reason )
	{
		return new RequestResponseProcess(
					operation,
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( outputExpression == null ) ? null : (VariablePath)outputExpression.cloneExpression( reason ),
					process.clone( reason )
				);
	}
	
	public Process receiveMessage( final SessionMessage sessionMessage, jolie.State state )
	{
		log( "received message " + sessionMessage.message().id() );
		if ( inputVarPath != null ) {
			inputVarPath.getValue( state.root() ).refCopy( sessionMessage.message().value() );
		}

		return new Process() {
			public void run()
				throws FaultException, ExitingException
			{
				runBehaviour( sessionMessage.channel(), sessionMessage.message() );
			}

			public Process clone( TransformationReason reason )
			{
				return this;
			}

			public boolean isKillable()
			{
				return false;
			}
		};
	}

	public void run()
		throws FaultException, ExitingException
	{
		ExecutionThread ethread = ExecutionThread.currentThread();
		if ( ethread.isKilled() ) {
			return;
		}

		Future< SessionMessage > f = ethread.requestMessage( operation );
		try {
			SessionMessage m = f.get();
			receiveMessage( m, ethread.state() ).run();
		} catch( Exception e ) {
			Interpreter.getInstance().logSevere( e );
		}
	}
	
	public VariablePath inputVarPath()
	{
		return inputVarPath;
	}

	private CommMessage createFaultMessage( CommMessage request, FaultException f )
		throws TypeCheckingException
	{
		if ( operation.typeDescription().faults().containsKey( f.faultName() ) ) {
			Type faultType = operation.typeDescription().faults().get( f.faultName() );
			if ( faultType != null ) {
				faultType.check( f.value() );
			}
		} else {
			Interpreter.getInstance().logSevere(
				"Request-Response process for " + operation.id() +
				" threw an undeclared fault for that operation (" + f.faultName() + "), throwing TypeMismatch" );
			f = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error" );
		}
		return CommMessage.createFaultResponse( request, f );
	}
	
	private void runBehaviour( CommChannel channel, CommMessage message )
		throws FaultException
	{
		FaultException typeMismatch = null;

		FaultException fault = null;
		CommMessage response = null;
		try {
			try {
				process.run();
			} catch( ExitingException e ) {}
			ExecutionThread ethread = ExecutionThread.currentThread();
			if ( ethread.isKilled() ) {
				try {
					response = createFaultMessage( message, ethread.killerFault() );
				} catch( TypeCheckingException e ) {
					typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + ethread.killerFault().faultName() + " (operation " + operation.id() + "): " + e.getMessage() );
					response = CommMessage.createFaultResponse( message, typeMismatch );
				}
			} else {
				response =
					CommMessage.createResponse(
						message,
						( outputExpression == null ) ? Value.UNDEFINED_VALUE : outputExpression.evaluate()
					);
				if ( operation.typeDescription().responseType() != null ) {
					try {
						operation.typeDescription().responseType().check( response.value() );
					} catch( TypeCheckingException e ) {
						typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response input operation output value TypeMismatch (operation " + operation.id() + "): " + e.getMessage() );
						response = CommMessage.createFaultResponse( message, new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error (TypeMismatch)" ) );
					}
				}
			}
		} catch( FaultException f ) {
			try {
				response = createFaultMessage( message, f );
			} catch( TypeCheckingException e ) {
				typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + f.faultName() + " (operation " + operation.id() + "): " + e.getMessage() );
				response = CommMessage.createFaultResponse( message, typeMismatch );
			}
			fault = f;
		}

		try {
			channel.send( response );
			log( "sent response for message " + message.id() );
		} catch( IOException e ) {
			//Interpreter.getInstance().logSevere( e );
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} finally {
			try {
				channel.release(); // TODO: what if the channel is in disposeForInput?
			} catch( IOException e ) {
				Interpreter.getInstance().logSevere( e );
			}
		}

		if ( fault != null ) {
			if ( typeMismatch != null ) {
				Interpreter.getInstance().logWarning( typeMismatch.value().strValue() );
			}
			throw fault;
		} else if ( typeMismatch != null ) {
			throw typeMismatch;
		}
	}
}
