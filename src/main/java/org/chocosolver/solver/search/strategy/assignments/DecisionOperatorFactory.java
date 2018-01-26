/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.assignments;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;

/**
 * Create serializable decisions. 
 * Decisions are static nested classes because serialization of lambda functions or anonymous classes is compiler-dependent.
 * Furthermore, the serialization of the singleton pattern requires a special treatment.
 * 
 * @see <a href="@linkp http://stackoverflow.com/questions/19440511/explanation-needed-why-adding-implements-serializable-to-singleton-class-is-ins">stack overflow</a>
 * @author Arnaud Malapert
 *
 */
public final class DecisionOperatorFactory {

    private DecisionOperatorFactory() {}

    public static DecisionOperator<IntVar> makeIntEq() {
        return IntEqDecision.getInstance();
    }

    public static DecisionOperator<IntVar> makeIntNeq() {
        return IntNeqDecision.getInstance();
    }

    public static DecisionOperator<IntVar> makeIntSplit() {
        return IntSplitDecision.getInstance();
    }

    public static DecisionOperator<IntVar> makeIntReverseSplit() {
        return IntReverseSplitDecision.getInstance();
    }

    public static DecisionOperator<SetVar> makeSetForce() {
        return SetForceDecision.getInstance();
    }

    public static DecisionOperator<SetVar> makeSetRemove() {
        return SetRemoveDecision.getInstance();
    }
    // INTEGERS
    private static final class IntEqDecision implements DecisionOperator<IntVar> {

        private static final long serialVersionUID = 7293773317776136982L;

        private final static IntEqDecision INSTANCE = new IntEqDecision();

        public final static IntEqDecision getInstance() {
            return INSTANCE;
        }

        /**
         * readResolve method to preserve singleton property
         */
        private Object readResolve() {
            // Return the one true INSTANCE and let the garbage collector
            // take care of the INSTANCE impersonator.
            return INSTANCE;
        }

        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.instantiateTo(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.removeValue(value, cause);
        }

        @Override
        public String toString() {
            return " == ";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return makeIntNeq();
        }
    }

    private static final class IntNeqDecision implements DecisionOperator<IntVar> {

        private static final long serialVersionUID = 3056222234436601667L;
        private final static IntNeqDecision INSTANCE = new IntNeqDecision();

        public final static IntNeqDecision getInstance() {
            return INSTANCE;
        }

        /**
         * readResolve method to preserve singleton property
         */
        private Object readResolve() {
            // Return the one true INSTANCE and let the garbage collector
            // take care of the INSTANCE impersonator.
            return INSTANCE;
        }

        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.removeValue(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.instantiateTo(value, cause);
        }

        @Override
        public String toString() {
            return " != ";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return makeIntEq();
        }
    }

    private static final class IntSplitDecision implements DecisionOperator<IntVar> {

        private static final long serialVersionUID = 2796498653106384502L;
        private final static IntSplitDecision INSTANCE = new IntSplitDecision();

        public final static IntSplitDecision getInstance() {
            return INSTANCE;
        }

        private IntSplitDecision() {
            super();
        }

        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateUpperBound(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateLowerBound(value + 1, cause);
        }

        @Override
        public String toString() {
            return " <= ";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return makeIntReverseSplit();
        }

    }

    private static final class IntReverseSplitDecision implements DecisionOperator<IntVar> {

        private static final long serialVersionUID = -4155926684198463505L;

        private final static IntReverseSplitDecision INSTANCE = new IntReverseSplitDecision();

        public final static IntReverseSplitDecision getInstance() {
            return INSTANCE;
        }

        private IntReverseSplitDecision() {}

        /**
         * readResolve method to preserve singleton property
         */
        private Object readResolve() {
            // Return the one true INSTANCE and let the garbage collector
            // take care of the INSTANCE impersonator.
            return INSTANCE;
        }

        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateLowerBound(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateUpperBound(value - 1, cause);
        }

        @Override
        public String toString() {
            return " >= ";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return makeIntSplit();
        }
    }


    // SETS
    private static final class SetForceDecision implements DecisionOperator<SetVar> {

        private static final long serialVersionUID = -4868225105307378160L;

        private final static SetForceDecision INSTANCE = new DecisionOperatorFactory.SetForceDecision();

        public final static SetForceDecision getInstance() {
            return INSTANCE;
        }

        private SetForceDecision() {}

        /**
         * readResolve method to preserve singleton property
         */
        private Object readResolve() {
            // Return the one true INSTANCE and let the garbage collector
            // take care of the INSTANCE impersonator.
            return INSTANCE;
        }

        @Override
        public boolean apply(SetVar var, int element, ICause cause) throws ContradictionException {
            return var.force(element, cause);
        }

        @Override
        public boolean unapply(SetVar var, int element, ICause cause) throws ContradictionException {
            return var.remove(element, cause);
        }

        @Override
        public String toString() {
            return " contains ";
        }

        @Override
        public DecisionOperator<SetVar> opposite() {
            return DecisionOperatorFactory.makeSetRemove();
        }
    }

    private static final class SetRemoveDecision implements DecisionOperator<SetVar> {

        private static final long serialVersionUID = -580239209082758455L;

        private final static SetRemoveDecision INSTANCE = new SetRemoveDecision();

        public final static SetRemoveDecision getInstance() {
            return INSTANCE;
        }

        private SetRemoveDecision() {}

        /**
         * readResolve method to preserve singleton property
         */
        private Object readResolve() {
            // Return the one true INSTANCE and let the garbage collector
            // take care of the INSTANCE impersonator.
            return INSTANCE;
        }

        @Override
        public boolean apply(SetVar var, int element, ICause cause) throws ContradictionException {
            return var.remove(element, cause);
        }

        @Override
        public boolean unapply(SetVar var, int element, ICause cause) throws ContradictionException {
            return var.force(element, cause);
        }

        @Override
        public String toString() {
            return " !contains ";
        }

        @Override
        public DecisionOperator<SetVar> opposite() {
            return DecisionOperatorFactory.makeSetForce();
        }
    }
}
