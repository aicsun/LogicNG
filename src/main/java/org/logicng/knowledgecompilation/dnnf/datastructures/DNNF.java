package org.logicng.knowledgecompilation.dnnf.datastructures;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;
import org.logicng.formulas.cache.FunctionCacheEntry;
import org.logicng.knowledgecompilation.dnnf.DNNFProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class DNNF {

    private final SortedSet<Variable> originalVariables;
    private final Set<DNNFProperty> properties;
    private final Formula formula;

    public DNNF(final SortedSet<Variable> originalVariables, final Set<DNNFProperty> properties, final Formula dnnf) {
        this.originalVariables = originalVariables;
        this.properties = properties;
        this.formula = dnnf;
    }

    public Set<DNNFProperty> properties() {
        return this.properties;
    }

    public Formula formula() {
        return this.formula;
    }

    public int numberOfPhysicalNodes() {
        return numberOfPhysicalNodesImpl(this.formula, new HashSet<>());
    }

    public BigDecimal modelCount() {
        final Object cached = this.formula.functionCacheEntry(FunctionCacheEntry.DNNF_MODELCOUNT);
        final BigDecimal result;
        if (cached != null) {
            result = (BigDecimal) cached;
        } else {
            result = count(this.formula, new HashMap<>());
        }
        this.formula.setFunctionCacheEntry(FunctionCacheEntry.DNNF_MODELCOUNT, result);
        final SortedSet<Variable> dontCareVariables = new TreeSet<>();
        final SortedSet<Variable> dnnfVariables = this.formula.variables();
        for (final Variable originalVariable : this.originalVariables) {
            if (!dnnfVariables.contains(originalVariable)) {
                dontCareVariables.add(originalVariable);
            }
        }
        final BigDecimal factor = BigDecimal.valueOf(2).pow(dontCareVariables.size());
        return result.multiply(factor);
    }

    private BigDecimal count(final Formula dnnf, final Map<Formula, BigDecimal> internalCache) {
        BigDecimal c = internalCache.get(dnnf);
        if (c == null) {
            switch (dnnf.type()) {
                case LITERAL:
                case TRUE:
                    c = BigDecimal.ONE;
                    break;
                case AND:
                    c = BigDecimal.ONE;
                    for (final Formula op : dnnf) {
                        c = c.multiply(count(op, internalCache));
                    }
                    break;
                case OR:
                    final int allVariables = dnnf.variables().size();
                    c = BigDecimal.ZERO;
                    for (final Formula op : dnnf) {
                        final BigDecimal opCount = count(op, internalCache);
                        final BigDecimal factor = BigDecimal.valueOf(2L).pow(allVariables - op.variables().size());
                        c = c.add(opCount.multiply(factor));
                    }
                    break;
                case FALSE:
                    c = BigDecimal.ZERO;
                    break;
            }
            internalCache.put(dnnf, c);
        }
        return c;
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
}
