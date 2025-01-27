/***************************************************************************
 *   Copyright (C) 2010 by Balint Maschio <bmaschio@italianasoftware.com>  *
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

package joliex.java.impl;

import java.util.ArrayList;
import java.util.List;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import joliex.java.support.GeneralProgramVisitor;


/**
 *
 * @author Balint Maschio
 */
public class ProgramVisitor   extends GeneralProgramVisitor implements OLVisitor
{
	//final private Program program;
	final private List<InterfaceDefinition> interfaceDefinitions =
		new ArrayList<InterfaceDefinition>();
	final private List<OutputPortInfo> outportPortDefinitions =
		new ArrayList<OutputPortInfo>();
	final private List<InputPortInfo> inputPortDefinitions =
		new ArrayList<InputPortInfo>();
	/*final private List< Type > inputPortDefinitions =
	new ArrayList< InputPortInfo >();*/

	public ProgramVisitor( Program program )
	{
		super(program);

	}

	public void run()
	{
		super.program.accept( this );

	}

	public void clearLists()
	{
		interfaceDefinitions.clear();
		outportPortDefinitions.clear();
		inputPortDefinitions.clear();
	}

	public InterfaceDefinition[] getInterfaceDefinitions()
	{

		return interfaceDefinitions.toArray( new InterfaceDefinition[]{} );
	}

	public OutputPortInfo[] getOutputPortInfo()
	{
		return outportPortDefinitions.toArray( new OutputPortInfo[]{} );

	}

	public InputPortInfo[] getInputPortInfo()
	{

		return inputPortDefinitions.toArray( new InputPortInfo[]{} );

	}

	public void visit( Program n )
	{
		for( OLSyntaxNode child : n.children() ) {
			child.accept( this );
		}
	}

	public void visit( InterfaceDefinition n )
	{
		interfaceDefinitions.add( n );
	}

	public void visit( OneWayOperationDeclaration decl )
	{
	}

	public void visit( RequestResponseOperationDeclaration decl )
	{
	}

	public void visit( DefinitionNode n )
	{
	}

	public void visit( ParallelStatement n )
	{
	}

	public void visit( SequenceStatement n )
	{
	}

	public void visit( NDChoiceStatement n )
	{
	}

	public void visit( OneWayOperationStatement n )
	{
	}

	public void visit( RequestResponseOperationStatement n )
	{
	}

	public void visit( NotificationOperationStatement n )
	{
	}

	public void visit( SolicitResponseOperationStatement n )
	{
	}

	public void visit( LinkInStatement n )
	{
	}

	public void visit( LinkOutStatement n )
	{
	}

	public void visit( AssignStatement n )
	{
	}

	public void visit( IfStatement n )
	{
	}

	public void visit( DefinitionCallStatement n )
	{
	}

	public void visit( WhileStatement n )
	{
	}

	public void visit( OrConditionNode n )
	{
	}

	public void visit( AndConditionNode n )
	{
	}

	public void visit( NotConditionNode n )
	{
	}

	public void visit( CompareConditionNode n )
	{
	}

	public void visit( ExpressionConditionNode n )
	{
	}

	public void visit( ConstantIntegerExpression n )
	{
	}

	public void visit( ConstantRealExpression n )
	{
	}

	public void visit( ConstantStringExpression n )
	{
	}

	public void visit( ProductExpressionNode n )
	{
	}

	public void visit( SumExpressionNode n )
	{
	}

	public void visit( VariableExpressionNode n )
	{
	}

	public void visit( NullProcessStatement n )
	{
	}

	public void visit( Scope n )
	{
	}

	public void visit( InstallStatement n )
	{
	}

	public void visit( CompensateStatement n )
	{
	}

	public void visit( ThrowStatement n )
	{
	}

	public void visit( ExitStatement n )
	{
	}

	public void visit( ExecutionInfo n )
	{
	}

	public void visit( CorrelationSetInfo n )
	{
	}

	public void visit( InputPortInfo n )
	{

		inputPortDefinitions.add( n );


	}

	public void visit( OutputPortInfo n )
	{

		outportPortDefinitions.add( n );

	}

	public void visit( PointerStatement n )
	{
	}

	public void visit( DeepCopyStatement n )
	{
	}

	public void visit( RunStatement n )
	{
	}

	public void visit( UndefStatement n )
	{
	}

	public void visit( ValueVectorSizeExpressionNode n )
	{
	}

	public void visit( PreIncrementStatement n )
	{
	}

	public void visit( PostIncrementStatement n )
	{
	}

	public void visit( PreDecrementStatement n )
	{
	}

	public void visit( PostDecrementStatement n )
	{
	}

	public void visit( ForStatement n )
	{
	}

	public void visit( ForEachStatement n )
	{
	}

	public void visit( SpawnStatement n )
	{
	}

	public void visit( IsTypeExpressionNode n )
	{
	}

	public void visit( TypeCastExpressionNode n )
	{
	}

	public void visit( SynchronizedStatement n )
	{
	}

	public void visit( CurrentHandlerStatement n )
	{
	}

	public void visit( EmbeddedServiceNode n )
	{
	}

	public void visit( InstallFixedVariableExpressionNode n )
	{
	}

	public void visit( VariablePathNode n )
	{
	}

	public void visit( TypeInlineDefinition n )
	{
	}

	public void visit( TypeDefinitionLink n )
	{
	}

	public void visit( DocumentationComment n )
	{
	}
	
	public void visit( AddAssignStatement n ) {}
	public void visit( SubtractAssignStatement n ) {}
	public void visit( MultiplyAssignStatement n ) {}
	public void visit( DivideAssignStatement n ) {}
}
