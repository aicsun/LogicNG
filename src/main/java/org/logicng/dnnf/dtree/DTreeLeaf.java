package org.logicng.dnnf.dtree;

import org.logicng.datastructures.Tristate;
import org.logicng.dnnf.DnnfSATSolver;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.sat.MiniSatStyleSolver;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

public class DTreeLeaf implements DTree {

    private final int id;
    private final Formula clause;
    private final int clauseSize;

    private BitSet varSet;
    private int[] variables;
    private int[] literals;
    private int[] dynamicVarSetHelper;

    private final int[] separator = new int[0];
    private final BitSet separatorBitSet = new BitSet();

    private DnnfSATSolver solver;
    private final int[] staticClauseIds;

    public DTreeLeaf(final int id, final Formula clause) {
        this.id = id;
        this.clause = clause;
        this.staticClauseIds = new int[]{id};
        this.clauseSize = clause.variables().size();
        assert this.clauseSize >= 2;
    }

    @Override
    public void initialize(final DnnfSATSolver solver) {
        this.solver = solver;
        final SortedSet<Literal> lits = this.clause.literals();
        final int size = lits.size();
        this.varSet = new BitSet();
        this.variables = new int[size];
        this.literals = new int[size];
        int i = 0;
        for (final Literal literal : lits) {
            final int var = solver.variableIndex(literal);
            this.varSet.set(var);
            this.variables[i] = var;
            this.literals[i] = MiniSatStyleSolver.mkLit(var, !literal.phase());
            i++;
        }
        this.dynamicVarSetHelper = new int[size];
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public int[] staticVarSetArray() {
        return this.variables;
    }

    @Override
    public BitSet staticVarSet() {
        return this.varSet;
    }

    @Override
    public SortedSet<Variable> staticLiteralSet() {
        return this.clause.variables();
    }

    @Override
    public int[] staticSeparator() {
        return this.separator;
    }

    @Override
    public void dynamicVarSet(final BitSet vars) {
        //    dynamicVarSetN(vars);
        if (this.clauseSize < 2) {
            return;
        }
        if (this.clauseSize == 2) {
            dynamicVarSet2(vars);
        } else {
            dynamicVarSetN(vars);
        }
    }

    //  private void dynamicVarSet1(final BitSet vars) {
    //     do nothing -- SAT Solver will have
    //  }

    private boolean dynamicVarSet2(final BitSet vars) {
        final int lit0 = this.literals[0];
        final Tristate lit0Val = this.solver.valueOf(lit0);
        if (lit0Val == Tristate.TRUE) {
            return true;
        }
        final int lit1 = this.literals[1];
        final Tristate lit1Val = this.solver.valueOf(lit1);
        if (lit1Val == Tristate.TRUE) {
            return true;
        }
        if (lit0Val == Tristate.UNDEF) {
            vars.set(MiniSatStyleSolver.var(lit0));
        }
        if (lit1Val == Tristate.UNDEF) {
            vars.set(MiniSatStyleSolver.var(lit1));
        }
        return false;
    }

    private boolean dynamicVarSetN(final BitSet vars) {
        int toAdd = 0;
        for (int i = 0; i < this.literals.length; i++) {
            final int literal = this.literals[i];
            switch (this.solver.valueOf(literal)) {
                case TRUE:
                    if (i > 0) {
                        // find satisfied literal faster next time
                        this.literals[i] = this.literals[0];
                        this.literals[0] = literal;
                    }
                    return true;
                case UNDEF:
                    this.dynamicVarSetHelper[toAdd++] = MiniSatStyleSolver.var(literal);
            }
        }
        for (int i = 0; i < toAdd; i++) {
            vars.set(this.dynamicVarSetHelper[i]);
        }
        return false;
    }

    @Override
    public BitSet dynamicSeparator() {
        return this.separatorBitSet;
    }

    @Override
    public int[] staticClauseIds() {
        return this.staticClauseIds;
    }

    @Override
    public void cacheKey(final BitSet key, final int numberOfVariables) {
        final boolean subsumed = this.clauseSize == 2 ? dynamicVarSet2(key) : dynamicVarSetN(key);
        if (!subsumed) {
            key.set(numberOfVariables + this.id);
        }
    }

    private boolean isSubsumed() {
        for (final int literal : this.literals) {
            if (this.solver.valueOf(literal) == Tristate.TRUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void countUnsubsumedOccurrences(final int[] occurrences) {
        if (!isSubsumed()) {
            for (final int var : this.variables) {
                final int occ = occurrences[var];
                if (occ != -1) {
                    ++occurrences[var];
                }
            }
        }
    }

    @Override
    public int depth() {
        return 1;
    }

    @Override
    public int widestSeparator() {
        return 0;
    }

    @Override
    public List<DTreeLeaf> leafs() {
        final List<DTreeLeaf> result = new LinkedList<>();
        result.add(this);
        return result;
    }

    public Formula clause() {
        return this.clause;
    }

    @Override
    public String toString() {
        return String.format("DTreeLeaf: %d, %s", this.id, this.clause);
    }

    public int[] literals() {
        return this.literals;
    }

    public int clauseSize() {
        return this.clauseSize;
    }

    public int getId() {
        return this.id;
    }
}
