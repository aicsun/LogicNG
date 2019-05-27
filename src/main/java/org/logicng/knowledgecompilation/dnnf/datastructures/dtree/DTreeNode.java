package org.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import org.logicng.collections.LNGIntVector;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Variable;
import org.logicng.knowledgecompilation.dnnf.DNNFSATSolver;
import org.logicng.solvers.sat.MiniSatStyleSolver;

import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class DTreeNode implements DTree {

    private final DTree left;
    private final DTree right;
    private final int size;

    private DNNFSATSolver solver;

    private int[] staticVariables;
    private int[] staticSeparator;
    private BitSet staticVarSet;
    private final SortedSet<Variable> staticLiteralSet;
    private final BitSet staticSeparatorBitSet;
    private final int[] staticClauseIds;
    private final int depth;
    private int widestSeparator;

    private final DTreeLeaf[] leafs; // all leafs
    private final DTreeLeaf[] leftLeafs;
    private final DTreeLeaf[] rightLeafs;

    private int[] clauseContents; // content of all clauses under this node, e.g. clause {1,3} with id 0, {2,6,8} with id 1, {4,6} with id 6 --> [1,3,-1,2,6,-2,4,6,-7]
    private int[] leftClauseContents;
    private int[] rightClauseContents;

    private BitSet localLeftVarSet;
    private BitSet localRightVarSet;

    public DTreeNode(final DTree left, final DTree right) {
        this.left = left;
        this.right = right;
        this.size = left.size() + right.size();

        final List<DTreeLeaf> ll = left.leafs();
        excludeUnitLeafs(ll);
        this.leftLeafs = ll.toArray(new DTreeLeaf[ll.size()]);
        final List<DTreeLeaf> rl = right.leafs();
        excludeUnitLeafs(rl);
        this.rightLeafs = rl.toArray(new DTreeLeaf[rl.size()]);
        this.leafs = new DTreeLeaf[this.leftLeafs.length + this.rightLeafs.length];
        System.arraycopy(this.leftLeafs, 0, this.leafs, 0, this.leftLeafs.length);
        System.arraycopy(this.rightLeafs, 0, this.leafs, this.leftLeafs.length, this.rightLeafs.length);

        this.staticLiteralSet = new TreeSet<>(left.staticLiteralSet());
        this.staticLiteralSet.addAll(right.staticLiteralSet());
        this.staticSeparatorBitSet = new BitSet();
        final int[] leftClauseIds = left.staticClauseIds();
        final int[] rightClauseIds = right.staticClauseIds();
        this.staticClauseIds = new int[leftClauseIds.length + rightClauseIds.length];
        System.arraycopy(leftClauseIds, 0, this.staticClauseIds, 0, leftClauseIds.length);
        System.arraycopy(rightClauseIds, 0, this.staticClauseIds, leftClauseIds.length, rightClauseIds.length);
        this.depth = 1 + Math.max(left.depth(), right.depth());
    }

    public DTree left() {
        return this.left;
    }

    public DTree right() {
        return this.right;
    }

    @Override
    public void initialize(final DNNFSATSolver solver) {
        this.solver = solver;
        this.left.initialize(solver);
        this.right.initialize(solver);
        this.staticVarSet = this.left.staticVarSet();
        this.staticVarSet.or(this.right.staticVarSet());
        this.staticVariables = toArray(this.staticVarSet);
        this.staticSeparator = sortedIntersect(this.left.staticVarSetArray(), this.right.staticVarSetArray());
        for (final int i : this.staticSeparator) {
            this.staticSeparatorBitSet.set(i);
        }
        this.widestSeparator = Math.max(this.staticSeparator.length, Math.max(this.left.widestSeparator(), this.right.widestSeparator()));
        this.localLeftVarSet = new BitSet(this.staticVariables[this.staticVariables.length - 1]);
        this.localRightVarSet = new BitSet(this.staticVariables[this.staticVariables.length - 1]);

        final LNGIntVector lClauseContents = new LNGIntVector();
        for (final DTreeLeaf leaf : this.leftLeafs) {
            for (final int i : leaf.literals()) {
                lClauseContents.push(i);
            }
            lClauseContents.push(-leaf.getId() - 1);
        }
        this.leftClauseContents = lClauseContents.toArray();
        final LNGIntVector rClauseContents = new LNGIntVector();
        for (final DTreeLeaf leaf : this.rightLeafs) {
            for (final int i : leaf.literals()) {
                rClauseContents.push(i);
            }
            rClauseContents.push(-leaf.getId() - 1);
        }
        this.rightClauseContents = rClauseContents.toArray();
        this.clauseContents = new int[this.leftClauseContents.length + this.rightClauseContents.length];
        System.arraycopy(this.leftClauseContents, 0, this.clauseContents, 0, this.leftClauseContents.length);
        System.arraycopy(this.rightClauseContents, 0, this.clauseContents, this.leftClauseContents.length, this.rightClauseContents.length);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int[] staticVarSetArray() {
        return this.staticVariables;
    }

    @Override
    public BitSet staticVarSet() {
        return this.staticVarSet;
    }

    @Override
    public SortedSet<Variable> staticLiteralSet() {
        return this.staticLiteralSet;
    }

    @Override
    public int[] staticSeparator() {
        return this.staticSeparator;
    }

    @Override
    public void dynamicVarSet(final BitSet vars) {
        for (final DTreeLeaf leaf : this.leafs) {
            leaf.dynamicVarSet(vars);
        }
    }

    @Override
    public BitSet dynamicSeparator() {
        this.localLeftVarSet.clear();
        this.localRightVarSet.clear();
        //    for (final DTreeLeaf leaf : leftLeafs)
        //      leaf.dynamicVarSet(localLeftVarSet);
        //    for (final DTreeLeaf leaf : rightLeafs)
        //      leaf.dynamicVarSet(localRightVarSet);
        varSet(this.leftClauseContents, this.localLeftVarSet);
        varSet(this.rightClauseContents, this.localRightVarSet);
        this.localLeftVarSet.and(this.localRightVarSet);
        return this.localLeftVarSet;
    }

    private void varSet(final int[] clausesContents, final BitSet localVarSet) {
        int i = 0;
        while (i < clausesContents.length) {
            int j = i;
            boolean subsumed = false;
            while (clausesContents[j] >= 0) {
                if (!subsumed && this.solver.valueOf(clausesContents[j]) == Tristate.TRUE) {
                    subsumed = true;
                }
                j++;
            }
            if (!subsumed) {
                for (int n = i; n < j; n++) {
                    if (this.solver.valueOf(clausesContents[n]) == Tristate.UNDEF) {
                        localVarSet.set(MiniSatStyleSolver.var(clausesContents[n]));
                    }
                }
            }
            i = j + 1;
        }
    }

    @Override
    public int[] staticClauseIds() {
        return this.staticClauseIds;
    }

    @Override
    public void cacheKey(final BitSet key, final int numberOfVariables) {
        int i = 0;
        while (i < this.clauseContents.length) {
            int j = i;
            boolean subsumed = false;
            while (this.clauseContents[j] >= 0) {
                if (!subsumed && this.solver.valueOf(this.clauseContents[j]) == Tristate.TRUE) {
                    subsumed = true;
                }
                j++;
            }
            if (!subsumed) {
                key.set(-this.clauseContents[j] + 1 + numberOfVariables);
                for (int n = i; n < j; n++) {
                    if (this.solver.valueOf(this.clauseContents[n]) == Tristate.UNDEF) {
                        key.set(MiniSatStyleSolver.var(this.clauseContents[n]));
                    }
                }
            }
            i = j + 1;
        }
    }

    @Override
    public void countUnsubsumedOccurrences(final int[] occurrences) {
        for (final DTreeLeaf leaf : this.leafs) {
            leaf.countUnsubsumedOccurrences(occurrences);
        }
    }

    @Override
    public int depth() {
        return this.depth;
    }

    @Override
    public int widestSeparator() {
        return this.widestSeparator;
    }

    @Override
    public List<DTreeLeaf> leafs() {
        final List<DTreeLeaf> result = this.left.leafs();
        result.addAll(this.right.leafs());
        return result;
    }

    @Override
    public String toString() {
        return String.format("DTreeNode: [%s, %s]", this.left, this.right);
    }

    private void excludeUnitLeafs(final List<DTreeLeaf> leafs) {
        leafs.removeIf(dTreeLeaf -> dTreeLeaf.clauseSize() == 1);
    }

    public static int[] toArray(final BitSet bits) {
        final int[] result = new int[bits.cardinality()];
        int n = 0;
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            result[n++] = i;
        }
        return result;
    }

    public static int[] sortedIntersect(final int[] left, final int[] right) {
        final SortedSet<Integer> l = new TreeSet<>();
        final SortedSet<Integer> intersection = new TreeSet<>();
        for (final int i : left) {
            l.add(i);
        }
        for (final int i : right) {
            if (l.contains(i)) {
                intersection.add(i);
            }
        }
        final int[] result = new int[intersection.size()];
        int i = 0;
        for (final Integer elem : intersection) {
            result[i++] = elem;
        }
        return result;
    }
}
