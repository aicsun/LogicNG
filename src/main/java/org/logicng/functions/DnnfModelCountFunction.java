package org.logicng.functions;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;
import org.logicng.formulas.cache.FunctionCacheEntry;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public final class DnnfModelCountFunction implements FormulaFunction<BigInteger> {

    @Override
    public BigInteger apply(final Formula dnnf, final boolean cache) {
        final Object cached = dnnf.functionCacheEntry(FunctionCacheEntry.DNNF_MODELCOUNT);
        if (cached != null) {
            return (BigInteger) cached;
        }
        final BigInteger result = count(dnnf, new HashMap<>());
        if (cache) {
            dnnf.setFunctionCacheEntry(FunctionCacheEntry.DNNF_MODELCOUNT, result);
        }
        return result;
    }

    private BigInteger count(final Formula dnnf, final Map<Formula, BigInteger> internalCache) {
        BigInteger c = internalCache.get(dnnf);
        if (c == null) {
            switch (dnnf.type()) {
                case LITERAL:
                case TRUE:
                    c = BigInteger.ONE;
                    break;
                case AND:
                    c = BigInteger.ONE;
                    for (final Formula op : dnnf) {
                        c = c.multiply(count(op, internalCache));
                    }
                    break;
                case OR:
                    final int allVariables = dnnf.variables().size();
                    c = BigInteger.ZERO;
                    for (final Formula op : dnnf) {
                        final BigInteger opCount = count(op, internalCache);
                        final BigInteger factor = BigInteger.valueOf(2L).pow(allVariables - op.variables().size());
                        c = c.add(opCount.multiply(factor));
                    }
                    break;
                case FALSE:
                    c = BigInteger.ZERO;
                    break;
            }
            internalCache.put(dnnf, c);
        }
        return c;
    }
}
