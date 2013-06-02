package propnet;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import machina.PropNetStateMachine;
import org.junit.Test;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/1/13
 * Time: 4:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetMachineTest {

    @Test
    public void manyStepthroughTest() {
        for (int i=0; i<20; i++)
            stepthroughTest();
    }

    @Test
    public void stepthroughTest() {
        // does the actions spit out by propnet state machine at each step match
        // backward state machine?

        Random rand = new Random();
        List<Rule> rules = SimpleGames.getButtonsAndLights();

        BackwardStateMachine bsm = BackwardStateMachine.createForRules(rules);
        PropNetStateMachine pnsm = PropNetStateMachine.createPropNetStateMachine(rules);

        pnsm.printMappings();
        System.out.println();

        Set<Dob> bsmState = bsm.getInitial();
        Set<Dob> pnsmState = pnsm.getInitial();

        System.out.println();

        boolean endGame = false;
        int iteration = 1;
        while (!endGame) {

            System.out.println("[ITERATION " + iteration++ + "]");
            System.out.println("[BSM STATE] " + bsmState);
            System.out.println("[PNSM STATE] " + pnsmState);
            System.out.println("[PNSM ON BASES] " + pnsm.net.onBases);
            //PropNetStateMachine.printTopographicProps(pnsm.net);

            ListMultimap<Dob,Dob> bsmActions = bsm.getActions(bsmState);
            ListMultimap<Dob,Dob> pnsmActions = pnsm.getActions(pnsmState);

            List<Dob> bsmRoles = Lists.newArrayList(bsmActions.keySet());
            List<Dob> pnsmRoles = Lists.newArrayList(pnsmActions.keySet());

            Map<Dob,Dob> matchedRoles = matchDobList(bsmRoles, pnsmRoles);


            assertTrue("mismatch between roles!", matchedRoles.keySet().size() == bsmRoles.size());

            Map<Dob,Dob> pickedBSMActions = Maps.newHashMap();
            Map<Dob,Dob> pickedPNSMActions = Maps.newHashMap();

            for (Dob role : bsmRoles) {

                List<Dob> bsmRoleActions = bsmActions.get(role);
                List<Dob> pnsmRoleActions = pnsmActions.get(matchedRoles.get(role));

                Map<Dob,Dob> matchedActions = matchDobList(bsmRoleActions, pnsmRoleActions);

                assertTrue("mismatch between actions for role" + role, matchedActions.keySet().size() ==
                                                                        bsmRoleActions.size());

                int actionId = rand.nextInt(bsmRoleActions.size());
                Dob bsmRoleAction = bsmRoleActions.get(actionId);

                // synchronized actions
                pickedBSMActions.put(role, bsmRoleAction);
                pickedPNSMActions.put(matchedRoles.get(role), matchedActions.get(bsmRoleAction));

                System.out.println("[BSM ACTION] " + bsmRoleAction);
                System.out.println("[PNSM ACTION] " + matchedActions.get(bsmRoleAction));
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


            System.out.println("[PNSM STATE AFTER ACTION] " + pnsm.extractState());

            System.out.println();
        }
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
