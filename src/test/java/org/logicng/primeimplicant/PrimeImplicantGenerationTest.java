package org.logicng.primeimplicant;

import org.junit.Ignore;
import org.junit.Test;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.readers.DimacsReader;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.SATPredicate;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.solvers.MiniSat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Unit tests for {@link PrimeImplicantGeneration}.
 * @version 1.6.0
 * @since 1.6.0
 */
public class PrimeImplicantGenerationTest {

    private static final FormulaFactory f = new FormulaFactory();
    private static final PropositionalParser p = new PropositionalParser(f);

    private void testImplicantProperty(final SortedSet<Literal> implicant, final Formula formula) {
        assertThat(f.and(implicant).holds(new SATPredicate(f))).isTrue();
        assertThat(f.implication(f.and(implicant), formula).holds(new TautologyPredicate(f))).isTrue();
    }

    private void testMinimalityProperty(final SortedSet<Literal> primeImplicant, final Formula formula) {
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formula.negate());
        for (final Literal lit : primeImplicant) {
            final SortedSet<Literal> assumptions = new TreeSet<>(primeImplicant);
            assumptions.remove(lit);
            assumptions.add(lit.negate());
            assertThat(solver.sat(assumptions)).isEqualTo(Tristate.TRUE);
        }
    }

    private void testAnyPrimeImplicant(final Formula formula) {
        final SortedSet<Literal> primeImplicant = PrimeImplicantGeneration.compute(formula);
        if (formula.holds(new SATPredicate(f))) {
            assertThat(primeImplicant).isNotNull();
            testImplicantProperty(primeImplicant, formula);
            testMinimalityProperty(primeImplicant, formula);
        } else {
            assertThat(primeImplicant).isNull();
        }

    }

    private Formula getBlockingClause(final SortedSet<Literal> literals) {
        final SortedSet<Literal> negatedLiterals = new TreeSet<>();
        for (final Literal lit : literals) {
            negatedLiterals.add(lit.negate());
        }
        return f.or(negatedLiterals);
    }

    private void testPrimeImplicantFrom(final Formula formula, final int numPrimeImplicants) {
        final MiniSat satSolver = MiniSat.miniSat(formula.factory());
        satSolver.add(formula);
        boolean runOutOfModels = false;
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        for (int i = 0; i < numPrimeImplicants; ++i) {
            final boolean sat = satSolver.sat() == Tristate.TRUE;
            if (sat) {
                final SortedSet<Literal> implicant = satSolver.model(formula.variables()).literals();
                final SortedSet<Literal> primeImplicant = PrimeImplicantGeneration.computeFrom(formula, implicant);
                primeImplicants.add(primeImplicant);
                testImplicantProperty(primeImplicant, formula);
                testMinimalityProperty(primeImplicant, formula);
                satSolver.add(getBlockingClause(primeImplicant));
            } else {
                runOutOfModels = true;
                break;
            }
        }
        if (runOutOfModels) {
            Formula cover = f.falsum();
            for (final SortedSet<Literal> primeImplicant : primeImplicants) {
                cover = f.or(cover, f.and(primeImplicant));
            }
            assertThat(f.equivalence(cover, formula).holds(new TautologyPredicate(f))).isTrue();
        }
    }

    private void test(final Formula formula, final int numPrimeImplicants) {
        testAnyPrimeImplicant(formula);
        testPrimeImplicantFrom(formula, numPrimeImplicants);
    }

    @Test
    public void testSimple() throws ParserException {
        test(p.parse("a & b & c"), 5);
        test(p.parse("a | b | c"), 5);
        test(p.parse("a & (b | c)"), 5);
        test(p.parse("a & (b | c) & c & (D | E) & D"), 5);
    }

    @Test
    public void testSmallFormulas() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/small_formulas.txt", f);
        test(formula, 5);
    }

    @Test
    public void testLargeFormula() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        test(formula, 5);
    }

    @Ignore
    @Test
    public void testDimacsFiles() throws IOException {
        final File testFolder = new File("src/test/resources/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final File file : files) {
            final String fileName = file.getName();
            if (fileName.endsWith(".cnf")) {
                test(f.cnf(DimacsReader.readCNF(file, f)), 2);
            }
        }
    }
}