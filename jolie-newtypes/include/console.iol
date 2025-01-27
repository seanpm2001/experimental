/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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

cset { coreJavaserviceConsoleToken: InRequest.token }

type RegisterForInputRequest: void {
  .enableSessionListener?: bool
}

type SubscribeSessionListener: void {
	.token: string
}

type UnsubscribeSessionListener: void {
	.token: string
}

type InRequest: string {
	.token?: string
}

interface ConsoleInterface {
RequestResponse:
	print( undefined )( void ), 

	println( undefined )( void ), 

	/**!
	  it enables the console for input listening
	  parameter session_listener_enabled enables console input listening for more than one service session (default=false)
	*/
	registerForInput( RegisterForInputRequest )( void ),

	/**!
	  it receives a token string which identifies a service session.
	  it enables the session to receive inputs from the console
	*/
	subscribeSessionListener( SubscribeSessionListener )( void ),
	
	/**!
	  it disables a session to receive inputs from the console, previously registered with subscribeSessionListener operation
	*/
	unsubscribeSessionListener( UnsubscribeSessionListener )( void )
}

outputPort Console {
Interfaces: ConsoleInterface
}

embedded {
Java:
	"joliex.io.ConsoleService" in Console
}

inputPort ConsoleInputPort {
Location: "local"
OneWay:
	in( InRequest )
}
