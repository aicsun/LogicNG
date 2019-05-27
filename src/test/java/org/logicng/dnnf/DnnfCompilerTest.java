package org.logicng.dnnf;

import org.junit.Assert;
import org.junit.Test;
import org.logicng.bdds.BDDFactory;
import org.logicng.bdds.datastructures.BDD;
import org.logicng.bdds.orderings.ForceOrdering;
import org.logicng.dnnf.datastructures.DNNF;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.FormulaParser;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.io.readers.DimacsReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.transformations.cnf.CNFFactorization;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DnnfCompilerTest {

    private final FormulaFactory f = new FormulaFactory();
    private final FormulaParser parser = new PseudoBooleanParser(this.f);

    @Test
    public void testTrivialFormulas() throws ParserException {
        testSmallFormula(this.parser.parse("$true"));
        testSmallFormula(this.parser.parse("$false"));
        testSmallFormula(this.parser.parse("a"));
        testSmallFormula(this.parser.parse("~a"));
        testSmallFormula(this.parser.parse("a & b"));
        testSmallFormula(this.parser.parse("a | b"));
        testSmallFormula(this.parser.parse("a => b"));
        testSmallFormula(this.parser.parse("a <=> b"));
        testSmallFormula(this.parser.parse("a | b | c"));
        testSmallFormula(this.parser.parse("a & b & c"));
        testSmallFormula(this.parser.parse("f & ((~b | c) <=> ~a & ~c)"));
        testSmallFormula(this.parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"));
        testSmallFormula(this.parser.parse("a + b + c + d <= 1"));
        testSmallFormula(this.parser.parse("a + b + c + d <= 3"));
        testSmallFormula(this.parser.parse("2*a + 3*b + -2*c + d < 5"));
        testSmallFormula(this.parser.parse("2*a + 3*b + -2*c + d >= 5"));
        testSmallFormula(this.parser.parse("~a & (~a | b | c | d)"));
    }

    @Test
    public void testLargeFormulas() throws IOException {
        final FormulaFactory f = new FormulaFactory();
        List<Formula> dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_1.cnf", f);
        testSmallFormula(f.cnf(dimacs));
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_2.cnf", f);
        testSmallFormula(f.cnf(dimacs));
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_3.cnf", f);
        testSmallFormula(f.cnf(dimacs));
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_4.cnf", f);
        testSmallFormula(f.cnf(dimacs));
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_5.cnf", f);
        testSmallFormula(f.cnf(dimacs));
    }

    private void testSmallFormula(final Formula formula) {
        final DnnfFactory dnnfFactory = new DnnfFactory();
        final DNNF dnnf = dnnfFactory.compile(formula);
        final BigDecimal dnnfCount = dnnf.modelCount();
        final Formula equivalence = this.f.equivalence(formula, dnnf.formula());
        Assert.assertTrue(equivalence.holds(new TautologyPredicate(this.f)));
        final BigDecimal bddCount = countWithBdd(formula);
        Assert.assertEquals(bddCount, dnnfCount);
    }

    private BigDecimal countWithBdd(final Formula formula) {
        if (formula.type() == FType.TRUE) {
            return BigDecimal.ONE;
        } else if (formula.type() == FType.FALSE) {
            return BigDecimal.ZERO;
        }
        final BDDFactory factory = new BDDFactory(100000, 1000000, formula.factory());

        final Formula cnf = formula.transform(new CNFFactorization());
        final List<Variable> order = new ArrayList<>(new ForceOrdering().getOrder(cnf));
        formula.variables().stream().filter(v -> !order.contains(v)).forEach(order::add);
        factory.setVariableOrder(order);
        final BDD bdd = factory.build(cnf);
        return bdd.modelCount();
    }
}
