package org.logicng.knowledgecompilation.dnnf;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;

import java.util.BitSet;

public interface DNNFSATSolver {

    void add(final Formula formula);

    boolean start();

    boolean decide(final int var, boolean phase);

    void undoDecide(final int var);

    boolean atAssertionLevel();

    boolean assertCdLiteral();

    Formula newlyImplied(final BitSet knownVariables);

    int variableIndex(final Literal var);

    Literal litForIdx(final int var);

    Tristate valueOf(final int lit);

    int highestVarActivity(final BitSet separator);
}
