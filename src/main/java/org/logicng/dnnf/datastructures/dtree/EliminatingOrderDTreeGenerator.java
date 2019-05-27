package org.logicng.dnnf.datastructures.dtree;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.predicates.CNFPredicate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class EliminatingOrderDTreeGenerator implements DTreeGenerator {

    /**
     * Generates a DTree from an arbitrary eliminating order of variables as described in
     * A. Darwiche "Decomposable Negation Normal Form" (algorithm "el2dt")
     */
    public final DTree generateWithEliminatingOrder(final Formula cnf, final List<Literal> ordering) {
        assert cnf.variables().size() == ordering.size();

        if (!cnf.holds(new CNFPredicate()) || cnf.isAtomicFormula()) {
            throw new IllegalArgumentException("Cannot generate DTree from a non-cnf formula or atomic formula");
        } else if (cnf.type() != FType.AND)  // cnf.depth == 1
        {
            return new DTreeLeaf(0, cnf);
        }

        final LinkedList<DTree> sigma = new LinkedList<>();
        int id = 0;
        for (final Formula clause : cnf) {
            sigma.add(new DTreeLeaf(id++, clause));
        }

        for (final Literal variable : ordering) {
            final LinkedList<DTree> gamma = new LinkedList<>();
            final Iterator<DTree> sigmaIterator = sigma.iterator();
            while (sigmaIterator.hasNext()) {
                final DTree tree = sigmaIterator.next();
                if (tree.staticLiteralSet().contains(variable)) {
                    gamma.add(tree);
                    sigmaIterator.remove();
                }
            }
            if (!gamma.isEmpty()) {
                sigma.add(compose(gamma));
            }
        }

        return compose(sigma);
    }

    private DTree compose(final List<DTree> trees) {
        assert !trees.isEmpty();

        if (trees.size() == 1) {
            return trees.get(0);
        } else {
            final DTree left = compose(trees.subList(0, trees.size() / 2));
            final DTree right = compose(trees.subList(trees.size() / 2, trees.size()));
            return new DTreeNode(left, right);
        }
    }
}
