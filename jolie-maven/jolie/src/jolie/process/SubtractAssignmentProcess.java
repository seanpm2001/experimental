/***************************************************************************
 *   Copyright (C) 2011 by Karoly Szanto                                   *
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

import jolie.ExecutionThread;
import jolie.runtime.Expression;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * Subtract an expression value from a VariablePath's value, assigning the resulting
 * value to the VariablePath.
 * @see Expression
 * @see VariablePath
 * @author Karoly Szanto
 */
public class SubtractAssignmentProcess implements Process, Expression
{
	final private VariablePath varPath;
	final private Expression expression;

	/** Constructor.
	 *
	 * @param varPath the variable which will receive the value
	 * @param expression the expression to be evaluated and subtracted from the variable's value
	 * @throws InvalidIdException if varId does not identify a variable
	 */
	public SubtractAssignmentProcess( VariablePath varPath, Expression expression )
	{
		this.varPath = varPath;
		this.expression = expression;
	}

	public Process clone( TransformationReason reason )
	{
		return new AssignmentProcess(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ) );
	}

	public Expression cloneExpression( TransformationReason reason )
	{
		return new AssignmentProcess(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ) );
	}

	/** Evaluates the expression and adds its value to the variable's value. */
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		varPath.getValue().subtract( expression.evaluate() );
	}

	public Value evaluate()
	{
		Value val = varPath.getValue();
		val.subtract( expression.evaluate() );
		return val;
	}

	public boolean isKillable()
	{
		return true;
	}
}
