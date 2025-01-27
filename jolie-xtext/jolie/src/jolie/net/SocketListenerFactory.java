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

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import jolie.Interpreter;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.VariablePath;

/**
 * A factory for <code>SocketListener</code>.
 * @see SocketListener
 * @author Fabrizio Montesi
 */
public class SocketListenerFactory extends CommListenerFactory
{
	public SocketListenerFactory( CommCore commCore )
	{
		super( commCore );
	}

	public CommListener createListener(
					Interpreter interpreter,
					URI location,
					CommProtocolFactory protocolFactory,
					VariablePath protocolConfigurationPath,
					Collection< String > operationNames,
					Map< String, AggregatedOperation > aggregationMap,
					Map< String, OutputPort > redirectionMap
				)
		throws IOException
	{
		return new SocketListener( interpreter, location, protocolFactory, protocolConfigurationPath, operationNames, aggregationMap, redirectionMap );
	}
}
