package org.logicng.dnnf;

import org.logicng.dnnf.datastructures.DNNF;
import org.logicng.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;
import org.logicng.transformations.BackboneSimplifier;
import org.logicng.transformations.cnf.CNFSubsumption;

import java.util.EnumSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class DnnfFactory {

    private final BackboneSimplifier backboneSimplifier;
    private final CNFSubsumption subsumption;

    public DnnfFactory() {
        this.backboneSimplifier = new BackboneSimplifier();
        this.subsumption = new CNFSubsumption();
    }

    public DNNF compile(final Formula formula) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables());
        final Formula cnf = formula.cnf();
        originalVariables.addAll(cnf.variables());
        final Formula simplifedFormula = simplifyFormula(cnf);
        final DnnfCompiler compiler = new DnnfCompiler(simplifedFormula);
        final Formula dnnf = compiler.compile(new MinFillDTreeGenerator());
        final Set<DnnfProperty> properties = EnumSet.of(DnnfProperty.DECOMPOSABLE, DnnfProperty.DETERMINISTIC);
        return new DNNF(originalVariables, properties, dnnf);
    }

    private Formula simplifyFormula(final Formula formula) {
        return formula.transform(this.backboneSimplifier).transform(this.subsumption);
    }

    //public boolean isDecomposable(final Formula formula) {
    //    EnumSet<DnnfProperty> properties = this.knownFormulas.get(formula);
    //    if (properties == null) {
    //        properties = EnumSet.noneOf(DnnfProperty.class);
    //    }
    //    if (properties.contains(DnnfProperty.DECOMPOSABLE)) {
    //        return true;
    //    }
    //    final boolean result = decomposableImpl(formula, new HashSet<>());
    //    if (result) {
    //        properties.add(DnnfProperty.DECOMPOSABLE);
    //    }
    //    this.knownFormulas.put(formula, properties);
    //    return result;
    //}
    //
    //private boolean decomposableImpl(final Formula formula, final Set<Formula> seen) {
    //    seen.add(formula);
    //    if (formula.type() == FType.AND) {
    //        final Iterator<Formula> it = formula.iterator();
    //        final SortedSet<Variable> currentVariables = it.next().variables();
    //        while (it.hasNext()) {
    //            for (final Variable variable : it.next().variables()) {
    //                if (currentVariables.contains(variable)) {
    //                    return false;
    //                }
    //                currentVariables.add(variable);
    //            }
    //        }
    //    }
    //    for (final Formula op : formula) {
    //        if (!decomposableImpl(op, seen)) {
    //            return false;
    //        }
    //    }
    //    return true;
    //}
}
