/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.explanations;

import choco.kernel.common.util.iterators.DisposableValueIterator;
import com.sun.tools.javac.util.Pair;
import solver.ICause;
import solver.Solver;
import solver.constraints.propagators.Propagator;
import solver.exception.ContradictionException;
import solver.search.strategy.decision.Decision;
import solver.variables.IntVar;
import solver.variables.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: njussien
 * Date: 26 oct. 2010
 * Time: 14:18:18
 * <p/>
 * An RecorderExplanationEngine is used to record explanations throughout computation.
 * Here we just record the explanations in a HashMap ...
 * <p/>
 */
public class RecorderExplanationEngine extends ExplanationEngine {

    HashMap<Variable, OffsetIStateBitset> removedvalues; // maintien du domaine courant
    HashMap<IntVar, HashMap<Integer, ValueRemoval>> valueremovals; // maintien de la base de deduction
    HashMap<Deduction, Explanation> database; // base d'explications

    HashMap<Variable, HashMap<Integer, Pair<VariableAssignment, Integer>>> variableassignments; // maintien de la base de VariableAssignment
    HashMap<Variable, HashMap<Integer, VariableRefutation>> variablerefutations; // maintien de la base de VariableRefutation


    public RecorderExplanationEngine(Solver solver) {
        super(solver);
        removedvalues = new HashMap<Variable, OffsetIStateBitset>();
        valueremovals = new HashMap<IntVar, HashMap<Integer, ValueRemoval>>();
        database = new HashMap<Deduction, Explanation>();
        variableassignments = new HashMap<Variable, HashMap<Integer, Pair<VariableAssignment, Integer>>>();
        variablerefutations = new HashMap<Variable, HashMap<Integer, VariableRefutation>>();

        for(Variable v : solver.getVars()) {
            getRemovedValues((IntVar) v); // TODO make a more generic method for that
        }
    }

    @Override
    public OffsetIStateBitset getRemovedValues(IntVar v) {
        OffsetIStateBitset toreturn = removedvalues.get(v);
        if (toreturn == null) {
            toreturn = new OffsetIStateBitset(v); // .getSolver().getEnvironment().makeBitSet(v.getUB());
            removedvalues.put(v, toreturn);
            valueremovals.put(v, new HashMap<Integer, ValueRemoval>());
        }
        return toreturn;
    }

    @Override
    public Explanation retrieve(IntVar var, int val) {
        return database.get(getValueRemoval(var, val));
    }

    protected ValueRemoval getValueRemoval(IntVar var, int val) {
        ValueRemoval vr = valueremovals.get(var).get(val);
        if (vr == null) {
            vr = new ValueRemoval(var, val);
            valueremovals.get(var).put(val, vr);
        }
        return vr;
    }


    @Override
    public VariableAssignment getVariableAssignment(IntVar var, int val) {
        HashMap mapvar = variableassignments.get(var);
        if (mapvar == null) {
            variableassignments.put(var, new HashMap<Integer, Pair<VariableAssignment, Integer>>());
            variableassignments.get(var).put(val, new Pair<VariableAssignment, Integer>(
                    new VariableAssignment(var, val), this.solver.getEnvironment().getWorldIndex()));
        }
        Pair<VariableAssignment, Integer> vrw = variableassignments.get(var).get(val);
        if (vrw == null) {
            vrw = new Pair<VariableAssignment, Integer>(new VariableAssignment(var, val), this.solver.getEnvironment().getWorldIndex());
            variableassignments.get(var).put(val, vrw);
        }
        return vrw.fst;
    }

    @Override
    public VariableRefutation getVariableRefutation(IntVar var, int val, Decision dec) {
        HashMap mapvar = variablerefutations.get(var);
        if (mapvar == null) {
            variablerefutations.put(var, new HashMap<Integer, VariableRefutation>());
            variablerefutations.get(var).put(val, new VariableRefutation(var, val, dec));
        }
        VariableRefutation vr = variablerefutations.get(var).get(val);
        if (vr == null) {
            vr = new VariableRefutation(var, val, dec);
            variablerefutations.get(var).put(val, vr);
        }
        vr.decision = dec;
        return vr;
    }


    @Override
    public void removeValue(IntVar var, int val, ICause cause) {
        OffsetIStateBitset invdom = getRemovedValues(var);
        Deduction vr = getValueRemoval(var, val);
        Explanation expl = cause.explain(vr);
        database.put(vr, expl);
        invdom.set(val);
        emList.onRemoveValue(var, val, cause, expl);
    }

    @Override
    public void updateLowerBound(IntVar var, int old, int val, ICause cause) {
        OffsetIStateBitset invdom = getRemovedValues(var);
        for (int v = old; v < val; v++) {    // it�ration explicite des valeurs retir�es
            Deduction vr = getValueRemoval(var, v);
            Explanation expl = cause.explain(vr);
            database.put(vr, expl);
            invdom.set(v);
            emList.onUpdateLowerBound(var, old, val, cause, expl);
        }
    }

    @Override
    public void updateUpperBound(IntVar var, int old, int val, ICause cause) {
        OffsetIStateBitset invdom = getRemovedValues(var);
        for (int v = old; v > val; v--) {    // it�ration explicite des valeurs retir�es
            Deduction vr = getValueRemoval(var, v);
            Explanation explain = cause.explain(vr);
            database.put(vr, explain);
            invdom.set(v);
            emList.onUpdateUpperBound(var, old, val, cause, explain);
        }
    }


    @Override
    public void instantiateTo(IntVar var, int val, ICause cause) {
        OffsetIStateBitset invdom = getRemovedValues(var);
        DisposableValueIterator it = var.getValueIterator(true);
        while (it.hasNext()) {
            int v = it.next();
            if ( v != val ) {
                Deduction vr = getValueRemoval(var,v);
                Explanation explain = cause.explain(vr);
                database.put(vr, explain);
                invdom.set(v);
                emList.onInstantiateTo(var, val, cause, explain);
            }
        }
    }

    @Override
    public Explanation flatten(Explanation expl) {
        Explanation toreturn = new Explanation(null, null);

        Set<Deduction> toexpand = new HashSet<Deduction>();
        Set<Deduction> expanded = new HashSet<Deduction>();

        if (expl.deductions != null) {
            toexpand = new HashSet<Deduction>(expl.deductions); //
        }
        while (!toexpand.isEmpty()) {
            Deduction d = toexpand.iterator().next();
            toexpand.remove(d);
            expanded.add(d);
            Explanation e = database.get(d);

            if (e != null) {
                if (e.contraintes != null) {
                    for (Propagator prop : e.contraintes) {
                        toreturn.add(prop);
                    }
                }
                if (e.deductions != null) {

                    for (Deduction ded : e.deductions) {
                        if (!expanded.contains(ded)) {
                            toexpand.add(ded);
                        }
                    }
                }
            } else {
                toreturn.add(d);
            }
        }
        return toreturn;
    }

    @Override
    public Explanation flatten(IntVar var, int val) {
        // TODO check that it is always called with val NOT in var
        return flatten(getValueRemoval(var, val));
    }

    @Override
    public Explanation flatten(Deduction deduction) {
        Explanation expl = new Explanation(null, null);
        expl.add(deduction);
        return flatten(expl);
    }

    @Override
    public Deduction explain(IntVar var, int val) {
        return explain(getValueRemoval(var, val));
    }

    @Override
    public Deduction explain(Deduction deduction) {
        return deduction;
    }


    private Decision updateVRExplainUponbacktracking(int nworld, Explanation expl) {
        Decision dec = solver.getSearchLoop().decision; // the current decision to undo
        while (dec != null && nworld > 1) {
            dec = dec.getPrevious();
            nworld--;
        }
        if (dec != null) {
            Deduction vr = dec.getPositiveDeduction();
            Deduction assign = dec.getNegativeDeduction();
            expl.remove(assign);
            database.put(vr, flatten(expl));
        }
        return dec;
    }


    @Override
    public int getWorldNumber(Variable va, int val) {
        Pair<VariableAssignment, Integer> vr = variableassignments.get(va).get(val);
        if (vr != null) {
            return vr.snd;
        }
        else {
            System.err.println("RecorderExplanationEngine.getWorldNumber");
            System.err.println("incoherent state !!!");
            System.exit(-1);
            return 0;
        }
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        if ((cex.v != null) || (cex.c != null)) { // contradiction on domain wipe out
            Explanation expl = (cex.v != null) ? cex.v.explain(VariableState.DOM)
                    : cex.c.explain(null);
            Solver solver = (cex.v != null) ? cex.v.getSolver() : cex.c.getConstraint().getVariables()[0].getSolver();
            Explanation complete = flatten(expl);
            int upto = complete.getMostRecentWorldToBacktrack(this);
            solver.getSearchLoop().overridePreviousWorld(upto);
            Decision dec = updateVRExplainUponbacktracking(upto, complete);
            emList.onContradiction(cex, complete, upto, dec);
        } else {
            System.err.println("RecorderExplanationEngine.onContradiction");
            System.err.println("incoherent state !!!");
            System.exit(-1);
        }
    }

    @Override
    public void onRemoveValue(IntVar var, int val, ICause cause, Explanation explanation) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("::EXPL:: REMVAL " + val + " FROM " + var + " APPLYING " + cause + " BECAUSE OF " + explanation);
        }
    }

    @Override
    public void onUpdateLowerBound(IntVar intVar, int old, int value, ICause cause, Explanation explanation) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("::EXPL:: UPLOWB from " + old + " to " + value + " FOR " + intVar + " APPLYING " + cause + " BECAUSE OF " + explanation);
        }
    }

    @Override
    public void onUpdateUpperBound(IntVar intVar, int old, int value, ICause cause, Explanation explanation) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("::EXPL:: UPUPPB from " + old + " to " + value + " FOR " + intVar + " APPLYING " + cause + " BECAUSE OF " + explanation);
        }
    }

    @Override
    public void onInstantiateTo(IntVar var, int val, ICause cause, Explanation explanation) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("::EXPL:: INST to " + val + " FOR " + var + " APPLYING " + cause + " BECAUSE OF " + explanation);
        }
    }

    @Override
    public void onContradiction(ContradictionException cex, Explanation explanation, int upTo, Decision decision) {
        if (LOGGER.isInfoEnabled()) {
            if (cex.v != null) {
                LOGGER.info("::EXPL:: CONTRADICTION on " + cex.v + " BECAUSE " + explanation);
            }
            else if (cex.c != null) {
                LOGGER.info("::EXPL:: CONTRADICTION on " + cex.c + " BECAUSE " + explanation);
            }
            LOGGER.info("::EXPL:: BACKTRACK on " + decision +" (up to " + upTo + " level(s))");
        }

    }
}
