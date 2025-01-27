/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package jolie.lang.parse.ast.types;

import java.util.Map;
import java.util.Set;
import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ParsingContext;
import jolie.util.Range;

/**
 *
 * @author Fabrizio Montesi
 */
public class TypeDefinitionLink extends TypeDefinition
{
	private final TypeDefinition linkedType;

	public TypeDefinitionLink( ParsingContext context, String id, Range cardinality, TypeDefinition linkedType )
	{
		super( context, id, cardinality );
		this.linkedType = linkedType;
	}

	public TypeDefinition linkedType()
	{
		return linkedType;
	}

	public boolean isValid()
	{
		return ( linkedType != null );
	}

	public boolean untypedSubTypes()
	{
		return linkedType.untypedSubTypes();
	}

	public boolean hasSubTypes()
	{
		return linkedType.hasSubTypes();
	}

	public TypeDefinition getSubType( String id )
	{
		return linkedType.getSubType( id );
	}

	public NativeType nativeType()
	{
		return linkedType.nativeType();
	}

	public Set< Map.Entry< String, TypeDefinition > > subTypes()
	{
		return linkedType.subTypes();
	}

	public boolean hasSubType( String id )
	{
		return linkedType.hasSubType( id );
	}

	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
