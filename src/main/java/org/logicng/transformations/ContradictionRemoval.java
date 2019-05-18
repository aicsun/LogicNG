package org.logicng.transformations;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaTransformation;
import org.logicng.formulas.Not;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.SolverState;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes all contradictory subformulas from a formula under the given precondition.
 * <p>
 * Example 1:
 * Given the precondition b and the formula a | (~b & c), the result after the contradiction removal would be a.
 * @version 2.0.0
 * @since 2.0.0
 */
public class ContradictionRemoval implements FormulaTransformation {

    private final SATSolver solver;
    private final SolverState solverState;

    /**
     * Constructs a contradiction removal transformation with no precondition.
     * @param f formula factory
     */
    public ContradictionRemoval(final FormulaFactory f) {
        this.solver = MiniSat.miniSat(f);
        this.solverState = null;
    }

    /**
     * Constructs a contradiction removal transformation with the given precondition.
     * @param precondition the precondition
     */
    public ContradictionRemoval(final Formula precondition) {
        this.solver = MiniSat.miniSat(precondition.factory());
        this.solver.add(precondition);
        this.solverState = this.solver.saveState();
    }

    /**
     * Constructs a contradiction removal transformation with the given SAT solver.  If there are already formulas on
     * the solver, these formulas are kept and the transformation is performed against these formulas.  The solver state
     * is not changed.
     * @param solver the SAT solver
     */
    public ContradictionRemoval(final SATSolver solver) {
        this.solver = solver;
        this.solverState = solver.saveState();
    }

    /**
     * Loads the given state of the given solver. If the given state is {@code null} the solver be reset.
     * @param solver solver
     * @param state  state to load or {@code null} if solver should be reset
     */
    protected static void loadStateOrElseReset(final SATSolver solver, final SolverState state) {
        // TODO move method to SATSolver interface? SATPredicate also uses it
        if (state == null) {
            solver.reset();
        } else {
            solver.loadState(state);
        }
    }

    private boolean isContradiction(final Formula formula) {
        this.solver.add(formula);
        final boolean isContradiction = this.solver.sat() == Tristate.FALSE;
        loadStateOrElseReset(this.solver, this.solverState);
        return isContradiction;
    }

    @Override
    public Formula apply(final Formula formula, final boolean cache) {
        final FormulaFactory f = formula.factory();
        switch (formula.type()) {
            case TRUE:
                if (this.solver.sat() == Tristate.FALSE) {
                    return f.falsum();
                } else {
                    return formula;
                }
            case FALSE:
                return formula;
            case LITERAL:
            case PBC:
                if (isContradiction(formula)) {
                    return f.falsum();
                } else {
                    return formula;
                }
            case NOT:
                if (isContradiction(formula)) {
                    return f.falsum();
                } else {
                    final Not not = (Not) formula;
                    return f.not(apply(not.operand(), cache));
                }
            case AND:
            case OR:
                if (isContradiction(formula)) {
                    return f.falsum();
                } else {
                    final List<Formula> newOps = new ArrayList<>(formula.numberOfOperands());
                    for (final Formula op : formula) {
                        newOps.add(op.transform(this));
                    }
                    return formula.type() == FType.AND ? f.and(newOps) : f.or(newOps);
                }
            case IMPL:
            case EQUIV:
                if (isContradiction(formula)) {
                    return f.falsum();
                } else {
                    final BinaryOperator binaryOperator = (BinaryOperator) formula;
                    final Formula newLeft = binaryOperator.left().transform(this);
                    final Formula newRight = binaryOperator.right().transform(this);
                    return formula.type() == FType.IMPL ? f.implication(newLeft, newRight) : f.equivalence(newLeft, newRight);
                }
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.type());
        }
    }
}
