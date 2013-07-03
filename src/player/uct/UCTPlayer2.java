package player.uct;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * User: david
 * Date: 6/26/13
 * Time: 10:32 AM
 * Description:
 *      Provides the depth charge performing for different uct players (native or vanilla)
 * M: machine type
 */
public abstract class UCTPlayer2<M extends GgpStateMachine> extends Player.StateBased<M> {

    public static Random rand = new Random(System.currentTimeMillis());
    public static final double discFactor = 0.999;
    private static boolean verbose = true;

    private UCTCharger charger;

    public abstract String getTag();

    @Override
    protected final void move() {
        explore();
    }

    @Override
    protected void reflect() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public final void explore() {
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
                System.out.println(getTag() + " Role " + role + "] picked move " + selected +
                        " with monte carlo goal score: " + roleCache.monteCarloScore(pair));
            }
            else
                System.out.println(getTag() + " Role " + role + "] no charges completed. picking random move.");
            System.out.println();
        }
    }
}
