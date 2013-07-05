package util;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import machina.PropNetStateMachine;
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
    public static boolean verbose = false;

    public static void stepThroughVanilla(List<Rule> rules) {
        stepThrough(rules, PropNetStateMachine.create(PropNetFactory.createForStateMachine(rules)));
    }

    public static void stepThroughNative(List<Rule> rules) {
        stepThrough(rules, PropNetStateMachine.create(NativePropNetFactory.createForStateMachine(rules)));
    }

    /**
     * The actual correctness comparison method
     * @param rules
     * @param pnsm
     */
    public static void stepThrough(List<Rule> rules, PropNetStateMachine pnsm) {
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
            assertTrue("mismatch between state! pnsm: " + pnsmState + " bsm: " + bsmState, dobMatch(bsmState, pnsmState));

            ListMultimap<Dob,Dob> bsmActions = bsm.getActions(bsmState);
            ListMultimap<Dob,Dob> pnsmActions = pnsm.getActions(pnsmState);

            List<Dob> bsmRoles = Lists.newArrayList(bsmActions.keySet());
            List<Dob> pnsmRoles = Lists.newArrayList(pnsmActions.keySet());

            Map<Dob,Dob> matchedRoles = matchDobList(bsmRoles, pnsmRoles);

            // do the dobs in the roles have the same name?
            assertTrue("mismatch between roles! pnsm: " + pnsmRoles + " bsm: " + bsmRoles, dobMatch(bsmRoles, pnsmRoles));

            Map<Dob,Dob> pickedBSMActions = Maps.newHashMap();
            Map<Dob,Dob> pickedPNSMActions = Maps.newHashMap();

            for (Dob role : bsmRoles) {
                List<Dob> bsmRoleActions = bsmActions.get(role);

                // find matching action dob for the pnsm
                List<Dob> pnsmRoleActions = pnsmActions.get(matchedRoles.get(role));

                Map<Dob,Dob> matchedActions = matchDobList(bsmRoleActions, pnsmRoleActions);

                // just need to check for size since the matching takes care of name comparison
                assertTrue("mismatch between actions for role" + role, matchedActions.keySet().size() ==
                        bsmRoleActions.size());

                int actionId = rand.nextInt(bsmRoleActions.size());
                Dob bsmRoleAction = bsmRoleActions.get(actionId);

                // make sure machines submit the same actions
                pickedBSMActions.put(role, bsmRoleAction);
                pickedPNSMActions.put(matchedRoles.get(role), matchedActions.get(bsmRoleAction));

                if (verbose) {
                    System.out.println("[BSM ACTION] " + bsmRoleAction);
                    System.out.println("[PNSM ACTION] " + matchedActions.get(bsmRoleAction));
                }
            }

            if (verbose) {
                System.out.println("[BSM TERMINAL] " + bsm.isTerminal(bsmState));
                System.out.println("[PNSM TERMINAL] " + pnsm.isTerminal(pnsmState));
            }
            assertTrue("mismatch between terminal!", bsm.isTerminal(bsmState) == pnsm.isTerminal(pnsmState));

            // check goals
            if (bsm.isTerminal(bsmState)) {

                Map<Dob,Integer> bsmGoals = bsm.getGoals(bsmState);
                Map<Dob,Integer> pnsmGoals = pnsm.getGoals(pnsmState);

                for (Dob bsmRole : matchedRoles.keySet()) {
                    Dob pnsmRole = matchedRoles.get(bsmRole);

                    assertEquals(bsmGoals.get(bsmRole), pnsmGoals.get(pnsmRole));
                }
                break;
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
