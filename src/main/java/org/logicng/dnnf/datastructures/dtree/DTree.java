package org.logicng.dnnf.datastructures.dtree;

import org.logicng.dnnf.DnnfSATSolver;
import org.logicng.formulas.Variable;

import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;

/**
 * Interface for a Decomposition Tree (DTree) for the DnnfCompiler
 * This is either a DTreeNode or a DTreeLeaf
 */
public interface DTree {

    void initialize(final DnnfSATSolver solver);

    int size();

    /**
     * Returns all variables of this DTree.
     * <p>
     * Since this set of variables can be cached, this is a constant time operation.
     * @return all variables of this DTree
     */
    int[] staticVarSetArray();

    BitSet staticVarSet();

    SortedSet<Variable> staticLiteralSet();

    /**
     * Returns the "static" separator of this DTree.  All clauses/variables are considered, whether they are subsumed or not.
     * @return the separator of this DTree
     */
    int[] staticSeparator();

    /**
     * Computes the dynamic variable set of this DTree.  "Dynamic" means that subsumed clauses are ignored during the computation.
     * The dynamic variable set includes all variables which:
     * - are contained in the given set (interestingVars)
     * - are not yet assigned and
     * - occur in clauses that are currently not subsumed
     * @return The dynamic variable set of this DTree
     */
    void dynamicVarSet(final BitSet vars);

    /**
     * The dynamic separator of this DTree.  "Dynamic" means that subsumed clauses are ignored during the separator computation.
     * @return The dynamic separator of this DTree
     */
    BitSet dynamicSeparator();

    /**
     * The ids clauses in this DTree.
     * @return The clause ids
     */
    int[] staticClauseIds();

    /**
     * The ids of all non-subsumed clauses in this DTree.
     * @param key
     * @param numberOfVariables
     * @return The dynamic clause ids
     */
    void cacheKey(final BitSet key, final int numberOfVariables);

    /**
     * Counts the number of unsubsumed occurrences for each variable in occurrences.
     * <p>
     * The parameter occurrences should be modified by the method accordingly.
     * @param occurrences The current number of occurrences for each variable which should be modified accordingly
     */
    //TODO: Find an alternative for Map<Integer, Integer>
    void countUnsubsumedOccurrences(final int[] occurrences);

    int depth();

    int widestSeparator();

    List<DTreeLeaf> leafs();
    //  void toFileWithFormatter(DTreeFormatter formatter, File file) throws IOException;
}
