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

package org.logicng.explanations.smus;

import static org.logicng.handlers.Handler.aborted;
import static org.logicng.handlers.Handler.start;
import static org.logicng.handlers.OptimizationHandler.satHandler;
import static org.logicng.solvers.maxsat.OptimizationConfig.OptimizationType.SAT_OPTIMIZATION;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.handlers.MaxSATHandler;
import org.logicng.handlers.OptimizationHandler;
import org.logicng.handlers.SATHandler;
import org.logicng.propositions.Proposition;
import org.logicng.propositions.StandardProposition;
import org.logicng.solvers.MaxSATSolver;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.SolverState;
import org.logicng.solvers.functions.OptimizationFunction;
import org.logicng.solvers.maxsat.OptimizationConfig;
import org.logicng.solvers.maxsat.algorithms.MaxSAT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Computation of a SMUS (smallest MUS, smallest minimal unsatisfiable set).
 * <p>
 * Implementation is based on &quot;Smallest MUS extraction with minimal
 * hitting set dualization&quot; (Ignatiev, Previti, Liffiton, &amp;
 * Marques-Silva, 2015).
 * @version 2.6.0
 * @since 2.0.0
 */
public final class SmusComputation {

    private static final String PROPOSITION_SELECTOR = "@PROPOSITION_SEL_";

    /**
     * Private empty constructor.  Class only contains static utility methods.
     */
    private SmusComputation() {
        // Intentionally left empty
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some additional constraint.
     * @param <P>                   the subtype of the propositions
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> List<P> computeSmus(
            final List<P> propositions, final List<Formula> additionalConstraints, final FormulaFactory f) {
        final OptimizationConfig cfg = new OptimizationConfig(SAT_OPTIMIZATION, null, null, null);
        return computeSmus(propositions, additionalConstraints, f, cfg);
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some additional constraint.
     * <p>
     * The SMUS computation can be called with an {@link OptimizationHandler}. The given handler instance will be
     * used for every subsequent * {@link org.logicng.solvers.functions.OptimizationFunction} call and the handler's
     * SAT handler is used for every subsequent SAT call.
     * @param <P>                   the subtype of the propositions
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @param handler               the handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> List<P> computeSmus(
            final List<P> propositions,
            final List<Formula> additionalConstraints,
            final FormulaFactory f,
            final OptimizationHandler handler) {
        final OptimizationConfig cfg = new OptimizationConfig(SAT_OPTIMIZATION, null, handler, null);
        return computeSmus(propositions, additionalConstraints, f, cfg);
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some additional constraint.
     * <p>
     * The SMUS computation can be called with an {@link OptimizationHandler}. The given handler instance will be used
     * for every subsequent {@link org.logicng.solvers.functions.OptimizationFunction} call and the handler's SAT
     * handler is used for every subsequent SAT call.
     * @param <P>                   the subtype of the propositions
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @param config                the optimization configuration
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> List<P> computeSmus(
            final List<P> propositions,
            final List<Formula> additionalConstraints,
            final FormulaFactory f,
            final OptimizationConfig config) {
        if (config.getOptimizationType() == SAT_OPTIMIZATION) {
            return computeSmusSAT(propositions, additionalConstraints, f, config.getOptimizationHandler());
        } else {
            return computeSmusMaxSAT(propositions, additionalConstraints, f, config);
        }
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional constraints.
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static List<Formula> computeSmusForFormulas(
            final List<Formula> formulas,
            final List<Formula> additionalConstraints,
            final FormulaFactory f) {
        final OptimizationConfig cfg = new OptimizationConfig(SAT_OPTIMIZATION, null, null, null);
        return computeSmusForFormulas(formulas, additionalConstraints, f, cfg);
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional constraints.
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @param handler               the SMUS handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static List<Formula> computeSmusForFormulas(
            final List<Formula> formulas,
            final List<Formula> additionalConstraints,
            final FormulaFactory f,
            final OptimizationHandler handler) {
        final OptimizationConfig cfg = new OptimizationConfig(SAT_OPTIMIZATION, null, handler, null);
        return computeSmusForFormulas(formulas, additionalConstraints, f, cfg);
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional constraints.
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @param f                     the formula factory
     * @param config                the optimization configuration
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static List<Formula> computeSmusForFormulas(
            final List<Formula> formulas,
            final List<Formula> additionalConstraints,
            final FormulaFactory f,
            final OptimizationConfig config) {
        final List<Proposition> props = formulas.stream().map(StandardProposition::new).collect(Collectors.toList());
        final List<Proposition> smus = computeSmus(props, additionalConstraints, f, config);
        return smus == null ? null : smus.stream().map(Proposition::formula).collect(Collectors.toList());
    }

    private static <P extends Proposition> List<P> computeSmusSAT(
            final List<P> propositions,
            final List<Formula> additionalConstraints,
            final FormulaFactory f,
            final OptimizationHandler handler) {
        start(handler);
        final SATSolver growSolver = MiniSat.miniSat(f);
        growSolver.add(additionalConstraints == null ? Collections.singletonList(f.verum()) : additionalConstraints);
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolver.add(f.equivalence(selector, proposition.formula()));
        }
        final boolean sat = growSolver.sat(satHandler(handler), propositionMapping.keySet()) == Tristate.TRUE;
        if (sat || aborted(handler)) {
            return null;
        }
        final SATSolver hSolver = MiniSat.miniSat(f);
        while (true) {
            final SortedSet<Variable> h = minimumHs(hSolver, propositionMapping.keySet(), handler);
            if (h == null || aborted(handler)) {
                return null;
            }
            final SortedSet<Variable> c = grow(growSolver, h, propositionMapping.keySet(), handler);
            if (aborted(handler)) {
                return null;
            }
            if (c == null) {
                return h.stream().map(propositionMapping::get).collect(Collectors.toList());
            }
            hSolver.add(f.or(c));
        }
    }

    private static <P extends Proposition> List<P> computeSmusMaxSAT(
            final List<P> propositions,
            final List<Formula> addConstraints,
            final FormulaFactory f,
            final OptimizationConfig config) {
        final MaxSATHandler handler = config.getMaxSATHandler();
        start(handler);
        final Collection<? extends Formula> additionalConstraints = addConstraints == null
                ? Collections.singletonList(f.verum())
                : addConstraints;
        final List<Formula> growSolverConstraints = new ArrayList<>(additionalConstraints);
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolverConstraints.add(f.equivalence(selector, proposition.formula()));
        }
        final SATSolver satSolver = MiniSat.miniSat(f);
        satSolver.add(growSolverConstraints);
        final SATHandler satHandler = handler == null ? null : handler.satHandler();
        final boolean sat = satSolver.sat(satHandler, propositionMapping.keySet()) == Tristate.TRUE;
        if (sat || aborted(satHandler)) {
            return null;
        }
        final List<Formula> hSolverConstraints = new ArrayList<>();
        while (true) {
            final SortedSet<Variable> h = minimumHs(hSolverConstraints, propositionMapping.keySet(), config, f);
            if (h == null || aborted(handler)) {
                return null;
            }
            final SortedSet<Variable> c = grow(growSolverConstraints, h, propositionMapping.keySet(), config, f);
            if (aborted(handler)) {
                return null;
            }
            if (c == null) {
                return h.stream().map(propositionMapping::get).collect(Collectors.toList());
            }
            hSolverConstraints.add(f.or(c));
        }
    }

    private static SortedSet<Variable> minimumHs(
            final SATSolver hSolver, final Set<Variable> variables, final OptimizationHandler handler) {
        final Assignment minimumHsModel = hSolver.execute(OptimizationFunction.builder()
                .handler(handler)
                .literals(variables)
                .minimize().build());
        return aborted(handler) ? null : new TreeSet<>(minimumHsModel.positiveVariables());
    }

    private static SortedSet<Variable> minimumHs(
            final List<Formula> constraints,
            final Set<Variable> variables,
            final OptimizationConfig config,
            final FormulaFactory f) {
        final MaxSATSolver maxSatSolver = config.genMaxSATSolver(f);
        constraints.forEach(maxSatSolver::addHardFormula);
        for (final Variable v : variables) {
            maxSatSolver.addSoftFormula(v.negate(), 1);
        }
        final MaxSAT.MaxSATResult result = maxSatSolver.solve(config.getMaxSATHandler());
        if (result == MaxSAT.MaxSATResult.UNDEF) {
            return null;
        }
        return new TreeSet<>(maxSatSolver.model().positiveVariables());
    }

    private static SortedSet<Variable> grow(
            final SATSolver growSolver,
            final SortedSet<Variable> h,
            final Set<Variable> variables,
            final OptimizationHandler handler) {
        final SolverState solverState = growSolver.saveState();
        growSolver.add(h);
        final Assignment maxModel = growSolver.execute(OptimizationFunction.builder()
                .handler(handler)
                .literals(variables)
                .maximize().build());
        if (maxModel == null || aborted(handler)) {
            return null;
        } else {
            final List<Variable> maximumSatisfiableSet = maxModel.positiveVariables();
            growSolver.loadState(solverState);
            final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
            maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
            return minimumCorrectionSet;
        }
    }

    private static SortedSet<Variable> grow(
            final List<Formula> constraints,
            final SortedSet<Variable> h,
            final Set<Variable> variables,
            final OptimizationConfig config,
            final FormulaFactory f) {
        final MaxSATSolver maxSatSolver = config.genMaxSATSolver(f);
        constraints.forEach(maxSatSolver::addHardFormula);
        h.forEach(maxSatSolver::addHardFormula);
        for (final Variable v : variables) {
            maxSatSolver.addSoftFormula(v, 1);
        }
        final MaxSAT.MaxSATResult result = maxSatSolver.solve(config.getMaxSATHandler());
        if (result == MaxSAT.MaxSATResult.UNDEF) {
            return null;
        }
        final Assignment maxModel = maxSatSolver.model();
        if (maxModel == null) {
            return null;
        } else {
            final List<Variable> maximumSatisfiableSet = maxModel.positiveVariables();
            final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
            maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
            return minimumCorrectionSet;
        }
    }
}
