package player.uct;

import com.google.common.collect.Lists;
import rekkura.ggp.milleu.Game;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/2/13
 * Time: 9:50 PM
 *
 * This is gross and should share code with UCT player
 */
public class UCTPropNetPlayer extends PropNetBased{
    public static Random rand = new Random();
    public static final double discFactor = 0.999;
    private static boolean verbose = true;

    private UCTCharger charger;

    public static String getTag() {
        return "[UCT PropNet]";
    }


    @Override
    protected void plan() {
        charger = new UCTCharger(Lists.newArrayList(machine.getActions(machine.getInitial()).keySet()));
        System.out.println(getTag() + "Done building charger!");
        System.out.println(getTag() + "Role: " + role);
        explore();
    }

    @Override
    protected void move() {
        explore();
    }

    @Override
    protected void reflect() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void explore() {
        setDecision(anyDecision());
        Game.Turn current = getTurn();
        Set<Dob> state = current.state;
        List<Dob> candidateActions = machine.getActions(state).get(role);

        int chargeCount = 0;
        Dob selected = candidateActions.get(0);
        StateActionPair pair = new StateActionPair(state, selected);
        UCTCache roleCache = charger.actionCaches.get(role);

        while(validState()) {
            chargeCount++;
            charger.fireAndReel(state, machine);
            selected = charger.bestMove(role, state, candidateActions);
            setDecision(current.turn, selected);
        }
        if (verbose) {
            System.out.println(getTag() + " Charge count: " + chargeCount);
            System.out.println(getTag() + " State cache size: " + charger.sharedStateCache.size());
            if (roleCache.explored(state, selected)) {
                System.out.println(getTag() + " Role " + role + "] picked move " + selected + " with monte carlo goal score: " + roleCache.monteCarloScore(pair));
            }
            else
                System.out.println(getTag() + " Role " + role + "] no charges completed. picking random move.");
            System.out.println();
        }
    }
}
