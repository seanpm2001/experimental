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

package jolie.lang.parse;

import java.util.Collection;
import jolie.lang.parse.context.ParsingContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import jolie.lang.Constants.ExecutionMode;

import jolie.lang.Constants.OperandType;
import jolie.lang.parse.CorrelationFunctionInfo.CorrelationPairInfo;
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
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationAliasInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
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
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.URIParsingContext;
import jolie.util.ArrayListMultiMap;
import jolie.util.MultiMap;
import jolie.util.Pair;

/**
 * Checks the well-formedness and validity of a JOLIE program.
 * @see Program
 * @author Fabrizio Montesi
 */
public class SemanticVerifier implements OLVisitor
{
	public static class Configuration {
		public boolean checkForMain = true;
	}
	
	private final Program program;
	private boolean valid = true;
	private final Configuration configuration;

	private ExecutionInfo executionInfo = new ExecutionInfo( URIParsingContext.DEFAULT, ExecutionMode.SINGLE );
	private final Map< String, InputPortInfo > inputPorts = new HashMap< String, InputPortInfo >();
	private final Map< String, OutputPortInfo > outputPorts = new HashMap< String, OutputPortInfo >();
	
	private final Set< String > subroutineNames = new HashSet< String > ();
	private final Map< String, OneWayOperationDeclaration > oneWayOperations =
						new HashMap< String, OneWayOperationDeclaration >();
	private final Map< String, RequestResponseOperationDeclaration > requestResponseOperations =
						new HashMap< String, RequestResponseOperationDeclaration >();

	private final Map< TypeDefinition, List< TypeDefinition > > typesToBeEqual = new HashMap< TypeDefinition, List< TypeDefinition > >();
	private final Map< OneWayOperationDeclaration, List< OneWayOperationDeclaration > > owToBeEqual =
		new HashMap< OneWayOperationDeclaration, List< OneWayOperationDeclaration > >();
	private final Map< RequestResponseOperationDeclaration, List< RequestResponseOperationDeclaration > > rrToBeEqual =
		new HashMap< RequestResponseOperationDeclaration, List< RequestResponseOperationDeclaration > >();
	private final List< CorrelationSetInfo > correlationSets = new LinkedList< CorrelationSetInfo >();

	private boolean insideInputPort = false;
	private boolean insideInit = false;
	private boolean mainDefined = false;
	private CorrelationFunctionInfo correlationFunctionInfo = new CorrelationFunctionInfo();
	private final MultiMap< String, String > inputTypeNameMap =
		new ArrayListMultiMap< String, String >(); // Maps type names to the input operations that use them

	private ExecutionMode executionMode = ExecutionMode.SINGLE;

	private static final Logger logger = Logger.getLogger( "JOLIE" );
	
	private final Map< String, TypeDefinition > definedTypes;
	private final List< TypeDefinitionLink > definedTypeLinks = new LinkedList< TypeDefinitionLink >();
	//private TypeDefinition rootType; // the type representing the whole session state
	private final Map< String, Boolean > isConstantMap = new HashMap< String, Boolean >();

	public SemanticVerifier( Program program, Configuration configuration )
	{
		this.program = program;
		this.definedTypes = OLParser.createTypeDeclarationMap( program.context() );
		this.configuration = configuration;
		/*rootType = new TypeInlineDefinition(
			new ParsingContext(),
			"#RootType",
			NativeType.VOID,
			jolie.lang.Constants.RANGE_ONE_TO_ONE
		);*/
	}
	
	public SemanticVerifier( Program program )
	{
		this( program, new Configuration() );
	}

	public CorrelationFunctionInfo correlationFunctionInfo()
	{
		return correlationFunctionInfo;
	}

	public ExecutionMode executionMode()
	{
		return executionMode;
	}

	private void encounteredAssignment( String varName )
	{
		if ( isConstantMap.containsKey( varName ) ) {
			isConstantMap.put( varName, false );
		} else {
			isConstantMap.put( varName, true );
		}
	}

	private void addTypeEqualnessCheck( TypeDefinition key, TypeDefinition type )
	{
		List< TypeDefinition > toBeEqualList = typesToBeEqual.get( key );
		if ( toBeEqualList == null ) {
			toBeEqualList = new LinkedList< TypeDefinition >();
			typesToBeEqual.put( key, toBeEqualList );
		}
		toBeEqualList.add( type );
	}

	private void addOneWayEqualnessCheck( OneWayOperationDeclaration key, OneWayOperationDeclaration oneWay )
	{
		List< OneWayOperationDeclaration > toBeEqualList = owToBeEqual.get( key );
		if ( toBeEqualList == null ) {
			toBeEqualList = new LinkedList< OneWayOperationDeclaration >();
			owToBeEqual.put( key, toBeEqualList );
		}
		toBeEqualList.add( oneWay );
	}

	private void addRequestResponseEqualnessCheck( RequestResponseOperationDeclaration key, RequestResponseOperationDeclaration requestResponse )
	{
		List< RequestResponseOperationDeclaration > toBeEqualList = rrToBeEqual.get( key );
		if ( toBeEqualList == null ) {
			toBeEqualList = new LinkedList< RequestResponseOperationDeclaration >();
			rrToBeEqual.put( key, toBeEqualList );
		}
		toBeEqualList.add( requestResponse );
	}

	private void encounteredAssignment( VariablePathNode path )
	{
		encounteredAssignment( ((ConstantStringExpression)path.path().get( 0 ).key()).value() );
	}

	public Map< String, Boolean > isConstantMap()
	{
		return isConstantMap;
	}

	private void warning( OLSyntaxNode node, String message )
	{
		if ( node == null ) {
			logger.warning( message );
		} else {
			logger.warning( node.context().sourceName() + ":" + node.context().line() + ": " + message );
		}
	}
	
	private void error( OLSyntaxNode node, String message )
	{
		valid = false;
		if ( node != null ) {
			ParsingContext context = node.context();
			logger.severe( context.sourceName() + ":" + context.line() + ": " + message );
		} else {
			logger.severe( message );
		}
	}

	private void resolveLazyLinks()
	{
		for( TypeDefinitionLink l : definedTypeLinks ) {
			l.setLinkedType( definedTypes.get( l.linkedTypeName() ) );
			if ( l.linkedType() == null ) {
				error( l, "type " + l.id() + "points to an undefined type (" + l.linkedTypeName() + ")" );
			}
		}
	}

	private void checkToBeEqualTypes()
	{
		for( Entry< TypeDefinition, List< TypeDefinition > > entry : typesToBeEqual.entrySet() ) {
			for( TypeDefinition type : entry.getValue() ) {
				if ( entry.getKey().isEquivalentTo( type ) == false ) {
					error( type, "type " + type.id() + " has already been defined with a different structure" );
				}
			}
		}

		for( Entry< OneWayOperationDeclaration, List< OneWayOperationDeclaration > > entry : owToBeEqual.entrySet() ) {
			for( OneWayOperationDeclaration ow : entry.getValue() ) {
				checkEqualness( entry.getKey(), ow );
			}
		}

		for( Entry< RequestResponseOperationDeclaration, List< RequestResponseOperationDeclaration > > entry : rrToBeEqual.entrySet() ) {
			for( RequestResponseOperationDeclaration rr : entry.getValue() ) {
				checkEqualness( entry.getKey(), rr );
			}
		}
	}

	private void checkCorrelationSets()
	{
		Collection< String > operations;
		Set< String > correlatingOperations = new HashSet< String >();
		Set< String > currCorrelatingOperations = new HashSet< String >();
		for( CorrelationSetInfo cset : correlationSets ) {
			correlationFunctionInfo.correlationSets().add( cset );
			currCorrelatingOperations.clear();
			for( CorrelationSetInfo.CorrelationVariableInfo csetVar : cset.variables() ) {
				for( CorrelationAliasInfo alias : csetVar.aliases() ) {
					checkCorrelationAlias( alias );
					
					operations = inputTypeNameMap.get( alias.guardName() );
					for( String operationName : operations ) {
						currCorrelatingOperations.add( operationName );
						correlationFunctionInfo.putCorrelationPair(
							operationName,
							new CorrelationPairInfo(
								csetVar.correlationVariablePath(),
								alias.variablePath()
							)
						);
					}
				}
			}
			for( String operationName : currCorrelatingOperations ) {
				if ( correlatingOperations.contains( operationName ) ) {
					error( cset, "Operation " + operationName +
						" is specified on more than one correlation set. Each operation can correlate using only one correlation set."
					);
				} else {
					correlatingOperations.add( operationName );
					correlationFunctionInfo.operationCorrelationSetMap().put( operationName, cset );
					correlationFunctionInfo.correlationSetOperations().put( cset, operationName );
				}
			}
		}

		Collection< CorrelationPairInfo > pairs;
		for( Map.Entry< String, CorrelationSetInfo > entry : correlationFunctionInfo.operationCorrelationSetMap().entrySet() ) {
			pairs = correlationFunctionInfo.getOperationCorrelationPairs( entry.getKey() );
			if ( pairs.size() != entry.getValue().variables().size() ) {
				error( entry.getValue(), "Operation " + entry.getKey() +
						" has not an alias specified for every variable in the correlation set."
					);
			}
		}
	}

	private void checkCorrelationAlias( CorrelationAliasInfo alias )
	{
		TypeDefinition type = definedTypes.get( alias.guardName() );
		if ( type == null ) {
			error( alias.variablePath(), "type " + alias.guardName() + " is undefined" );
		}

		if ( type.containsPath( alias.variablePath() ) == false ) {
			error( alias.variablePath(), "type " + alias.guardName() + " does not contain the specified path" );
		}
	}

	public boolean validate()
	{
		program.accept( this );
		resolveLazyLinks();
		checkToBeEqualTypes();
		checkCorrelationSets();
		
		if ( configuration.checkForMain && mainDefined == false ) {
			error( null, "Main procedure not defined" );
		}
		
		if ( !valid ) {
			logger.severe( "Aborting: input file semantically invalid." );
			return false;
		}

		return valid;
	}

	private boolean isTopLevelType = true;

	public void visit( TypeInlineDefinition n )
	{
		checkCardinality( n );
		boolean backupRootType = isTopLevelType;
		if ( isTopLevelType ) {
			// Check if the type has already been defined with a different structure
			TypeDefinition type = definedTypes.get( n.id() );
			if ( type != null ) {
				addTypeEqualnessCheck( type, n );
			}
		}

		isTopLevelType = false;

		if ( n.hasSubTypes() ) {
			for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
				entry.getValue().accept( this );
			}
		}

		isTopLevelType = backupRootType;

		if ( isTopLevelType ) {
			definedTypes.put( n.id(), n );
		}
	}
	
	public void visit( TypeDefinitionLink n )
	{
		checkCardinality( n );
		if ( isTopLevelType ) {
			// Check if the type has already been defined with a different structure
			TypeDefinition type = definedTypes.get( n.id() );
			if ( type != null ) {
				addTypeEqualnessCheck( type, n );
			}
			definedTypes.put( n.id(), n );
		}
		definedTypeLinks.add( n );
	}

	private void checkCardinality( TypeDefinition type )
	{
		if ( type.cardinality().min() < 0 ) {
			error( type, "type " + type.id() + " specifies an invalid minimum range value (must be positive)" );
		}
		if ( type.cardinality().max() < 0 ) {
			error( type, "type " + type.id() + " specifies an invalid maximum range value (must be positive)" );
		}
	}

	public void visit( SpawnStatement n )
	{
		n.body().accept( this );
	}

	public void visit( DocumentationComment n )
	{}

	public void visit( Program n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	public void visit( VariablePathNode n )
	{
		if ( insideInit && n.isCSet() ) {
			error( n, "Correlation variable access is forbidden in init procedures" );
		}

		if ( n.isCSet() && !n.isStatic() ) {
			error( n, "Correlation paths must be statically defined" );
		}
	}

	public void visit( InputPortInfo n )
	{
		if ( inputPorts.get( n.id() ) != null ) {
			error( n, "input port " + n.id() + " has been already defined" );
		}
		inputPorts.put( n.id(), n );

		insideInputPort = true;

		Set< String > opSet = new HashSet< String >();

		for( OperationDeclaration op : n.operations() ) {
			if ( opSet.contains( op.id() ) ) {
				error( n, "input port " + n.id() + " declares operation " + op.id() + " multiple times" );
			} else {
				opSet.add( op.id() );
				op.accept( this );
			}
		}

		OutputPortInfo outputPort;
		for( String portName : n.aggregationList() ) {
			outputPort = outputPorts.get( portName );
			if ( outputPort == null ) {
				error( n, "input port " + n.id() + " aggregates an undefined output port (" + portName + ")" );
			}/* else {
				for( OperationDeclaration op : outputPort.operations() ) {
					if ( opSet.contains( op.id() ) ) {
						error( n, "input port " + n.id() + " declares duplicate operation " + op.id() + " from aggregated output port " + outputPort.id() );
					} else {
						opSet.add( op.id() );
					}
				}
			}*/
		}

		insideInputPort = false;
	}
	
	public void visit( OutputPortInfo n )
	{
		if ( outputPorts.get( n.id() ) != null )
			error( n, "output port " + n.id() + " has been already defined" );
		outputPorts.put( n.id(), n );

		encounteredAssignment( n.id() );

		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
		}
	}
		
	public void visit( OneWayOperationDeclaration n )
	{
		if ( definedTypes.get( n.requestType().id() ) == null ) {
			error( n, "unknown type: " + n.requestType().id() + " for operation " + n.id() );
		}
		if ( insideInputPort ) { // Input operation
			if ( oneWayOperations.containsKey( n.id() ) ) {
				OneWayOperationDeclaration other = oneWayOperations.get( n.id() );
				addOneWayEqualnessCheck( n, other );
			} else {
				oneWayOperations.put( n.id(), n );
				inputTypeNameMap.put( n.requestType().id(), n.id() );
			}
		}
	}
		
	public void visit( RequestResponseOperationDeclaration n )
	{
		if ( definedTypes.get( n.requestType().id() ) == null ) {
			error( n, "unknown type: " + n.requestType().id() + " for operation " + n.id() );
		}
		if ( definedTypes.get( n.responseType().id() ) == null ) {
			error( n, "unknown type: " + n.responseType().id() + " for operation " + n.id() );
		}
		for( Entry< String, TypeDefinition > fault : n.faults().entrySet() ) {
			if ( definedTypes.containsKey( fault.getValue().id() ) == false ) {
				error( n, "unknown type for fault " + fault.getKey() );
			}
		}

		if ( insideInputPort ) { // Input operation
			if ( requestResponseOperations.containsKey( n.id() ) ) {
				RequestResponseOperationDeclaration other = requestResponseOperations.get( n.id() );
				addRequestResponseEqualnessCheck( n, other );
			} else {
				requestResponseOperations.put( n.id(), n );
				inputTypeNameMap.put( n.requestType().id(), n.id() );
			}
		}
	}

	private void checkEqualness( OneWayOperationDeclaration n, OneWayOperationDeclaration other )
	{
		if ( n.requestType().isEquivalentTo( other.requestType() ) == false ) {
			error( n, "input operations sharing the same name cannot declare different request types (One-Way operation " + n.id() + ")" );
		}
	}
	
	private void checkEqualness( RequestResponseOperationDeclaration n, RequestResponseOperationDeclaration other )
	{
		if ( n.requestType().isEquivalentTo( other.requestType() ) == false ) {
			error( n, "input operations sharing the same name cannot declare different request types (Request-Response operation " + n.id() + ")" );
		}

		if ( n.responseType().isEquivalentTo( other.responseType() ) == false ) {
			error( n, "input operations sharing the same name cannot declare different response types (Request-Response operation " + n.id() + ")" );
		}

		if ( n.faults().size() != other.faults().size() ) {
			error( n, "input operations sharing the same name cannot declared different fault types (Request-Response operation " + n.id() );
		}

		for( Entry< String, TypeDefinition > fault : n.faults().entrySet() ) {
			if ( fault.getValue() != null ) {
				if ( !other.faults().containsKey( fault.getKey() ) || !other.faults().get( fault.getKey() ).isEquivalentTo( fault.getValue() ) ) {
					error( n, "input operations sharing the same name cannot declared different fault types (Request-Response operation " + n.id() );
				}
			}
		}
	}

	public void visit( DefinitionNode n )
	{
		if ( subroutineNames.contains( n.id() ) ) {
			error( n, "Procedure " + n.id() + " uses an already defined identifier" );
		} else {
			subroutineNames.add( n.id() );
		}
		
		if ( "main".equals( n.id() ) ) {
			mainDefined = true;
			if ( executionInfo.mode() != ExecutionMode.SINGLE ) {
				if ( ( n.body() instanceof NDChoiceStatement
					|| n.body() instanceof RequestResponseOperationStatement
					|| n.body() instanceof OneWayOperationStatement
					) == false
				) {
					// The main body is not an input
					if ( n.body() instanceof SequenceStatement ) {
						OLSyntaxNode first = ((SequenceStatement)n.body()).children().get( 0 );
						if ( (first instanceof RequestResponseOperationStatement
							|| first instanceof OneWayOperationStatement) == false
						) {
							// The main body is not even a sequence starting with an input
							error( n.body(),
								"The first statement of the main procedure must be an input if the execution mode is not single"
							);
						}
					} else {
						// The main body is not even a sequence
						error( n.body(),
							"The first statement of the main procedure must be an input if the execution mode is not single"
						);
					}
				}
			}
		}
		if ( n.id().equals( "init" ) ) {
			insideInit = true;
		}
		n.body().accept( this );
		insideInit = false;
	}
		
	public void visit( ParallelStatement stm )
	{
		for( OLSyntaxNode node : stm.children() ) {
			node.accept( this );
		}
	}
		
	public void visit( SequenceStatement stm )
	{
		for( OLSyntaxNode node : stm.children() ) {
			node.accept( this );
		}
	}
		
	public void visit( NDChoiceStatement stm )
	{
		Set< String > operations = new HashSet< String >();
		String name = null;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
			if ( pair.key() instanceof OneWayOperationStatement ) {
				name = ((OneWayOperationStatement)pair.key()).id();
			} else if ( pair.key() instanceof RequestResponseOperationStatement ) {
				name = ((RequestResponseOperationStatement)pair.key()).id();
			} else {
				error( pair.key(), "Input choices can contain only One-Way or Request-Response guards" );
			}
			if ( operations.contains( name ) ) {
				error( pair.key(), "Input choices can not have duplicate input guards (input statement for operation " + name + ")" );
			} else {
				operations.add( name );
			}
			pair.key().accept( this );
			pair.value().accept( this );
		}
	}
	
	public void visit( NotificationOperationStatement n )
	{
		OutputPortInfo p = outputPorts.get( n.outputPortId() );
		if ( p == null ) {
			error( n, n.outputPortId() + " is not a valid output port" );
		} else {
			OperationDeclaration decl = p.operationsMap().get( n.id() );
			if ( decl == null )
				error( n, "Operation " + n.id() + " has not been declared in output port type " + p.id() );
			else if ( !( decl instanceof OneWayOperationDeclaration ) )
				error( n, "Operation " + n.id() + " is not a valid one-way operation in output port " + p.id() );
		} 
	}
	
	public void visit( SolicitResponseOperationStatement n )
	{
		if ( n.inputVarPath() != null ) {
			encounteredAssignment( n.inputVarPath() );
		}
		OutputPortInfo p = outputPorts.get( n.outputPortId() );
		if ( p == null ) {
			error( n, n.outputPortId() + " is not a valid output port" );
		} else {
			OperationDeclaration decl = p.operationsMap().get( n.id() );
			if ( decl == null ) {
				error( n, "Operation " + n.id() + " has not been declared in output port " + p.id() );
			} else if ( !(decl instanceof RequestResponseOperationDeclaration) ) {
				error( n, "Operation " + n.id() + " is not a valid request-response operation in output port " + p.id() );
			}
		}

		/*if ( n.inputVarPath() != null && n.inputVarPath().isCSet() ) {
			error( n, "Receiving a message in a correlation variable is forbidden" );
		}*/
	}
	
	public void visit( ThrowStatement n )
	{
		verify( n.expression() );
	}

	public void visit( CompensateStatement n ) {}
	
	public void visit( InstallStatement n )
	{
		for( Pair< String, OLSyntaxNode > pair : n.handlersFunction().pairs() ) {
			pair.value().accept( this );
		}
	}

	public void visit( Scope n )
	{
		n.body().accept( this );
	}
	
	public void visit( OneWayOperationStatement n )
	{
		verify( n.inputVarPath() );
		if ( n.inputVarPath() != null ) {
			if ( n.inputVarPath().isCSet() ) {
				error( n, "Receiving a message in a correlation variable is forbidden" );
			}
			encounteredAssignment( n.inputVarPath() );
		}
	}

	public void visit( RequestResponseOperationStatement n )
	{
		verify( n.inputVarPath() );
		verify( n.process() );
		if ( n.inputVarPath() != null ) {
			if ( n.inputVarPath().isCSet() ) {
				error( n, "Receiving a message in a correlation variable is forbidden" );
			}
			encounteredAssignment( n.inputVarPath() );
		}
	}

	public void visit( LinkInStatement n ) {}
	public void visit( LinkOutStatement n ) {}

	public void visit( SynchronizedStatement n )
	{
		n.body().accept( this );
	}
		
	public void visit( AssignStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	public void visit( AddAssignStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	public void visit( SubtractAssignStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	public void visit( MultiplyAssignStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	public void visit( DivideAssignStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		n.expression().accept( this );
	}

	private void verify( OLSyntaxNode n )
	{
		if ( n != null ) {
			n.accept( this );
		}
	}

	public void visit( PointerStatement n )
	{
		encounteredAssignment( n.leftPath() );
		encounteredAssignment( n.rightPath() );
		n.leftPath().accept( this );
		n.rightPath().accept( this );

		if ( n.rightPath().isCSet() ) {
			error( n, "Making an alias to a correlation variable is forbidden" );
		}
	}
	
	public void visit( DeepCopyStatement n )
	{
		encounteredAssignment( n.leftPath() );
		n.leftPath().accept( this );
		n.rightPath().accept( this );
		if ( n.leftPath().isCSet() ) {
			error( n, "Deep copy on a correlation variable is forbidden" );
		}
	}

	public void visit( IfStatement n )
	{
		for( Pair< OLSyntaxNode, OLSyntaxNode > choice : n.children() ) {
			verify( choice.key() );
			verify( choice.value() );
		}
		verify( n.elseProcess() );
	}

	public void visit( DefinitionCallStatement n ) {}

	public void visit( WhileStatement n )
	{
		n.condition().accept( this );
		n.body().accept( this );
	}

	public void visit( OrConditionNode n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	public void visit( AndConditionNode n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	public void visit( NotConditionNode n )
	{
		n.condition().accept( this );
	}

	public void visit( CompareConditionNode n )
	{
		n.leftExpression().accept( this );
		n.rightExpression().accept( this );
	}

	public void visit( ExpressionConditionNode n ) 
	{
		n.expression().accept( this );
	}

	public void visit( ConstantIntegerExpression n ) {}
	public void visit( ConstantRealExpression n ) {}
	public void visit( ConstantStringExpression n ) {}

	public void visit( ProductExpressionNode n )
	{
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
		}
	}

	public void visit( SumExpressionNode n )
	{
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
		}
	}

	public void visit( VariableExpressionNode n )
	{
		n.variablePath().accept( this );
	}

	public void visit( InstallFixedVariableExpressionNode n )
	{
		n.variablePath().accept(  this );
	}

	public void visit( NullProcessStatement n ) {}

	public void visit( ExitStatement n ) {}

	public void visit( ExecutionInfo n )
	{
		executionMode = n.mode();
		executionInfo = n;
	}

	public void visit( CorrelationSetInfo n )
	{
		VariablePathSet pathSet = new VariablePathSet();

		VariablePathNode path;
		for( CorrelationSetInfo.CorrelationVariableInfo csetVar : n.variables() ) {
			path = csetVar.correlationVariablePath();
			if ( path.isGlobal() ) {
				error( path, "Correlation variables can not be global" );
			} else if ( path.isCSet() ) {
				error( path, "Correlation variables can not be in the csets structure" );
			} else {
				if ( path.isStatic() == false ) {
					error( path, "correlation variable paths can not make use of dynamic evaluation" );
				}
			}

			if ( pathSet.contains( path ) ) {
				error( path, "Duplicate correlation variable" );
			} else {
				pathSet.add( path );
			}

			for( CorrelationAliasInfo alias : csetVar.aliases() ) {
				if ( alias.variablePath().isGlobal() ) {
					error( alias.variablePath(), "Correlation variables can not be global" );
				} else if ( path.isCSet() ) {
					error( alias.variablePath(), "Correlation variables can not be in the csets structure" );
				} else {
					if ( alias.variablePath().isStatic() == false ) {
						error( alias.variablePath(), "correlation variable path aliases can not make use of dynamic evaluation" );
					}
				}
			}
		}
		correlationSets.add( n );
		/*VariablePathNode varPath;
		List< Pair< OLSyntaxNode, OLSyntaxNode > > path;
		for( List< VariablePathNode > list : n.variables() ) {
			varPath = list.get( 0 );
			if ( varPath.isGlobal() ) {
				error( list.get( 0 ), "Correlation variables can not be global" );
			}
			path = varPath.path();
			if ( path.size() > 1 ) {
				error( varPath, "Correlation variables can not be nested paths" );
			} else if ( path.get( 0 ).value() != null ) {
				error( varPath, "Correlation variables can not use arrays" );
			} else {
				correlationSet.add( ((ConstantStringExpression)path.get( 0 ).key()).value() );
			}
		}*/
	}

	public void visit( RunStatement n )
	{
		warning( n, "Run statement is not a stable feature yet." );
	}

	public void visit( ValueVectorSizeExpressionNode n )
	{
		n.variablePath().accept( this );
	}

	public void visit( PreIncrementStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	public void visit( PostIncrementStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	public void visit( PreDecrementStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	public void visit( PostDecrementStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
	}

	public void visit( UndefStatement n )
	{
		encounteredAssignment( n.variablePath() );
		n.variablePath().accept( this );
		if ( n.variablePath().isCSet() ) {
			error( n, "Undefining a correlation variable is forbidden" );
		}
	}

	
	public void visit( ForStatement n )
	{
		n.init().accept( this );
		n.condition().accept( this );
		n.post().accept( this );
		n.body().accept( this );
	}

	public void visit( ForEachStatement n )
	{
		n.keyPath().accept( this );
		n.targetPath().accept( this );
		n.body().accept( this );
	}

	public void visit( IsTypeExpressionNode n )
	{
		n.variablePath().accept( this );
	}

	public void visit( TypeCastExpressionNode n )
	{
		n.expression().accept( this );
	}

	public void visit( EmbeddedServiceNode n )
	{}
	
	/**
	 * @todo Must check if it's inside an install function
	 */
	public void visit( CurrentHandlerStatement n )
	{}

	public void visit( InterfaceDefinition n )
	{}
}
