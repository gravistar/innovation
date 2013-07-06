package util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import machina.PropNetStateMachine;
import org.junit.rules.ErrorCollector;
import propnet.vanilla.PropNetFactory;
import propnet.nativecode.NativePropNetFactory;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Colut;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/13/13
 * Time: 8:42 PM
 * Description:
 *      This is the core correctness test.  Checks that the output of a propnet state machine
 *      is the same as the backward prover state machine.
 */
public class MachineTestUtil {
    public static Random rand = new Random(System.currentTimeMillis());
    public static boolean verbose = true;
    public static boolean superVerbose = true;
    public static TestType testType = TestType.COLLECTOR;
    public static ErrorCollector collector = new ErrorCollector();

    public static enum TestType {
        ASSERT, COLLECTOR, CUSTOM
    }

    // game name is for logging
    public static void stepThroughVanilla(List<Rule> rules, String gameName) {
        stepThrough(rules, PropNetStateMachine.create(PropNetFactory.createForStateMachine(rules)), gameName);
    }

    public static void stepThroughNative(List<Rule> rules, String gameName) {
        stepThrough(rules, PropNetStateMachine.create(NativePropNetFactory.createForStateMachine(rules)), gameName);
    }

    /**
     * The actual correctness comparison method
     * @param rules
     * @param pnsm
     */
    public static void stepThrough(List<Rule> rules, PropNetStateMachine pnsm, String gameName) {
        BackwardStateMachine bsm = BackwardStateMachine.createForRules(rules);

        Set<Dob> bsmState = bsm.getInitial();
        Set<Dob> pnsmState = pnsm.getInitial();

        if (verbose)
            pnsm.printMappings();

        boolean endGame = false;
        int iteration = 1;
        while (!endGame) {
            if (verbose) {
                System.out.println("[ITERATION " + iteration++ + "]");
                System.out.println("[BSM STATE] " + bsmState);
                System.out.println("[PNSM STATE] " + pnsmState);
            }

            // do the dobs in the states have the same name?
            switch (testType) {
                case ASSERT:
                    assertTrue("mismatch between state! pnsm: " + pnsmState + " bsm: " + bsmState, dobMatch(bsmState, pnsmState));
                case CUSTOM: {
                    boolean match = dobMatch(bsmState, pnsmState);
                    if (!match) {
                        System.out.println(gameName + " failed!");
                        System.out.println("mismatch between state! pnsm: " + pnsmState + " bsm: " + bsmState);
                        System.out.println();
                        return;
                    }
                }
            }
            ListMultimap<Dob,Dob> bsmActions = bsm.getActions(bsmState);
            ListMultimap<Dob,Dob> pnsmActions = pnsm.getActions(pnsmState);

            List<Dob> bsmRoles = Lists.newArrayList(bsmActions.keySet());
            List<Dob> pnsmRoles = Lists.newArrayList(pnsmActions.keySet());

            Map<Dob,Dob> matchedRoles = matchDobList(bsmRoles, pnsmRoles);

            // do the dobs in the roles have the same name?
            switch (testType) {
                case ASSERT:
                    assertTrue("mismatch between roles! pnsm: " + pnsmRoles + " bsm: " + bsmRoles, dobMatch(bsmRoles, pnsmRoles));
                case CUSTOM: {
                    boolean match = dobMatch(pnsmRoles, bsmRoles);
                    if (!match) {
                        System.out.println(gameName + " failed!");
                        System.out.println("mismatch between roles! pnsm: " + pnsmRoles + " bsm: " + bsmRoles);
                        System.out.println();
                        return;
                    }
                }
            }

            Map<Dob,Dob> pickedBSMActions = Maps.newHashMap();
            Map<Dob,Dob> pickedPNSMActions = Maps.newHashMap();

            for (Dob role : bsmRoles) {
                List<Dob> bsmRoleActions = bsmActions.get(role);

                // find matching action dob for the pnsm
                List<Dob> pnsmRoleActions = pnsmActions.get(matchedRoles.get(role));

                Map<Dob,Dob> matchedActions = matchDobList(bsmRoleActions, pnsmRoleActions);

                // just need to check for size since the matching takes care of name comparison
                switch (testType) {
                    case ASSERT:
                        assertTrue("mismatch between actions for role" + role, matchedActions.keySet().size() ==
                                bsmRoleActions.size());
                    case CUSTOM: {
                        boolean match = matchedActions.keySet().size() == bsmRoleActions.size();
                        if (!match) {
                            System.out.println(gameName + " failed!");
                            System.out.println("mismatch between actions for role" + role);
                            System.out.println();
                            return;
                        }
                    }
                }

                int actionId = rand.nextInt(bsmRoleActions.size());
                Dob bsmRoleAction = bsmRoleActions.get(actionId);

                // make sure machines submit the same actions
                pickedBSMActions.put(role, bsmRoleAction);
                pickedPNSMActions.put(Preconditions.checkNotNull(matchedRoles.get(role), "no matching role for " + role),
                        Preconditions.checkNotNull(matchedActions.get(bsmRoleAction), "no matching action for " + bsmRoleAction));

                if (verbose) {
                    System.out.println("[BSM ACTION] " + bsmRoleAction);
                    System.out.println("[PNSM ACTION] " + matchedActions.get(bsmRoleAction));
                }
            }

            if (verbose) {
                System.out.println("[BSM TERMINAL] " + bsm.isTerminal(bsmState));
                System.out.println("[PNSM TERMINAL] " + pnsm.isTerminal(pnsmState));
            }

            switch (testType) {
                case ASSERT:
                    assertTrue("mismatch between terminal!", bsm.isTerminal(bsmState) == pnsm.isTerminal(pnsmState));
                case CUSTOM: {
                    boolean passed = bsm.isTerminal(bsmState) == pnsm.isTerminal(pnsmState);
                    if (!passed) {
                        System.out.println(gameName + " failed!");
                        System.out.println("mismatch between terminal!");
                        System.out.println();
                        return;
                    }
                }
            }
            // check goals
            if (bsm.isTerminal(bsmState)) {

                Map<Dob,Integer> bsmGoals = bsm.getGoals(bsmState);
                Map<Dob,Integer> pnsmGoals = pnsm.getGoals(pnsmState);

                for (Dob bsmRole : matchedRoles.keySet()) {
                    Dob pnsmRole = matchedRoles.get(bsmRole);
                    switch (testType) {
                        case ASSERT:
                            assertEquals(bsmGoals.get(bsmRole), pnsmGoals.get(pnsmRole));
                        case CUSTOM: {
                            boolean passed = bsmGoals.get(bsmRole) == pnsmGoals.get(pnsmRole);
                            if (!passed) {
                                System.out.println(gameName + " failed!");
                                System.out.println(" mismatch between goals for role " + bsmRole +
                                    " ! bsm: " + bsmGoals.get(bsmRole) + " pnsm: " + pnsmGoals.get(pnsmRole));
                                System.out.println();
                                return;
                            }
                        }
                    }
                }
                return;
            }

            bsmState = bsm.nextState(bsmState, pickedBSMActions);
            pnsmState = pnsm.nextState(pnsmState, pickedPNSMActions);

            if (verbose)
                System.out.println();
        }
    }

    // Utility matching methods
    public static boolean dobMatch(Collection<Dob> lhs, Collection<Dob> rhs) {
        Set<String> l = Sets.newHashSet();
        for (Dob d : lhs)
            l.add(d.toString());
        Set<String> r = Sets.newHashSet();
        for (Dob d : rhs)
            r.add(d.toString());
        return  Colut.containsSame(l, r);
    }

    public static Map<Dob,Dob> matchDobList(List<Dob> left, List<Dob> right) {
        Map<Dob,Dob> ret = Maps.newHashMap();
        Set<Dob> used = Sets.newHashSet();
        for (Dob l : left) {
            for (Dob r : right) {
                if (Dob.compare(l, r) == 0 && !used.contains(r) && !used.contains(l)) {
                    used.add(l);
                    used.add(r);
                    ret.put(l, r);
                    break;
                }
            }
        }
        return ret;
    }
}
