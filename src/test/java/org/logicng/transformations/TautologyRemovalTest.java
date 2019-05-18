package org.logicng.transformations;

import org.junit.Test;
import org.logicng.formulas.F;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TautologyRemoval}.
 * @version 2.0.0
 * @since 2.0.0
 */
public class TautologyRemovalTest {

    private final TautologyRemoval verumPreconditionRemoval01;
    private final TautologyRemoval verumPreconditionRemoval02;
    private final TautologyRemoval tautologyRemoval;
    private final TautologyRemoval falsumPreconditionRemoval;
    private final TautologyRemoval contradictionPreconditionRemoval;
    private final TautologyRemoval APreconditionRemoval;
    private final TautologyRemoval NAPreconditionRemoval;
    private final TautologyRemoval EQ1PreconditionRemoval;
    private final TautologyRemoval IMP1PreconditionRemoval;

    private final TautologyRemoval verumPreconditionRemovalWithSolver;
    private final TautologyRemoval tautologyRemovalWithSolver;
    private final TautologyRemoval falsumPreconditionRemovalWithSolver;
    private final TautologyRemoval contradictionPreconditionRemovalWithSolver;
    private final TautologyRemoval APreconditionRemovalWithSolver;
    private final TautologyRemoval NAPreconditionRemovalWithSolver;
    private final TautologyRemoval EQ1PreconditionRemovalWithSolver;
    private final TautologyRemoval IMP1PreconditionRemovalWithSolver;

    public TautologyRemovalTest() throws ParserException {
        this.verumPreconditionRemoval01 = new TautologyRemoval(F.f);
        this.verumPreconditionRemoval02 = new TautologyRemoval(F.f.verum());
        this.tautologyRemoval = new TautologyRemoval(F.f.parse("a | b | (~a & ~b)"));
        this.falsumPreconditionRemoval = new TautologyRemoval(F.f.falsum());
        this.contradictionPreconditionRemoval = new TautologyRemoval(F.f.parse("a & (a => b) & (b => ~a)"));
        this.APreconditionRemoval = new TautologyRemoval(F.A);
        this.NAPreconditionRemoval = new TautologyRemoval(F.NA);
        this.EQ1PreconditionRemoval = new TautologyRemoval(F.EQ1);
        this.IMP1PreconditionRemoval = new TautologyRemoval(F.IMP1);

        final SATSolver emptySolver = MiniSat.miniSat(F.f);
        this.verumPreconditionRemovalWithSolver = new TautologyRemoval(emptySolver);
        final SATSolver tautologySolver = MiniSat.miniSat(F.f);
        tautologySolver.add(F.f.parse("a | b | (~a & ~b)"));
        this.tautologyRemovalWithSolver = new TautologyRemoval(tautologySolver);
        final SATSolver falsumSolver = MiniSat.miniSat(F.f);
        falsumSolver.add(F.f.falsum());
        this.falsumPreconditionRemovalWithSolver = new TautologyRemoval(falsumSolver);
        final SATSolver contradictionSolver = MiniSat.miniSat(F.f);
        contradictionSolver.add(F.f.parse("a & (a => b) & (b => ~a)"));
        this.contradictionPreconditionRemovalWithSolver = new TautologyRemoval(contradictionSolver);
        final SATSolver aSolver = MiniSat.miniSat(F.f);
        aSolver.add(F.A);
        this.APreconditionRemovalWithSolver = new TautologyRemoval(aSolver);
        final SATSolver naSolver = MiniSat.miniSat(F.f);
        naSolver.add(F.NA);
        this.NAPreconditionRemovalWithSolver = new TautologyRemoval(naSolver);
        final SATSolver eq1Solver = MiniSat.miniSat(F.f);
        eq1Solver.add(F.EQ1);
        this.EQ1PreconditionRemovalWithSolver = new TautologyRemoval(eq1Solver);
        final SATSolver imp1Solver = MiniSat.miniSat(F.f);
        imp1Solver.add(F.IMP1);
        this.IMP1PreconditionRemovalWithSolver = new TautologyRemoval(imp1Solver);
    }

    @Test
    public void testFalsum() {
        assertThat(F.f.falsum().transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.tautologyRemoval)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.falsum().transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.falsum().transform(this.APreconditionRemoval)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.falsum());

        assertThat(F.f.falsum().transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.falsum().transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.falsum().transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(F.f.falsum().transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
    }

    @Test
    public void testVerum() {
        assertThat(F.f.verum().transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.tautologyRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.verum());

        assertThat(F.f.verum().transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.f.verum().transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
    }

    @Test
    public void testLiterals() {
        assertThat(F.A.transform(this.verumPreconditionRemoval01)).isEqualTo(F.A);
        assertThat(F.A.transform(this.verumPreconditionRemoval02)).isEqualTo(F.A);
        assertThat(F.A.transform(this.tautologyRemoval)).isEqualTo(F.A);
        assertThat(F.A.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.A);
        assertThat(F.A.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.A);

        assertThat(F.A.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.A);
        assertThat(F.A.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.A);
        assertThat(F.A.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.A.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.A);
        assertThat(F.A.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.A);

        assertThat(F.C.transform(this.verumPreconditionRemoval01)).isEqualTo(F.C);
        assertThat(F.C.transform(this.verumPreconditionRemoval02)).isEqualTo(F.C);
        assertThat(F.C.transform(this.tautologyRemoval)).isEqualTo(F.C);
        assertThat(F.C.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.C.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.C.transform(this.APreconditionRemoval)).isEqualTo(F.C);
        assertThat(F.C.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.C);
        assertThat(F.C.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.C);

        assertThat(F.C.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.C);
        assertThat(F.C.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.C);
        assertThat(F.C.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.C.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.C.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.C);
        assertThat(F.C.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.C);
        assertThat(F.C.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.C);

        assertThat(F.NA.transform(this.verumPreconditionRemoval01)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.verumPreconditionRemoval02)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.tautologyRemoval)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NA.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NA.transform(this.APreconditionRemoval)).isEqualTo(F.NA); // TODO q: should we identify tautology within ~A literal such that result is $false?
        assertThat(F.NA.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.NA);

        assertThat(F.NA.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NA.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NA.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.NA);
        assertThat(F.NA.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.NA);

        assertThat(F.NX.transform(this.verumPreconditionRemoval01)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.verumPreconditionRemoval02)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.tautologyRemoval)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NX.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NX.transform(this.APreconditionRemoval)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.NX);

        assertThat(F.NX.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NX.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NX.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.NX);
        assertThat(F.NX.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.NX);
    }

    @Test
    public void testNot() {
        assertThat(F.NOT1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.tautologyRemoval)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.APreconditionRemoval)).isEqualTo(F.NB);
        assertThat(F.NOT1.transform(this.NAPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.NOT1);

        assertThat(F.NOT1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.NB);
        assertThat(F.NOT1.transform(this.NAPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NOT1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.NOT1);
        assertThat(F.NOT1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.NOT1);

        assertThat(F.NOT3.transform(this.verumPreconditionRemoval01)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.verumPreconditionRemoval02)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.tautologyRemoval)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NOT3.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.NOT3.transform(this.APreconditionRemoval)).isEqualTo(F.f.falsum());
        assertThat(F.NOT3.transform(this.NAPreconditionRemoval)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.NOT3);

        assertThat(F.NOT3.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NOT3.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.NOT3.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(F.NOT3.transform(this.NAPreconditionRemovalWithSolver)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.NOT3);
        assertThat(F.NOT3.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.NOT3);
    }

    @Test
    public void testAnd() {
        assertThat(F.AND1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.tautologyRemoval)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.AND1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.AND1.transform(this.APreconditionRemoval)).isEqualTo(F.B);
        assertThat(F.AND1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.AND1);

        assertThat(F.AND1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.AND1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.AND1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.B);
        assertThat(F.AND1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.AND1);
        assertThat(F.AND1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.AND1);

        assertThat(F.AND2.transform(this.verumPreconditionRemoval01)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.verumPreconditionRemoval02)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.tautologyRemoval)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.AND2.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.AND2.transform(this.APreconditionRemoval)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.AND2);

        assertThat(F.AND2.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.AND2.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.AND2.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.AND2);
        assertThat(F.AND2.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.AND2);
    }

    @Test
    public void testOr() {
        assertThat(F.OR1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.tautologyRemoval)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.OR1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.OR1.transform(this.APreconditionRemoval)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.OR1);

        assertThat(F.OR1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.OR1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.OR1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.OR1);
        assertThat(F.OR1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.OR1);

        assertThat(F.OR4.transform(this.verumPreconditionRemoval01)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.verumPreconditionRemoval02)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.tautologyRemoval)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.OR4);

        assertThat(F.OR4.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.OR4.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.OR4);
        assertThat(F.OR4.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.OR4);
    }

    @Test
    public void testImplication() {
        assertThat(F.IMP1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.IMP1);
        assertThat(F.IMP1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.IMP1);
        assertThat(F.IMP1.transform(this.tautologyRemoval)).isEqualTo(F.IMP1);
        assertThat(F.IMP1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.APreconditionRemoval)).isEqualTo(F.B);
        assertThat(F.IMP1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.verum());

        assertThat(F.IMP1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.IMP1);
        assertThat(F.IMP1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.IMP1);
        assertThat(F.IMP1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.B);
        assertThat(F.IMP1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());

        assertThat(F.IMP2.transform(this.verumPreconditionRemoval01)).isEqualTo(F.IMP2);
        assertThat(F.IMP2.transform(this.verumPreconditionRemoval02)).isEqualTo(F.IMP2);
        assertThat(F.IMP2.transform(this.tautologyRemoval)).isEqualTo(F.IMP2);
        assertThat(F.IMP2.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.IMP2);

        assertThat(F.IMP2.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.IMP2);
        assertThat(F.IMP2.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.IMP2);
        assertThat(F.IMP2.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.IMP2.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.IMP2);
    }

    @Test
    public void testEquivalence() {
        assertThat(F.EQ1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.EQ1);
        assertThat(F.EQ1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.EQ1);
        assertThat(F.EQ1.transform(this.tautologyRemoval)).isEqualTo(F.EQ1);
        assertThat(F.EQ1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.APreconditionRemoval)).isEqualTo(F.B);
        assertThat(F.EQ1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.EQ1);

        assertThat(F.EQ1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.EQ1);
        assertThat(F.EQ1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.EQ1);
        assertThat(F.EQ1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.B);
        assertThat(F.EQ1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.EQ1);

        assertThat(F.EQ2.transform(this.verumPreconditionRemoval01)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.verumPreconditionRemoval02)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.tautologyRemoval)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.APreconditionRemoval)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.EQ2);

        assertThat(F.EQ2.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.EQ2);
        assertThat(F.EQ2.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.EQ2.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.EQ2);
    }

    @Test
    public void testPBC() {
        assertThat(F.PBC1.transform(this.verumPreconditionRemoval01)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.verumPreconditionRemoval02)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.tautologyRemoval)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.PBC1.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.PBC1.transform(this.APreconditionRemoval)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.PBC1);

        assertThat(F.PBC1.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.PBC1.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.PBC1.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.PBC1);
        assertThat(F.PBC1.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.PBC1);

        assertThat(F.PBC5.transform(this.verumPreconditionRemoval01)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.verumPreconditionRemoval02)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.tautologyRemoval)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.PBC5.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(F.PBC5.transform(this.APreconditionRemoval)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.PBC5);

        assertThat(F.PBC5.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.PBC5.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(F.PBC5.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.PBC5);
        assertThat(F.PBC5.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.PBC5);
    }

    @Test
    public void testMixedFormulas() throws ParserException {
        final Formula formula01 = F.f.parse("a & (b | (a => b)) & (a | c | d) & (~(a <=> b) => z)");
        assertThat(formula01.transform(this.verumPreconditionRemoval01)).isEqualTo(formula01);
        assertThat(formula01.transform(this.verumPreconditionRemoval02)).isEqualTo(formula01);
        assertThat(formula01.transform(this.tautologyRemoval)).isEqualTo(formula01);
        assertThat(formula01.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula01.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula01.transform(this.APreconditionRemoval)).isEqualTo(F.f.parse("b & (~b => z)"));
        assertThat(formula01.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.parse("a & (a | c | d)"));
        assertThat(formula01.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.parse("a & (a | c | d) & (~(a <=> b) => z)"));

        assertThat(formula01.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(formula01);
        assertThat(formula01.transform(this.tautologyRemovalWithSolver)).isEqualTo(formula01);
        assertThat(formula01.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula01.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula01.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.parse("b & (~b => z)"));
        assertThat(formula01.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.parse("a & (a | c | d)"));
        assertThat(formula01.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.parse("a & (a | c | d) & (~(a <=> b) => z)"));

        final Formula formula02 = F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b)) & (a | b | (~a & ~b))");
        assertThat(formula02.transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b))"));
        assertThat(formula02.transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b))"));
        assertThat(formula02.transform(this.tautologyRemoval)).isEqualTo(F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b))"));
        assertThat(formula02.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula02.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula02.transform(this.APreconditionRemoval)).isEqualTo(F.f.falsum());
        assertThat(formula02.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.falsum());
        assertThat(formula02.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.falsum());

        assertThat(formula02.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b))"));
        assertThat(formula02.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.parse("(a => ~(b | (a <=> b))) & a & b & ((a | c) => ~(a <=> b))"));
        assertThat(formula02.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula02.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula02.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(formula02.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());
        assertThat(formula02.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.falsum());

        final Formula formula03 = F.f.parse("(a => (b | (a <=> b))) & a & b & ((a | c) => (a <=> b))");
        assertThat(formula03.transform(this.verumPreconditionRemoval01)).isEqualTo(formula03);
        assertThat(formula03.transform(this.verumPreconditionRemoval02)).isEqualTo(formula03);
        assertThat(formula03.transform(this.tautologyRemoval)).isEqualTo(formula03);
        assertThat(formula03.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula03.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula03.transform(this.APreconditionRemoval)).isEqualTo(F.f.parse("b"));
        assertThat(formula03.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.parse("a & b"));
        assertThat(formula03.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.parse("a & b & ((a | c) => (a <=> b))"));

        assertThat(formula03.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(formula03);
        assertThat(formula03.transform(this.tautologyRemovalWithSolver)).isEqualTo(formula03);
        assertThat(formula03.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula03.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula03.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.parse("b"));
        assertThat(formula03.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.parse("a & b"));
        assertThat(formula03.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.parse("a & b & ((a | c) => (a <=> b))"));

        final Formula formula04 = F.f.parse("a | b & (a => b) | a & c & d | (~(a <=> b) => z)");
        assertThat(formula04.transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.tautologyRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.NAPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.verum());

        assertThat(formula04.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.NAPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula04.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());

        final Formula formula05 = F.f.parse("(a => ~(b & (a <=> b))) | a | b | (a & c => ~(a <=> b)) | a & b & (~a | ~b)");
        assertThat(formula05.transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.tautologyRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.NAPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.verum());

        assertThat(formula05.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.NAPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula05.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());

        final Formula formula06 = F.f.parse("(a => b & (a <=> b)) | a | b | (a & c => (a <=> b))");
        assertThat(formula06.transform(this.verumPreconditionRemoval01)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.verumPreconditionRemoval02)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.tautologyRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.falsumPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.contradictionPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.APreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.NAPreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.EQ1PreconditionRemoval)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.IMP1PreconditionRemoval)).isEqualTo(F.f.verum());

        assertThat(formula06.transform(this.verumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.tautologyRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.falsumPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.contradictionPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.APreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.NAPreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.EQ1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
        assertThat(formula06.transform(this.IMP1PreconditionRemovalWithSolver)).isEqualTo(F.f.verum());
    }
}
