package org.logicng.primeimplicant;

import org.logicng.collections.LNGIntVector;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.datastructures.MSVariable;
import org.logicng.solvers.sat.MiniSat2Solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Prime implicant generation.
 *
 * This class provides methods to generate a prime implicant for a formula.
 * For a formula, an arbitrary prime implicant can be generated or an already known implicant can be used to extract
 * a prime implicant from.
 * @version 1.6.0
 * @since 1.6.0
 */
public class PrimeImplicantGeneration {

    private static final MiniSatPrimeImplicant solver = new MiniSatPrimeImplicant();

    /**
     * Private constructor.
     */
    private PrimeImplicantGeneration() {
        // Intentionally left empty.
    }

    /**
     * Computes any prime implicant from the given formula.
     * @param formula formula
     * @return prime implicant or {@code null} if the formula is unsatisfiable
     */
    public static SortedSet<Literal> compute(final Formula formula) {
        solver.reset();
        return solver.compute(formula);
    }

    /**
     * Computes any prime implicant from the given formula on basis of the given implicant.
     * The resulting prime implicant will therefore consist of a subset (or the same set, if the implicant was already
     * an prime implicant) of the implicant.
     *
     * ATTENTION: The method does not check if the given implicant is really an implicant of the formula for performance
     * reasons. Ensure that you call this method only if "implicant => formula" holds.
     * @param formula   formula
     * @param implicant implicant to compute any prime implicant from
     * @return prime implicant or {@code null} if the formula is unsatisfiable
     */
    public static SortedSet<Literal> computeFrom(final Formula formula, final SortedSet<Literal> implicant) {
        solver.reset();
        return solver.computeFrom(formula, implicant);
    }

    private static class MiniSatPrimeImplicant extends MiniSat2Solver {

        private void newVars(final SortedSet<Variable> variables) {
            for (final Variable var : variables) {
                final int index = this.newVar(false, true);
                this.addName(var.name(), index);
            }
        }

        private LNGIntVector generateClauseVector(final Formula clause) {
            final LNGIntVector clauseVec = new LNGIntVector(clause.numberOfOperands());
            for (final Literal lit : clause.literals()) {
                int index = this.idxForName(lit.name());
                if (index == -1) {
                    index = this.newVar(false, true);
                    this.addName(lit.name(), index);
                }
                final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
                clauseVec.push(litNum);
            }
            return clauseVec;
        }

        private void add(final Formula formula) {
            final Formula cnf = formula.cnf();
            switch (cnf.type()) {
                case TRUE:
                    break;
                case FALSE:
                case LITERAL:
                case OR:
                    this.addClause(generateClauseVector(cnf), null);
                    break;
                case AND:
                    for (final Formula op : cnf) {
                        this.addClause(generateClauseVector(op), null);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected formula type in CNF: " + cnf.type());
            }
        }

        private List<Integer> getRelevantVarIndices(final Collection<Variable> variables) {
            final List<Integer> relevantVarIndices = new ArrayList<>(variables.size());
            for (final Variable var : variables) {
                final Integer idx = this.name2idx.get(var.name());
                if (idx != null) {
                    relevantVarIndices.add(idx);
                }
            }
            return relevantVarIndices;
        }

        private LNGIntVector toLNGIntVector(final SortedSet<Integer> literals) {
            final LNGIntVector vector = new LNGIntVector(literals.size());
            for (final Integer lit : literals) {
                vector.push(lit);
            }
            return vector;
        }

        private SortedSet<Integer> compute(final SortedSet<Integer> implicant, final SortedSet<Integer> dontCareCandidates) {
            final SortedSet<Integer> primeImplicant = new TreeSet<>(implicant);
            for (final Integer lit : dontCareCandidates) {
                primeImplicant.remove(lit);
                final LNGIntVector assumptions = toLNGIntVector(primeImplicant);
                assumptions.push(not(lit));
                if (solve(null, assumptions) == Tristate.TRUE) {
                    primeImplicant.add(lit);
                }
            }
            return primeImplicant;
        }

        private SortedSet<Literal> toLiterals(final FormulaFactory f, final SortedSet<Integer> intLiterals) {
            final SortedSet<Literal> literals = new TreeSet<>();
            for (final Integer intLit : intLiterals) {
                final Integer var = var(intLit);
                literals.add(f.literal(this.idx2name.get(var), !sign(intLit)));
            }
            return literals;
        }

        SortedSet<Literal> compute(final Formula formula) {
            final SortedSet<Variable> variables = formula.variables();
            // Note: Make formula variables known to solver in advance,
            // otherwise the name-index mapping gets lost when loading a previous solver state
            newVars(variables);
            final int[] state = saveState();
            add(formula);
            if (solve(null) == Tristate.TRUE) {
                final SortedSet<Integer> implicant = new TreeSet<>();
                final SortedSet<Integer> dontCareCandidates = new TreeSet<>();
                for (final Integer var : getRelevantVarIndices(variables)) {
                    final int lit = mkLit(var, !this.model.get(var));
                    final MSVariable msVariable = this.vars.get(var);
                    if (msVariable.level() > 0) {
                        dontCareCandidates.add(lit);
                    }
                    implicant.add(lit);
                }
                loadState(state);
                add(formula.negate());
                return toLiterals(formula.factory(), compute(implicant, dontCareCandidates));
            } else {
                return null;
            }
        }

        SortedSet<Literal> computeFrom(final Formula formula, final SortedSet<Literal> implicant) {
            add(formula.negate());
            final SortedSet<Integer> intImplicant = new TreeSet<>();
            for (final Literal lit : implicant) {
                final Integer idx = this.name2idx.get(lit.name());
                if (idx != null) {
                    final int intLit = mkLit(idx, !lit.phase());
                    intImplicant.add(intLit);
                }
            }
            return toLiterals(formula.factory(), compute(intImplicant, intImplicant));
        }
    }
}