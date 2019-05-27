package org.logicng.dnnf;

import org.logicng.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public final class DnnfFactory {

    private final Map<Formula, EnumSet<DnnfProperty>> knownFormulas; // Formula -> DnnfProperties
    private final Map<Formula, Formula> compiledDnnfs; // Formula -> Dnnf

    public DnnfFactory() {
        this.knownFormulas = new HashMap<>();
        this.compiledDnnfs = new HashMap<>();
    }

    public Formula compile(final Formula formula) {
        Formula dnnf = this.compiledDnnfs.get(formula);
        if (dnnf != null) {
            return dnnf;
        }
        final DnnfCompiler compiler = new DnnfCompiler(formula);
        dnnf = compiler.compile(new MinFillDTreeGenerator());
        this.compiledDnnfs.put(formula, dnnf);
        this.knownFormulas.put(dnnf, EnumSet.of(DnnfProperty.DECOMPOSABLE, DnnfProperty.DETERMINISTIC));
        return dnnf;
    }

    public int numberOfPhysicalNodes(final Formula formula) {
        return numberOfPhysicalNodesImpl(formula, new HashSet<>());
    }

    private int numberOfPhysicalNodesImpl(final Formula formula, final Set<Formula> seen) {
        if (seen.contains(formula)) {
            return 0;
        }
        int result = 1;
        for (final Formula op : formula) {
            result += numberOfPhysicalNodesImpl(op, seen);
        }
        seen.add(formula);
        return result;
    }

    //public BigInteger modelCount(final Formula formula, final int numberOfVariables) {
    //    final int formulaVars = formula.variables().size();
    //    if (numberOfVariables < formulaVars) {
    //        throw new IllegalArgumentException("Given number of variables is smaller than the actual number of variables.");
    //    }
    //    final EnumSet<DnnfProperty> properties = this.knownFormulas.get(formula);
    //    if (properties == null || !properties.contains(DnnfProperty.DETERMINISTIC) || !isDecomposable(formula)) {
    //        throw new IllegalArgumentException("Cannot verify that the given formula is a d-DNNF.");
    //    }
    //
    //    final BigInteger factor = BigInteger.valueOf(2).pow(numberOfVariables - formulaVars);
    //    return formula.apply(new DnnfModelCountFunction()).multiply(factor);
    //}

    public boolean isDecomposable(final Formula formula) {
        EnumSet<DnnfProperty> properties = this.knownFormulas.get(formula);
        if (properties == null) {
            properties = EnumSet.noneOf(DnnfProperty.class);
        }
        if (properties.contains(DnnfProperty.DECOMPOSABLE)) {
            return true;
        }
        final boolean result = decomposableImpl(formula, new HashSet<>());
        if (result) {
            properties.add(DnnfProperty.DECOMPOSABLE);
        }
        this.knownFormulas.put(formula, properties);
        return result;
    }

    private boolean decomposableImpl(final Formula formula, final Set<Formula> seen) {
        seen.add(formula);
        if (formula.type() == FType.AND) {
            final Iterator<Formula> it = formula.iterator();
            final SortedSet<Variable> currentVariables = it.next().variables();
            while (it.hasNext()) {
                for (final Variable variable : it.next().variables()) {
                    if (currentVariables.contains(variable)) {
                        return false;
                    }
                    currentVariables.add(variable);
                }
            }
        }
        for (final Formula op : formula) {
            if (!decomposableImpl(op, seen)) {
                return false;
            }
        }
        return true;
    }
}
