///////////////////////////////////////////////////////////////////////////
//                   __                _      _   ________               //
//                  / /   ____  ____ _(_)____/ | / / ____/               //
//                 / /   / __ \/ __ `/ / ___/  |/ / / __                 //
//                / /___/ /_/ / /_/ / / /__/ /|  / /_/ /                 //
//               /_____/\____/\__, /_/\___/_/ |_/\____/                  //
//                           /____/                                      //
//                                                                       //
//               The Next Generation Logic Library                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////
//                                                                       //
//  Copyright 2015-20xx Christoph Zengler                                //
//                                                                       //
//  Licensed under the Apache License, Version 2.0 (the "License");      //
//  you may not use this file except in compliance with the License.     //
//  You may obtain a copy of the License at                              //
//                                                                       //
//  http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                       //
//  Unless required by applicable law or agreed to in writing, software  //
//  distributed under the License is distributed on an "AS IS" BASIS,    //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      //
//  implied.  See the License for the specific language governing        //
//  permissions and limitations under the License.                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////

package org.logicng.transformations.dnf;

import org.junit.Assert;
import org.junit.Test;
import org.logicng.bdds.BDDFactory;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.F;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.predicates.DNFPredicate;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.List;
import java.util.SortedSet;

/**
 * Unit Tests for {@link BDDDNFTransformation}.
 * @version 1.6.0
 * @since 1.6.0
 */
public class BDDDNFTest {

  private final BDDDNFTransformation bdddnf = new BDDDNFTransformation();
  private final DNFPredicate dnfPredicate = new DNFPredicate();

  @Test
  public void testConstants() {
    Assert.assertEquals(F.TRUE, F.TRUE.transform(this.bdddnf));
    Assert.assertEquals(F.FALSE, F.FALSE.transform(this.bdddnf));
  }

  @Test
  public void testLiterals() {
    Assert.assertEquals(F.A, F.A.transform(this.bdddnf));
    Assert.assertEquals(F.NA, F.NA.transform(this.bdddnf));
  }

  @Test
  public void testBinaryOperators() {
    Assert.assertTrue(F.IMP1.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.IMP1, F.IMP1.transform(this.bdddnf), F.IMP1.variables()));
    Assert.assertTrue(F.IMP2.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.IMP2, F.IMP2.transform(this.bdddnf), F.IMP2.variables()));
    Assert.assertTrue(F.IMP3.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.IMP3, F.IMP3.transform(this.bdddnf), F.IMP3.variables()));
    Assert.assertTrue(F.EQ1.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.EQ1, F.EQ1.transform(this.bdddnf), F.EQ1.variables()));
    Assert.assertTrue(F.EQ2.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.EQ2, F.EQ2.transform(this.bdddnf), F.EQ2.variables()));
    Assert.assertTrue(F.EQ3.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.EQ3, F.EQ3.transform(this.bdddnf), F.EQ3.variables()));
    Assert.assertTrue(F.EQ4.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(F.EQ4, F.EQ4.transform(this.bdddnf), F.EQ4.variables()));
  }

  @Test
  public void testNAryOperators() throws ParserException {
    final FormulaFactory f = new FormulaFactory();
    final PropositionalParser p = new PropositionalParser(f);
    Assert.assertEquals(F.AND1, F.AND1.transform(this.bdddnf));
    Assert.assertEquals(F.OR1, F.OR1.transform(this.bdddnf));
    final Formula f1 = p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
    final Formula f2 = p.parse("~(a & b) | c | ~(x | ~y)");
    final Formula f3 = p.parse("a | b | (~x & ~y)");
    Assert.assertTrue(f1.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f1, f1.transform(this.bdddnf), f1.variables()));
    Assert.assertTrue(f2.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f2, f2.transform(this.bdddnf), f2.variables()));
    Assert.assertTrue(f3.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f3, f3.transform(this.bdddnf), f3.variables()));
  }

  @Test
  public void testNAryOperatorsWithExternalFactory() throws ParserException {
    final FormulaFactory f = new FormulaFactory();
    final PropositionalParser p = new PropositionalParser(f);
    final BDDFactory bddFactory = new BDDFactory(1000, 1000, F.f);
    bddFactory.setNumberOfVars(7);
    final BDDDNFTransformation transformation = new BDDDNFTransformation(bddFactory);
    Assert.assertEquals(F.AND1, F.AND1.transform(this.bdddnf));
    Assert.assertEquals(F.OR1, F.OR1.transform(this.bdddnf));
    final Formula f1 = p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
    final Formula f2 = p.parse("~(a & b) | c | ~(x | ~y)");
    final Formula f3 = p.parse("a | b | (~x & ~y)");
    Assert.assertTrue(f1.transform(transformation).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f1, f1.transform(transformation), f1.variables()));
    Assert.assertTrue(f2.transform(transformation).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f2, f2.transform(transformation), f2.variables()));
    Assert.assertTrue(f3.transform(transformation).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f3, f3.transform(transformation), f3.variables()));
  }

  @Test
  public void testNot() throws ParserException {
    final PropositionalParser p = new PropositionalParser(F.f);
    Assert.assertEquals(p.parse("~a"), p.parse("~a").transform(this.bdddnf));
    Assert.assertEquals(p.parse("a"), p.parse("~~a").transform(this.bdddnf));
    Assert.assertEquals(p.parse("a & ~b"), p.parse("~(a => b)").transform(this.bdddnf));
    final Formula f1 = p.parse("~(~(a | b) => ~(x | y))");
    final Formula f2 = p.parse("~(a <=> b)");
    final Formula f3 = p.parse("~(~(a | b) <=> ~(x | y))");
    final Formula f4 = p.parse("~(a & b & ~x & ~y)");
    final Formula f5 = p.parse("~(a | b | ~x | ~y)");
    Assert.assertTrue(f1.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f1, f1.transform(this.bdddnf), f1.variables()));
    Assert.assertTrue(f2.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f2, f2.transform(this.bdddnf), f2.variables()));
    Assert.assertTrue(f3.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f3, f3.transform(this.bdddnf), f3.variables()));
    Assert.assertTrue(f4.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f4, f4.transform(this.bdddnf), f4.variables()));
    Assert.assertTrue(f5.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f5, f5.transform(this.bdddnf), f5.variables()));
    Assert.assertTrue(f5.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f5, f5.transform(this.bdddnf), f5.variables()));
  }

  @Test
  public void testCC() throws ParserException {
    final PseudoBooleanParser p = new PseudoBooleanParser(F.f);
    final Formula f1 = p.parse("a <=> (1 * b <= 1)");
    final Formula f2 = p.parse("~(1 * b <= 1)");
    final Formula f3 = p.parse("(1 * b + 1 * c + 1 * d <= 1)");
    final Formula f4 = p.parse("~(1 * b + 1 * c + 1 * d <= 1)");
    Assert.assertTrue(f1.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f1, f1.transform(this.bdddnf), f1.variables()));
    Assert.assertTrue(f2.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f2, f2.transform(this.bdddnf), f2.variables()));
    Assert.assertTrue(f3.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f3, f3.transform(this.bdddnf), f3.variables()));
    Assert.assertTrue(f4.transform(this.bdddnf).holds(this.dnfPredicate));
    Assert.assertTrue(equivalentModels(f4, f4.transform(this.bdddnf), f4.variables()));
  }

  private boolean equivalentModels(final Formula f1, final Formula f2, final SortedSet<Variable> vars) {
    final SATSolver s = MiniSat.miniSat(f1.factory());
    s.add(f1);
    final List<Assignment> models1 = s.enumerateAllModels(vars);
    s.reset();
    s.add(f2);
    final List<Assignment> models2 = s.enumerateAllModels(vars);
    if (models1.size() != models2.size())
      return false;
    for (final Assignment model : models1)
      if (!models2.contains(model))
        return false;
    return true;
  }
}
