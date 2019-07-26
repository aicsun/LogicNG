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

package org.logicng.backbones;

import org.logicng.collections.LNGIntVector;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;
import org.logicng.solvers.datastructures.MSClause;
import org.logicng.solvers.datastructures.MSVariable;
import org.logicng.solvers.datastructures.MSWatcher;
import org.logicng.solvers.sat.MiniSat2Solver;
import org.logicng.transformations.cnf.PlaistedGreenbaumTransformationSolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * An extension of MiniSat to compute the backbone of a formula.
 * <p>
 * The algorithm iteratively checks each variable of an initial model (candidates) whether the variable
 * is a backbone variable. For each check the SAT solving procedure is called. Thus, at the number of SAT calls is at
 * most the number of variables.
 * <p>
 * Reference: Algorithm 3 in M. Janota, I. Lynce, J. Marques-Silva, Algorithms for Computing Backbones of Propositional
 * Formulae, AI Communications, Volume 28(2), 161-177, 2015.
 * @version 1.5.1
 * @since 1.5.0
 */
public class MiniSatBackbone extends MiniSat2Solver {

  private BackboneConfig config;
  private final PlaistedGreenbaumTransformationSolver pgTransformation = new PlaistedGreenbaumTransformationSolver(this, true);

  /**
   * Type of the backbone computation.
   */
  private BackboneType type;

  /**
   * Candidates to test for backbone.
   * <p>
   * The integers are solver internal literal integers.
   */
  private Stack<Integer> candidates;

  /**
   * Assumptions used to call the solver with, filled by identified backbone literals.
   */
  private LNGIntVector assumptions;

  /**
   * Backbone map: integer literal -> Tristate.
   * <ul>
   * <li>{@link Tristate#TRUE} is a positive backbone variable
   * <li>{@link Tristate#FALSE} is a negative backbone variable
   * <li>{@link Tristate#UNDEF} is an optional variable
   * </ul>
   */
  private HashMap<Integer, Tristate> backboneMap;

  /**
   * Creates a new backbone solver using the given configuration.
   * @param config configuration
   */
  public MiniSatBackbone(final BackboneConfig config) {
    this.config = config;
  }

  /**
   * Creates a new backbone solver using the default configuration.
   */
  public MiniSatBackbone() {
    this(new BackboneConfig.Builder().build());
  }

  /**
   * Sets a new backbone configuration.
   * @param newConfig the new {@link BackboneConfig}
   */
  public void setConfig(final BackboneConfig newConfig) {
    this.config = newConfig;
  }

  /**
   * Adds an arbitrary formula to the solver.
   * @param formula the formula
   */
  public void add(final Formula formula) {
    this.pgTransformation.addCNFtoSolver(formula, null);
  }

  /**
   * Adds the given arbitrary formulas to the solver.
   * @param formulas the formulas
   */
  public void add(final Collection<Formula> formulas) {
    for (final Formula formula : formulas) {
      add(formula);
    }
  }

  /**
   * Returns a list of relevant variable indices. A relevant variable is known by the solver.
   * @param variables variables to convert and filter
   * @return list of relevant variable indices
   */
  private List<Integer> getRelevantVarIndices(final Collection<Variable> variables) {
    final List<Integer> relevantVarIndices = new ArrayList<Integer>(variables.size());
    for (final Variable var : variables) {
      final Integer idx = this.name2idx.get(var.name());
      // Note: Unknown variables are variables added to the solver yet. Thus, these are optional variables and can
      // be left out for the backbone computation.
      if (idx != null) {
        relevantVarIndices.add(idx);
      }
    }
    return relevantVarIndices;
  }

  /**
   * Initializes the internal solver state.
   * @param type      backbone type
   * @param variables to test
   */
  private void init(final List<Integer> variables, final BackboneType type) {
    this.type = type;
    this.candidates = new Stack<Integer>();
    this.assumptions = new LNGIntVector(variables.size());
    this.backboneMap = new HashMap<Integer, Tristate>();
    for (final Integer var : variables) {
      this.backboneMap.put(var, Tristate.UNDEF);
    }
  }

  /**
   * Tests if positive backbone literals should be computed.
   * @return {@code true} if positive backbone literals should be computed, otherwise {@code false}
   */
  private boolean isBothOrPositiveType() {
    return this.type == BackboneType.POSITIVE_AND_NEGATIVE || this.type == BackboneType.ONLY_POSITIVE;
  }

  /**
   * Tests if negative backbone literals should be computed.
   * @return {@code true} if negative backbone literals should be computed, otherwise {@code false}
   */
  private boolean isBothOrNegativeType() {
    return this.type == BackboneType.POSITIVE_AND_NEGATIVE || this.type == BackboneType.ONLY_NEGATIVE;
  }

  /**
   * Tests if positive and negative backbone literals should be computed.
   * @return {@code true} if positive and negative backbone literals should be computed, otherwise {@code false}
   */
  private boolean isBothType() {
    return this.type == BackboneType.POSITIVE_AND_NEGATIVE;
  }

  /**
   * Builds the backbone object from the computed backbone literals.
   * @param variables relevant variables
   * @return backbone
   */
  private Backbone buildBackbone(final Collection<Variable> variables) {
    final SortedSet<Variable> posBackboneVars = isBothOrPositiveType() ? new TreeSet<Variable>() : null;
    final SortedSet<Variable> negBackboneVars = isBothOrNegativeType() ? new TreeSet<Variable>() : null;
    final SortedSet<Variable> optionalVars = isBothType() ? new TreeSet<Variable>() : null;
    for (final Variable var : variables) {
      final Integer idx = this.name2idx.get(var.name());
      if (idx == null) {
        if (isBothType()) {
          optionalVars.add(var);
        }
      } else {
        switch (this.backboneMap.get(idx)) {
          case TRUE:
            if (isBothOrPositiveType()) {
              posBackboneVars.add(var);
            }
            break;
          case FALSE:
            if (isBothOrNegativeType()) {
              negBackboneVars.add(var);
            }
            break;
          case UNDEF:
            if (isBothType()) {
              optionalVars.add(var);
            }
            break;
          default:
            throw new IllegalStateException("Unknown tristate: " + this.backboneMap.get(idx));
        }
      }
    }
    return Backbone.satBackbone(posBackboneVars, negBackboneVars, optionalVars);
  }

  /**
   * Computes the backbone of the given variables with respect to the formulas added to the solver.
   * @param variables variables to test
   * @param type      backbone type
   * @return the backbone projected to the relevant variables or {@code null} if the formula on the solver with the restrictions are not satisfiable
   */
  public Backbone compute(final Collection<Variable> variables, final BackboneType type) {
    final boolean sat = solve(null) == Tristate.TRUE;
    if (sat) {
      final List<Integer> relevantVarIndices = getRelevantVarIndices(variables);
      init(relevantVarIndices, type);
      compute(relevantVarIndices);
      return buildBackbone(variables);
    } else {
      return Backbone.unsatBackbone();
    }
  }

  /**
   * Tests the given variable whether it is a unit propagated literal on level 0.
   * <p>
   * Assumption: The formula on the solver has successfully been tested to be satisfiable before.
   * @param var variable index to test
   * @return {@code true} if the variable is a unit propagated literal on level 0, otherwise {@code false}
   */
  private boolean isUPZeroLit(final int var) {
    return this.vars.get(var).level() == 0;
  }

  /**
   * Tests the given literal whether it is unit in the given clause.
   * @param lit    literal to test
   * @param clause clause containing the literal
   * @return {@code true} if the literal is unit, {@code false} otherwise
   */
  private boolean isUnit(final int lit, final MSClause clause) {
    for (int i = 0; i < clause.size(); ++i) {
      final int clauseLit = clause.get(i);
      if (lit != clauseLit && this.model.get(var(clauseLit)) != sign(clauseLit)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests the given literal whether it is rotatable in the current model.
   * @param lit literal to test
   * @return {@code true} if the literal is rotatable, otherwise {@code false}
   */
  private boolean isRotatable(final int lit) {
    // A rotatable literal MUST NOT be a unit propagated literal
    if (v(lit).reason() != null) {
      return false;
    }
    // A rotatable literal MUST NOT be unit
    for (final MSWatcher watcher : this.watches.get(not(lit))) {
      if (isUnit(lit, watcher.clause())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds the given literal to the backbone result and optionally adds the literal to the solver.
   * @param lit literal to add
   */
  private void addBackboneLiteral(final int lit) {
    this.backboneMap.put(var(lit), sign(lit) ? Tristate.FALSE : Tristate.TRUE);
    this.assumptions.push(lit);
  }

  /**
   * Creates the initial candidate literals for the backbone computation.
   * @param variables variables to test
   * @return initial candidates
   */
  private Stack<Integer> createInitialCandidates(final List<Integer> variables) {
    for (final Integer var : variables) {
      if (isUPZeroLit(var)) {
        final int backboneLit = mkLit(var, !this.model.get(var));
        addBackboneLiteral(backboneLit);
      } else {
        final boolean modelPhase = this.model.get(var);
        if (isBothOrNegativeType() && !modelPhase || isBothOrPositiveType() && modelPhase) {
          final int lit = mkLit(var, !modelPhase);
          if (!this.config.isInitialUBCheckForRotatableLiterals() || !isRotatable(lit)) {
            this.candidates.add(lit);
          }
        }
      }
    }
    return this.candidates;
  }

  /**
   * Refines the upper bound by optional checks (UP zero literal, complement model literal, rotatable literal).
   */
  private void refineUpperBound() {
    for (final Integer lit : new ArrayList<Integer>(this.candidates)) {
      final int var = var(lit);
      if (isUPZeroLit(var)) {
        this.candidates.remove(lit);
        addBackboneLiteral(lit);
      } else if (this.config.isCheckForComplementModelLiterals() && this.model.get(var) == sign(lit)) {
        this.candidates.remove(lit);
      } else if (this.config.isCheckForRotatableLiterals() && isRotatable(lit)) {
        this.candidates.remove(lit);
      }
    }
  }

  /**
   * Tests the given literal with the formula on the solver for satisfiability.
   * @param lit literal to test
   * @return {@code true} if satisfiable, otherwise {@code false}
   */
  private boolean solve(final int lit) {
    this.assumptions.push(not(lit));
    final boolean sat = solve(null, this.assumptions) == Tristate.TRUE;
    this.assumptions.pop();
    return sat;
  }

  @Override
  protected void cancelUntil(final int level) {
    if (decisionLevel() > level) {
      for (int c = this.trail.size() - 1; c >= this.trailLim.get(level); c--) {
        final int x = var(this.trail.get(c));
        final MSVariable v = this.vars.get(x);
        v.assign(Tristate.UNDEF);
        v.setPolarity(false);
        insertVarOrder(x);
      }
      this.qhead = this.trailLim.get(level);
      this.trail.removeElements(this.trail.size() - this.trailLim.get(level));
      this.trailLim.removeElements(this.trailLim.size() - level);
    }
  }

  /**
   * Computes the backbone for the given variables.
   * @param variables variables to test
   */
  private void compute(final List<Integer> variables) {
    final Stack<Integer> candidates = createInitialCandidates(variables);
    while (candidates.size() > 0) {
      final int lit = candidates.pop();
      if (solve(lit)) {
        refineUpperBound();
      } else {
        addBackboneLiteral(lit);
      }
    }
  }

  @Override
  public void reset() {
    super.reset();
    this.pgTransformation.clearCache();
  }

  @Override
  public void loadState(final int[] state) {
    super.loadState(state);
    this.pgTransformation.clearCache();
  }
}
