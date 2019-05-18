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
 * Removes all tautology subformulas from a formula under the given precondition.
 * <p>
 * Example 1:
 * Given the precondition b and the formula a &amp (b | c), the result after the tautology removal would be a.
 * @version 2.0.0
 * @since 2.0.0
 */
public class TautologyRemoval implements FormulaTransformation {

    private final SATSolver solver;
    private final SolverState solverState;

    /**
     * Constructs a tautology removal transformation with no precondition.
     * @param f formula factory
     */
    public TautologyRemoval(final FormulaFactory f) {
        this.solver = MiniSat.miniSat(f);
        this.solverState = null;
    }

    /**
     * Constructs a tautology removal transformation with the given precondition.
     * @param precondition the precondition
     */
    public TautologyRemoval(final Formula precondition) {
        this.solver = MiniSat.miniSat(precondition.factory());
        this.solver.add(precondition);
        this.solverState = this.solver.saveState();
    }

    /**
     * Constructs a tautology removal transformation with the given SAT solver.  If there are already formulas on
     * the solver, these formulas are kept and the transformation is performed against these formulas.  The solver state
     * is not changed.
     * @param solver the SAT solver
     */
    public TautologyRemoval(final SATSolver solver) {
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

    private boolean isImplied(final Formula formula) {
        this.solver.add(formula.negate());
        final boolean isImplied = this.solver.sat() == Tristate.FALSE;
        loadStateOrElseReset(this.solver, this.solverState);
        return isImplied;
    }

    @Override
    public Formula apply(final Formula formula, final boolean cache) {
        final FormulaFactory f = formula.factory();
        switch (formula.type()) {
            case TRUE:
                return formula;
            case FALSE:
                if (this.solver.sat() == Tristate.FALSE) {
                    return f.verum();
                } else {
                    return formula;
                }
            case LITERAL:
            case PBC:
                if (isImplied(formula)) {
                    return f.verum();
                } else {
                    return formula;
                }
            case NOT:
                if (isImplied(formula)) {
                    return f.verum();
                } else {
                    final Not not = (Not) formula;
                    return f.not(apply(not.operand(), cache));
                }
            case AND:
            case OR:
                // TODO superclass to avoid duplicate code by an removal superclass or merging both removal classes into one?
                if (isImplied(formula)) {
                    return f.verum();
                } else {
                    final List<Formula> newOps = new ArrayList<>(formula.numberOfOperands());
                    for (final Formula op : formula) {
                        newOps.add(op.transform(this));
                    }
                    return formula.type() == FType.AND ? f.and(newOps) : f.or(newOps);
                }
            case IMPL:
            case EQUIV:
                if (isImplied(formula)) {
                    return f.verum();
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
