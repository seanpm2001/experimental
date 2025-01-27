/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.lang.parse.util.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;

/**
 * Visitor for creating a {@link ProgramInspectorImpl} object.
 * @author Fabrizio Montesi
 */
public class ProgramInspectorCreatorVisitor implements OLVisitor
{
	private final Map< URI, List< InterfaceDefinition > > interfaces = new HashMap< URI, List< InterfaceDefinition > >();
	private final Map< URI, List< InputPortInfo > > inputPorts = new HashMap< URI, List< InputPortInfo > >();
	private final Map< URI, List< OutputPortInfo > > outputPorts = new HashMap< URI, List< OutputPortInfo > >();
	private final Map< URI, List< TypeDefinition > > types = new HashMap< URI, List< TypeDefinition > >();
	private final Map< URI, List< DefinitionNode > > definitions = new HashMap< URI, List< DefinitionNode > >();
	private final Set< URI > sources = new HashSet< URI >();

	public ProgramInspectorCreatorVisitor( Program program )
	{
		program.accept( this );
	}

	public ProgramInspector createInspector()
	{
		return new ProgramInspectorImpl(
			sources.toArray( new URI[0] ),
			types,
			interfaces,
			inputPorts,
			outputPorts,
			definitions
		);
	}

	private void encounteredNode( OLSyntaxNode n )
	{
		sources.add( n.context().source() );
	}

	public void visit( Program n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	public void visit( InterfaceDefinition n )
	{
		List< InterfaceDefinition > list = interfaces.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< InterfaceDefinition >();
			interfaces.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( TypeInlineDefinition n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< TypeDefinition >();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( TypeDefinitionLink n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< TypeDefinition >();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}
	
	public void visit( DefinitionNode n )
	{
		List< DefinitionNode > list = definitions.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< DefinitionNode >();
			definitions.put( n.context().source(), list );
		}
		list.add( n );
		
		encounteredNode( n );
	}

	public void visit( TypeChoiceDefinition n )
	{
		//TODO
	}
	
	public void visit( InputPortInfo n )
	{
		List< InputPortInfo > list = inputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< InputPortInfo >();
			inputPorts.put( n.context().source(), list );
		}
		list.add( n );
		encounteredNode( n );
	}

	public void visit( OutputPortInfo n )
	{
		List< OutputPortInfo > list = outputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< OutputPortInfo >();
			outputPorts.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( OneWayOperationDeclaration decl ) {}
	public void visit( RequestResponseOperationDeclaration decl ) {}
	public void visit( ParallelStatement n ) {}
	public void visit( SequenceStatement n ) {}
	public void visit( NDChoiceStatement n ) {}
	public void visit( OneWayOperationStatement n ) {}
	public void visit( RequestResponseOperationStatement n ) {}
	public void visit( NotificationOperationStatement n ) {}
	public void visit( SolicitResponseOperationStatement n ) {}
	public void visit( LinkInStatement n ) {}
	public void visit( LinkOutStatement n ) {}
	public void visit( AssignStatement n ) {}
	public void visit( IfStatement n ) {}
	public void visit( DefinitionCallStatement n ) {}
	public void visit( WhileStatement n ) {}
	public void visit( OrConditionNode n ) {}
	public void visit( AndConditionNode n ) {}
	public void visit( NotExpressionNode n ) {}
	public void visit( CompareConditionNode n ) {}
	public void visit( ConstantIntegerExpression n ) {}
	public void visit( ConstantLongExpression n ) {}
	public void visit( ConstantBoolExpression n ) {}
	public void visit( ConstantDoubleExpression n ) {}
	public void visit( ConstantStringExpression n ) {}
	public void visit( ProductExpressionNode n ) {}
	public void visit( SumExpressionNode n ) {}
	public void visit( VariableExpressionNode n ) {}
	public void visit( NullProcessStatement n ) {}
	public void visit( Scope n ) {}
	public void visit( InstallStatement n ) {}
	public void visit( CompensateStatement n ) {}
	public void visit( ThrowStatement n ) {}
	public void visit( ExitStatement n ) {}
	public void visit( ExecutionInfo n ) {}
	public void visit( CorrelationSetInfo n ) {}
	public void visit( PointerStatement n ) {}
	public void visit( DeepCopyStatement n ) {}
	public void visit( RunStatement n ) {}
	public void visit( UndefStatement n ) {}
	public void visit( ValueVectorSizeExpressionNode n ) {}
	public void visit( PreIncrementStatement n ) {}
	public void visit( PostIncrementStatement n ) {}
	public void visit( PreDecrementStatement n ) {}
	public void visit( PostDecrementStatement n ) {}
	public void visit( ForStatement n ) {}
	public void visit( ForEachStatement n ) {}
	public void visit( SpawnStatement n ) {}
	public void visit( IsTypeExpressionNode n ) {}
	public void visit( TypeCastExpressionNode n ) {}
	public void visit( SynchronizedStatement n ) {}
	public void visit( CurrentHandlerStatement n ) {}
	public void visit( EmbeddedServiceNode n ) {}
	public void visit( InstallFixedVariableExpressionNode n ) {}
	public void visit( VariablePathNode n ) {}
	public void visit( DocumentationComment n ) {}
	public void visit( AddAssignStatement n ) {}
	public void visit( SubtractAssignStatement n ) {}
	public void visit( MultiplyAssignStatement n ) {}
	public void visit( DivideAssignStatement n ) {}
	public void visit( FreshValueExpressionNode n ) {}
	public void visit( InterfaceExtenderDefinition n ) {}
	public void visit( CourierDefinitionNode n ) {}
	public void visit( CourierChoiceStatement n ) {}
	public void visit( NotificationForwardStatement n ) {}
	public void visit( InstanceOfExpressionNode n ) {}
	public void visit( SolicitResponseForwardStatement n ) {}
}
