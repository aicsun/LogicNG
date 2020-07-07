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

/*
 * MiniCARD Copyright (c) 2012, Mark Liffiton, Jordyn Maglalang
 * <p>
 * MiniCARD is based on MiniSAT, whose original copyright notice is maintained below,
 * and it is released under the same license.
 * ---
 * <p>
 * MiniSat -- Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.logicng.solvers.sat;

import org.logicng.collections.LNGBooleanVector;
import org.logicng.collections.LNGIntVector;
import org.logicng.collections.LNGVector;
import org.logicng.datastructures.Tristate;
import org.logicng.handlers.SATHandler;
import org.logicng.propositions.Proposition;
import org.logicng.solvers.datastructures.MSClause;
import org.logicng.solvers.datastructures.MSVariable;
import org.logicng.solvers.datastructures.MSWatcher;

/**
 * A cardinality solver based on MiniCard.
 * @version 1.3
 * @since 1.0
 */
public final class MiniCard extends MiniSatStyleSolver {

    private static final int LIT_ERROR = -2;

    private LNGIntVector unitClauses;

    /**
     * Constructs a new MiniSAT 2 solver with the default values for solver configuration.  By default, incremental mode
     * is activated.
     */
    public MiniCard() {
        this(MiniSatConfig.builder().build());
    }

    /**
     * Constructs a new MiniSAT 2 solver with a given solver configuration.
     * @param config the solver configuration
     */
    public MiniCard(final MiniSatConfig config) {
        super(config);
        this.initializeMiniSAT();
    }

    /**
     * Initializes the additional parameters.
     */
    private void initializeMiniSAT() {
        this.unitClauses = new LNGIntVector();
        this.learntsizeAdjustConfl = 0;
        this.learntsizeAdjustCnt = 0;
        this.learntsizeAdjustStartConfl = 100;
        this.learntsizeAdjustInc = 1.5;
        this.maxLearnts = 0;
    }

    @Override
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = this.vars.size();
        final MSVariable newVar = new MSVariable(sign);
        this.vars.push(newVar);
        this.watches.push(new LNGVector<>());
        this.watches.push(new LNGVector<>());
        this.seen.push(false);
        newVar.setDecision(dvar);
        insertVarOrder(v);
        return v;
    }

    @Override
    public boolean addClause(final LNGIntVector ps, final Proposition proposition) {
        assert decisionLevel() == 0;
        if (!this.ok) {
            return false;
        }
        ps.sort();
        int p;
        int i;
        int j;
        for (i = 0, j = 0, p = LIT_UNDEF; i < ps.size(); i++) {
            if (value(ps.get(i)) == Tristate.TRUE || ps.get(i) == not(p)) {
                return true;
            } else if (value(ps.get(i)) != Tristate.FALSE && ps.get(i) != p) {
                p = ps.get(i);
                ps.set(j++, p);
            }
        }
        ps.removeElements(i - j);
        if (ps.empty()) {
            this.ok = false;
            return false;
        } else if (ps.size() == 1) {
            uncheckedEnqueue(ps.get(0), null);
            this.ok = propagate() == null;
            if (this.incremental) {
                this.unitClauses.push(ps.get(0));
            }
            return this.ok;
        } else {
            final MSClause c = new MSClause(ps, false);
            this.clauses.push(c);
            attachClause(c);
        }
        return true;
    }

    @Override
    public Tristate solve(final SATHandler handler) {
        this.handler = handler;
        if (this.handler != null) {
            this.handler.started();
        }
        this.model.clear();
        this.conflict.clear();
        if (!this.ok) {
            return Tristate.FALSE;
        }
        this.learntsizeAdjustConfl = this.learntsizeAdjustStartConfl;
        this.learntsizeAdjustCnt = (int) this.learntsizeAdjustConfl;
        this.maxLearnts = this.clauses.size() * this.learntsizeFactor;
        Tristate status = Tristate.UNDEF;
        int currRestarts = 0;
        while (status == Tristate.UNDEF && !this.canceledByHandler) {
            final double restBase = luby(this.restartInc, currRestarts);
            status = search((int) (restBase * this.restartFirst));
            currRestarts++;
        }
        if (status == Tristate.TRUE) {
            this.model = new LNGBooleanVector(this.vars.size());
            for (final MSVariable v : this.vars) {
                this.model.push(v.assignment() == Tristate.TRUE);
            }
        } else if (status == Tristate.FALSE && this.conflict.empty()) {
            this.ok = false;
        }
        if (this.handler != null) {
            this.handler.finishedSolving();
        }
        cancelUntil(0);
        this.handler = null;
        this.canceledByHandler = false;
        return status;
    }

    @Override
    public void reset() {
        super.initialize();
        this.initializeMiniSAT();
    }

    /**
     * Saves and returns the solver state expressed as an integer array which stores the length of the internal data
     * structures.  The array has length 5 and has the following layout:
     * <p>
     * {@code | current solver state | #vars | #clauses | #learnt clauses | #unit clauses |}
     * @return the current solver state
     */
    @Override
    public int[] saveState() {
        if (!this.incremental) {
            throw new IllegalStateException("Cannot save a state when the incremental mode is deactivated");
        }
        final int[] state;
        state = new int[5];
        state[0] = this.ok ? 1 : 0;
        state[1] = this.vars.size();
        state[2] = this.clauses.size();
        state[3] = this.learnts.size();
        state[4] = this.unitClauses.size();
        return state;
    }

    @Override
    public void loadState(final int[] state) {
        if (!this.incremental) {
            throw new IllegalStateException("Cannot save a state when the incremental mode is deactivated");
        }
        int i;
        completeBacktrack();
        this.ok = state[0] == 1;
        final int newVarsSize = Math.min(state[1], this.vars.size());
        for (i = this.vars.size() - 1; i >= newVarsSize; i--) {
            this.orderHeap.remove(this.name2idx.remove(this.idx2name.remove(i)));
        }
        this.vars.shrinkTo(newVarsSize);
        final int newClausesSize = Math.min(state[2], this.clauses.size());
        for (i = this.clauses.size() - 1; i >= newClausesSize; i--) {
            simpleRemoveClause(this.clauses.get(i));
        }
        this.clauses.shrinkTo(newClausesSize);
        final int newLearntsSize = Math.min(state[3], this.learnts.size());
        for (i = this.learnts.size() - 1; i >= newLearntsSize; i--) {
            simpleRemoveClause(this.learnts.get(i));
        }
        this.learnts.shrinkTo(newLearntsSize);
        this.watches.shrinkTo(newVarsSize * 2);
        this.unitClauses.shrinkTo(state[4]);
        for (i = 0; this.ok && i < this.unitClauses.size(); i++) {
            uncheckedEnqueue(this.unitClauses.get(i), null);
            this.ok = propagate() == null;
        }
    }

    @Override
    protected void uncheckedEnqueue(final int lit, final MSClause reason) {
        assert value(lit) == Tristate.UNDEF;
        final MSVariable var = v(lit);
        var.assign(Tristate.fromBool(!sign(lit)));
        var.setReason(reason);
        var.setLevel(decisionLevel());
        this.trail.push(lit);
    }

    @Override
    protected void attachClause(final MSClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                final int l = c.get(i);
                this.watches.get(l).push(new MSWatcher(c, LIT_UNDEF));
            }
            this.clausesLiterals += c.size();
        } else {
            assert c.size() > 1;
            this.watches.get(not(c.get(0))).push(new MSWatcher(c, c.get(1)));
            this.watches.get(not(c.get(1))).push(new MSWatcher(c, c.get(0)));
            if (c.learnt()) {
                this.learntsLiterals += c.size();
            } else {
                this.clausesLiterals += c.size();
            }
        }
    }

    @Override
    protected void detachClause(final MSClause c) {
        assert !c.isAtMost();
        assert c.size() > 1;
        this.watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
        this.watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        if (c.learnt()) {
            this.learntsLiterals -= c.size();
        } else {
            this.clausesLiterals -= c.size();
        }
    }

    @Override
    protected void removeClause(final MSClause c) {
        if (c.isAtMost()) {
            detachAtMost(c);
            for (int i = 0; i < c.atMostWatchers(); i++) {
                if (value(c.get(i)) == Tristate.FALSE && v(c.get(i)).reason() != null && v(c.get(i)).reason() == c) {
                    v(c.get(i)).setReason(null);
                }
            }
        } else {
            detachClause(c);
            if (locked(c)) {
                v(c.get(0)).setReason(null);
            }
        }
    }

    @Override
    protected MSClause propagate() {
        MSClause confl = null;
        int numProps = 0;
        while (this.qhead < this.trail.size()) {
            final int p = this.trail.get(this.qhead++);
            final LNGVector<MSWatcher> ws = this.watches.get(p);
            int iInd = 0;
            int jInd = 0;
            numProps++;
            while (iInd < ws.size()) {
                final MSWatcher i = ws.get(iInd);
                final int blocker = i.blocker();
                if (blocker != LIT_UNDEF && value(blocker) == Tristate.TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final MSClause c = i.clause();

                if (c.isAtMost()) {
                    final int newWatch = findNewWatch(c, p);
                    if (newWatch == LIT_UNDEF) {
                        for (int k = 0; k < c.atMostWatchers(); k++) {
                            if (c.get(k) != p && value(c.get(k)) != Tristate.FALSE) {
                                assert value(c.get(k)) == Tristate.UNDEF || value(c.get(k)) == Tristate.FALSE;
                                uncheckedEnqueue(not(c.get(k)), c);
                            }
                        }
                        ws.set(jInd++, ws.get(iInd++));
                    } else if (newWatch == LIT_ERROR) {
                        confl = c;
                        this.qhead = this.trail.size();
                        while (iInd < ws.size()) {
                            ws.set(jInd++, ws.get(iInd++));
                        }
                    } else if (newWatch == p) {
                        ws.set(jInd++, ws.get(iInd++));
                    } else {
                        iInd++;
                        final MSWatcher w = new MSWatcher(c, LIT_UNDEF);
                        this.watches.get(newWatch).push(w);
                    }
                } else {
                    final int falseLit = not(p);
                    if (c.get(0) == falseLit) {
                        c.set(0, c.get(1));
                        c.set(1, falseLit);
                    }
                    assert c.get(1) == falseLit;
                    iInd++;
                    final int first = c.get(0);
                    final MSWatcher w = new MSWatcher(c, first);
                    if (first != blocker && value(first) == Tristate.TRUE) {
                        ws.set(jInd++, w);
                        continue;
                    }
                    boolean foundWatch = false;
                    for (int k = 2; k < c.size() && !foundWatch; k++) {
                        if (value(c.get(k)) != Tristate.FALSE) {
                            c.set(1, c.get(k));
                            c.set(k, falseLit);
                            this.watches.get(not(c.get(1))).push(w);
                            foundWatch = true;
                        }
                    }
                    if (!foundWatch) {
                        ws.set(jInd++, w);
                        if (value(first) == Tristate.FALSE) {
                            confl = c;
                            this.qhead = this.trail.size();
                            while (iInd < ws.size()) {
                                ws.set(jInd++, ws.get(iInd++));
                            }
                        } else {
                            uncheckedEnqueue(first, c);
                        }
                    }
                }
            }
            ws.removeElements(iInd - jInd);
        }
        this.simpDBProps -= numProps;
        return confl;
    }

    @Override
    protected boolean litRedundant(final int p, final int abstractLevels) {
        this.analyzeStack.clear();
        this.analyzeStack.push(p);
        final int top = this.analyzeToClear.size();
        while (this.analyzeStack.size() > 0) {
            assert v(this.analyzeStack.back()).reason() != null;
            final MSClause c = v(this.analyzeStack.back()).reason();
            this.analyzeStack.pop();
            if (c.isAtMost()) {
                for (int i = 0; i < c.size(); i++) {
                    if (value(c.get(i)) != Tristate.TRUE) {
                        continue;
                    }
                    final int q = not(c.get(i));
                    if (!this.seen.get(var(q)) && v(q).level() > 0) {
                        if (v(q).reason() != null && (abstractLevel(var(q)) & abstractLevels) != 0) {
                            this.seen.set(var(q), true);
                            this.analyzeStack.push(q);
                            this.analyzeToClear.push(q);
                        } else {
                            for (int j = top; j < this.analyzeToClear.size(); j++) {
                                this.seen.set(var(this.analyzeToClear.get(j)), false);
                            }
                            this.analyzeToClear.removeElements(this.analyzeToClear.size() - top);
                            return false;
                        }
                    }
                }
            } else {
                for (int i = 1; i < c.size(); i++) {
                    final int q = c.get(i);
                    if (!this.seen.get(var(q)) && v(q).level() > 0) {
                        if (v(q).reason() != null && (abstractLevel(var(q)) & abstractLevels) != 0) {
                            this.seen.set(var(q), true);
                            this.analyzeStack.push(q);
                            this.analyzeToClear.push(q);
                        } else {
                            for (int j = top; j < this.analyzeToClear.size(); j++) {
                                this.seen.set(var(this.analyzeToClear.get(j)), false);
                            }
                            this.analyzeToClear.removeElements(this.analyzeToClear.size() - top);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void analyzeFinal(final int p, final LNGIntVector outConflict) {
        outConflict.clear();
        outConflict.push(p);
        if (decisionLevel() == 0) {
            return;
        }
        this.seen.set(var(p), true);
        int x;
        MSVariable v;
        for (int i = this.trail.size() - 1; i >= this.trailLim.get(0); i--) {
            x = var(this.trail.get(i));
            if (this.seen.get(x)) {
                v = this.vars.get(x);
                if (v.reason() == null) {
                    assert v.level() > 0;
                    outConflict.push(not(this.trail.get(i)));
                } else {
                    final MSClause c = v.reason();
                    if (!c.isAtMost()) {
                        for (int j = 1; j < c.size(); j++) {
                            if (v(c.get(j)).level() > 0) {
                                this.seen.set(var(c.get(j)), true);
                            }
                        }
                    } else {
                        for (int j = 0; j < c.size(); j++) {
                            if (value(c.get(j)) == Tristate.TRUE && v(c.get(j)).level() > 0) {
                                this.seen.set(var(c.get(j)), true);
                            }
                        }
                    }
                }
                this.seen.set(x, false);
            }
        }
        this.seen.set(var(p), false);
    }

    @Override
    protected void reduceDB() {
        int i;
        int j;
        final double extraLim = this.claInc / this.learnts.size();
        this.learnts.manualSort(MSClause.minisatComparator);
        for (i = j = 0; i < this.learnts.size(); i++) {
            final MSClause c = this.learnts.get(i);
            assert !c.isAtMost();
            if (c.size() > 2 && !locked(c) && (i < this.learnts.size() / 2 || c.activity() < extraLim)) {
                removeClause(this.learnts.get(i));
            } else {
                this.learnts.set(j++, this.learnts.get(i));
            }
        }
        this.learnts.removeElements(i - j);
    }

    @Override
    protected void removeSatisfied(final LNGVector<MSClause> cs) {
        int i;
        int j;
        for (i = j = 0; i < cs.size(); i++) {
            final MSClause c = cs.get(i);
            if (satisfied(c)) {
                removeClause(cs.get(i));
            } else {
                cs.set(j++, cs.get(i));
            }
        }
        cs.removeElements(i - j);
    }

    @Override
    protected boolean satisfied(final MSClause c) {
        if (c.isAtMost()) {
            int numFalse = 0;
            for (int i = 0; i < c.size(); i++) {
                if (value(c.get(i)) == Tristate.FALSE) {
                    numFalse++;
                    if (numFalse >= c.atMostWatchers() - 1) {
                        return true;
                    }
                }
            }
        } else {
            for (int i = 0; i < c.size(); i++) {
                if (value(c.get(i)) == Tristate.TRUE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean simplify() {
        assert decisionLevel() == 0;
        if (!this.ok || propagate() != null) {
            this.ok = false;
            return false;
        }
        if (nAssigns() == this.simpDBAssigns || (this.simpDBProps > 0)) {
            return true;
        }
        removeSatisfied(this.learnts);
        if (this.shouldRemoveSatsisfied) {
            removeSatisfied(this.clauses);
        }
        rebuildOrderHeap();
        this.simpDBAssigns = nAssigns();
        this.simpDBProps = this.clausesLiterals + this.learntsLiterals;
        return true;
    }

    /**
     * Adds an at-most k constraint.
     * @param ps  the literals of the constraint
     * @param rhs the right hand side of the constraint
     * @return {@code true} if the constraint was added, {@code false} otherwise
     */
    public boolean addAtMost(final LNGIntVector ps, final int rhs) {
        int k = rhs;
        assert decisionLevel() == 0;
        if (!this.ok) {
            return false;
        }
        ps.sort();
        int p;
        int i;
        int j;
        for (i = j = 0, p = LIT_UNDEF; i < ps.size(); i++) {
            if (value(ps.get(i)) == Tristate.TRUE) {
                k--;
            } else if (ps.get(i) == not(p)) {
                p = ps.get(i);
                j--;
                k--;
            } else if (value(ps.get(i)) != Tristate.FALSE && ps.get(i) != p) {
                p = ps.get(i);
                ps.set(j++, p);
            }
        }
        ps.removeElements(i - j);
        if (k >= ps.size()) {
            return true;
        }
        if (k < 0) {
            this.ok = false;
            return false;
        }
        if (k == 0) {
            for (i = 0; i < ps.size(); i++) {
                uncheckedEnqueue(not(ps.get(i)), null);
                if (this.incremental) {
                    this.unitClauses.push(not(ps.get(i)));
                }
            }
            this.ok = propagate() == null;
            return this.ok;
        }
        final MSClause cr = new MSClause(ps, false, true);
        cr.setAtMostWatchers(ps.size() - k + 1);
        this.clauses.push(cr);
        attachClause(cr);
        return true;
    }

    /**
     * Detaches a given at-most clause.
     * @param c the at-most clause.
     */
    private void detachAtMost(final MSClause c) {
        for (int i = 0; i < c.atMostWatchers(); i++) {
            this.watches.get(c.get(i)).remove(new MSWatcher(c, c.get(i)));
        }
        this.clausesLiterals -= c.size();
    }

    /**
     * The main search procedure of the CDCL algorithm.
     * @param nofConflicts the number of conflicts till the next restart
     * @return a {@link Tristate} representing the result.  {@code FALSE} if the formula is UNSAT, {@code TRUE} if the
     * formula is SAT, and {@code UNKNOWN} if the state is not known yet (restart)
     */
    private Tristate search(final int nofConflicts) {
        if (!this.ok) {
            return Tristate.FALSE;
        }
        int conflictC = 0;
        this.selectionOrderIdx = 0;
        while (true) {
            final MSClause confl = propagate();
            if (confl != null) {
                if (this.handler != null && !this.handler.detectedConflict()) {
                    this.canceledByHandler = true;
                    return Tristate.UNDEF;
                }
                conflictC++;
                if (decisionLevel() == 0) {
                    return Tristate.FALSE;
                }
                final LNGIntVector learntClause = new LNGIntVector();
                analyze(confl, learntClause);
                cancelUntil(this.analyzeBtLevel);
                if (this.analyzeBtLevel < this.selectionOrder.size()) {
                    this.selectionOrderIdx = this.analyzeBtLevel;
                }
                if (learntClause.size() == 1) {
                    uncheckedEnqueue(learntClause.get(0), null);
                    this.unitClauses.push(learntClause.get(0));
                } else {
                    final MSClause cr = new MSClause(learntClause, true);
                    this.learnts.push(cr);
                    attachClause(cr);
                    if (!this.incremental) {
                        claBumpActivity(cr);
                    }
                    uncheckedEnqueue(learntClause.get(0), cr);
                }
                decayActivities();
            } else {
                if (nofConflicts >= 0 && conflictC >= nofConflicts) {
                    cancelUntil(0);
                    return Tristate.UNDEF;
                }
                if (!this.incremental) {
                    if (decisionLevel() == 0 && !simplify()) {
                        return Tristate.FALSE;
                    }
                    if (this.learnts.size() - nAssigns() >= this.maxLearnts) {
                        reduceDB();
                    }
                }
                int next = LIT_UNDEF;
                while (decisionLevel() < this.assumptions.size()) {
                    final int p = this.assumptions.get(decisionLevel());
                    if (value(p) == Tristate.TRUE) {
                        this.trailLim.push(this.trail.size());
                    } else if (value(p) == Tristate.FALSE) {
                        analyzeFinal(not(p), this.conflict);
                        return Tristate.FALSE;
                    } else {
                        next = p;
                        break;
                    }
                }
                if (next == LIT_UNDEF) {
                    next = pickBranchLit();
                    if (next == LIT_UNDEF) {
                        return Tristate.TRUE;
                    }
                }
                this.trailLim.push(this.trail.size());
                uncheckedEnqueue(next, null);
            }
        }
    }

    private int findNewWatch(final MSClause c, final int p) {
        assert c.isAtMost();
        int newWatch = LIT_ERROR;
        int numFalse = 0;
        int numTrue = 0;
        final int maxTrue = c.size() - c.atMostWatchers() + 1;
        for (int q = 0; q < c.atMostWatchers(); q++) {
            final Tristate val = value(c.get(q));
            if (val == Tristate.UNDEF) {
                continue;
            } else if (val == Tristate.FALSE) {
                numFalse++;
                if (numFalse >= c.atMostWatchers() - 1) {
                    return p;
                }
                continue;
            }
            assert val == Tristate.TRUE;
            numTrue++;
            if (numTrue > maxTrue) {
                return LIT_ERROR;
            }
            if (c.get(q) == p) {
                assert newWatch == LIT_ERROR;
                for (int next = c.atMostWatchers(); next < c.size(); next++) {
                    if (value(c.get(next)) != Tristate.TRUE) {
                        newWatch = c.get(next);
                        c.set(next, c.get(q));
                        c.set(q, newWatch);
                        return newWatch;
                    }
                }
                newWatch = LIT_UNDEF;
            }
        }
        assert newWatch == LIT_UNDEF;
        if (numTrue > 1) {
            return LIT_ERROR;
        } else {
            return LIT_UNDEF;
        }
    }

    /**
     * Analyzes a given conflict clause wrt. the current solver state.  A 1-UIP clause is created during this procedure
     * and the new backtracking level is stored in the solver state.
     * @param conflictClause the conflict clause to start the resolution analysis with
     * @param outLearnt      the vector where the new learnt 1-UIP clause is stored
     */
    private void analyze(final MSClause conflictClause, final LNGIntVector outLearnt) {
        MSClause c = conflictClause;
        int pathC = 0;
        int p = LIT_UNDEF;
        outLearnt.push(-1);
        int index = this.trail.size() - 1;
        do {
            assert c != null;
            if (c.isAtMost()) {
                for (int j = 0; j < c.size(); j++) {
                    if (value(c.get(j)) != Tristate.TRUE) {
                        continue;
                    }
                    final int q = not(c.get(j));
                    if (!this.seen.get(var(q)) && v(q).level() > 0) {
                        varBumpActivity(var(q));
                        this.seen.set(var(q), true);
                        if (v(q).level() >= decisionLevel()) {
                            pathC++;
                        } else {
                            outLearnt.push(q);
                        }
                    }
                }
            } else {
                if (!this.incremental && c.learnt()) {
                    claBumpActivity(c);
                }
                for (int j = (p == LIT_UNDEF) ? 0 : 1; j < c.size(); j++) {
                    final int q = c.get(j);
                    if (!this.seen.get(var(q)) && v(q).level() > 0) {
                        varBumpActivity(var(q));
                        this.seen.set(var(q), true);
                        if (v(q).level() >= decisionLevel()) {
                            pathC++;
                        } else {
                            outLearnt.push(q);
                        }
                    }
                }
            }
            while (!this.seen.get(var(this.trail.get(index--)))) {
            }
            p = this.trail.get(index + 1);
            c = v(p).reason();
            this.seen.set(var(p), false);
            pathC--;
        } while (pathC > 0);
        outLearnt.set(0, not(p));
        simplifyClause(outLearnt);
    }

    /**
     * Minimizes a given learnt clause depending on the minimization method of the solver configuration.
     * @param outLearnt the learnt clause which should be minimized
     */
    private void simplifyClause(final LNGIntVector outLearnt) {
        int i;
        int j;
        this.analyzeToClear = new LNGIntVector(outLearnt);
        if (this.ccminMode == MiniSatConfig.ClauseMinimization.DEEP) {
            int abstractLevel = 0;
            for (i = 1; i < outLearnt.size(); i++) {
                abstractLevel |= abstractLevel(var(outLearnt.get(i)));
            }
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null || !litRedundant(outLearnt.get(i), abstractLevel)) {
                    outLearnt.set(j++, outLearnt.get(i));
                }
            }
        } else if (this.ccminMode == MiniSatConfig.ClauseMinimization.BASIC) {
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null) {
                    outLearnt.set(j++, outLearnt.get(i));
                } else {
                    final MSClause c = v(outLearnt.get(i)).reason();
                    assert !c.isAtMost();
                    for (int k = 1; k < c.size(); k++) {
                        if (!this.seen.get(var(c.get(k))) && v(c.get(k)).level() > 0) {
                            outLearnt.set(j++, outLearnt.get(i));
                            break;
                        }
                    }
                }
            }
        } else {
            i = j = outLearnt.size();
        }
        outLearnt.removeElements(i - j);
        this.analyzeBtLevel = 0;
        if (outLearnt.size() > 1) {
            int max = 1;
            for (int k = 2; k < outLearnt.size(); k++) {
                if (v(outLearnt.get(k)).level() > v(outLearnt.get(max)).level()) {
                    max = k;
                }
            }
            final int p = outLearnt.get(max);
            outLearnt.set(max, outLearnt.get(1));
            outLearnt.set(1, p);
            this.analyzeBtLevel = v(p).level();
        }
        for (int l = 0; l < this.analyzeToClear.size(); l++) {
            this.seen.set(var(this.analyzeToClear.get(l)), false);
        }
    }

    /**
     * Performs an unconditional backtrack to level zero.
     */
    private void completeBacktrack() {
        for (int v = 0; v < this.vars.size(); v++) {
            final MSVariable var = this.vars.get(v);
            var.assign(Tristate.UNDEF);
            var.setReason(null);
            if (!this.orderHeap.inHeap(v) && var.decision()) {
                this.orderHeap.insert(v);
            }
        }
        this.trail.clear();
        this.trailLim.clear();
        this.qhead = 0;
    }

    /**
     * Performs a simple removal of clauses used during the loading of an older state.
     * @param c the clause to remove
     */
    private void simpleRemoveClause(final MSClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                this.watches.get(c.get(i)).remove(new MSWatcher(c, c.get(i)));
            }
        } else {
            this.watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
            this.watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        }
    }
}
