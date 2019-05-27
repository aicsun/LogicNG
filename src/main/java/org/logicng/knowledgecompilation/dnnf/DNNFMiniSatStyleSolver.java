package org.logicng.knowledgecompilation.dnnf;

import org.logicng.collections.LNGIntVector;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.solvers.datastructures.MSClause;
import org.logicng.solvers.datastructures.MSVariable;
import org.logicng.solvers.sat.MiniSat2Solver;
import org.logicng.solvers.sat.MiniSatStyleSolver;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public final class DNNFMiniSatStyleSolver extends MiniSat2Solver implements DNNFSATSolver {

    private boolean newlyImpliedDirty = false;
    private int assertionLevel = -1;
    private LNGIntVector lastLearnt = null;
    private final FormulaFactory f;
    private final Tristate[] assignment;

    // reduce object allocation
    private final List<Literal> impliedOperands;

    public DNNFMiniSatStyleSolver(final FormulaFactory f, final int numberOfVariables) {
        this.f = f;
        this.assignment = new Tristate[2 * numberOfVariables];
        for (int i = 0; i < this.assignment.length; i++) {
            this.assignment[i] = Tristate.UNDEF;
        }
        this.impliedOperands = new ArrayList<>();
    }

    @Override
    public boolean start() {
        this.newlyImpliedDirty = true;
        return propagate() == null;
    }

    @Override
    public Tristate valueOf(final int lit) {
        return this.assignment[lit];
    }

    @Override
    public int highestVarActivity(final BitSet separator) {
        double max = -1;
        int maxVar = -1;
        for (int next = separator.nextSetBit(0); next != -1; next = separator.nextSetBit(next + 1)) {
            final double act = this.vars.get(next).activity();
            if (act > max) {
                maxVar = next;
                max = act;
            }
        }
        return maxVar;
    }

    @Override
    public int variableIndex(final Literal var) {
        return idxForName(var.name());
    }

    @Override
    public Literal litForIdx(final int var) {
        return this.f.literal(this.idx2name.get(var), true);
    }

    public static int var(final int lit) {
        return MiniSatStyleSolver.var(lit);
    }

    public static boolean phase(final int lit) {
        return !sign(lit);
    }

    @Override
    public void add(final Formula formula) {
        final Formula cnf = formula.cnf();
        switch (cnf.type()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                this.addClause(generateClauseVector(cnf.literals()), null);
                break;
            case AND:
                for (final Formula op : cnf) {
                    this.addClause(generateClauseVector(op.literals()), null);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + cnf);
        }
    }

    private LNGIntVector generateClauseVector(final Collection<Literal> literals) {
        final LNGIntVector clauseVec = new LNGIntVector(literals.size());
        for (final Literal lit : literals) {
            int index = idxForName(lit.name());
            if (index == -1) {
                index = newVar(false, true);
                addName(lit.name(), index);
            }
            final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
            clauseVec.push(litNum);
        }
        return clauseVec;
    }

    @Override
    public boolean decide(final int var, final boolean phase) {
        this.newlyImpliedDirty = true;
        final int lit = mkLit(var, !phase);
        this.trailLim.push(this.trail.size());
        uncheckedEnqueue(lit, null);
        return propagateAfterDecide();
    }

    @Override
    public void undoDecide(final int var) {
        this.newlyImpliedDirty = false;
        cancelUntil(this.vars.get(var).level() - 1);
    }

    @Override
    public boolean atAssertionLevel() {
        return decisionLevel() == this.assertionLevel;
    }

    @Override
    public boolean assertCdLiteral() {
        this.newlyImpliedDirty = true;
        if (!atAssertionLevel()) {
            throw new IllegalStateException("assertCdLiteral called although not at assertion level!");
        }

        if (this.lastLearnt.size() == 1) {
            uncheckedEnqueue(this.lastLearnt.get(0), null);
            this.unitClauses.push(this.lastLearnt.get(0));
        } else {
            final MSClause cr = new MSClause(this.lastLearnt, true);
            this.learnts.push(cr);
            attachClause(cr);
            if (!this.incremental) {
                claBumpActivity(cr);
            }
            uncheckedEnqueue(this.lastLearnt.get(0), cr);
        }
        varDecayActivity();
        if (!this.incremental) {
            claDecayActivity();
        }
        if (--this.learntsizeAdjustCnt == 0) {
            this.learntsizeAdjustConfl *= this.learntsizeAdjustInc;
            this.learntsizeAdjustCnt = (int) this.learntsizeAdjustConfl;
            this.maxLearnts *= this.learntsizeInc;
        }

        return propagateAfterDecide();
    }

    @Override
    public Formula newlyImplied(final BitSet knownVariables) {
        this.impliedOperands.clear();
        if (this.newlyImpliedDirty) {
            final int limit = this.trailLim.empty() ? -1 : this.trailLim.back();
            for (int i = this.trail.size() - 1; i > limit; i--) {
                final int lit = this.trail.get(i);
                if (knownVariables.get(var(lit))) {
                    this.impliedOperands.add(intToLiteral(lit));
                }
            }
        }
        this.newlyImpliedDirty = false;
        return this.f.and(this.impliedOperands);
    }

    private Literal intToLiteral(final int lit) {
        final String name = nameForIdx(var(lit));
        return this.f.literal(name, !sign(lit));
    }

    private boolean propagateAfterDecide() {
        final MSClause conflict = propagate();
        if (conflict != null) {
            handleConflict(conflict);
            return false;
        }
        return true;
    }

    @Override
    protected void uncheckedEnqueue(final int lit, final MSClause reason) {
        this.assignment[lit] = Tristate.TRUE;
        this.assignment[lit ^ 1] = Tristate.FALSE;
        super.uncheckedEnqueue(lit, reason);
    }

    @Override
    protected void cancelUntil(final int level) {
        if (decisionLevel() > level) {
            for (int c = this.trail.size() - 1; c >= this.trailLim.get(level); c--) {
                final int l = this.trail.get(c);
                this.assignment[l] = Tristate.UNDEF;
                this.assignment[l ^ 1] = Tristate.UNDEF;
                final int x = var(l);
                final MSVariable v = this.vars.get(x);
                v.assign(Tristate.UNDEF);
                v.setPolarity(sign(this.trail.get(c)));
                insertVarOrder(x);
            }
            this.qhead = this.trailLim.get(level);
            this.trail.removeElements(this.trail.size() - this.trailLim.get(level));
            this.trailLim.removeElements(this.trailLim.size() - level);
        }
    }

    private void handleConflict(final MSClause conflict) {
        if (decisionLevel() > 0) {
            this.lastLearnt = new LNGIntVector();
            analyze(conflict, this.lastLearnt);
            this.assertionLevel = this.analyzeBtLevel;
        } else {
            // solver unsat
            cancelUntil(0);
            this.lastLearnt = null;
            this.assertionLevel = -1;
        }
    }
}
