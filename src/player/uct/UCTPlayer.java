package player.uct;

import com.google.common.collect.Lists;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: david
 * Date: 6/26/13
 * Time: 10:32 AM
 * Description:
 *      Provides the depth charge performing for different uct players (native or vanilla)
 *      M: machine type
 */
public abstract class UCTPlayer<M extends GgpStateMachine> extends Player.StateBased<M> {
    public boolean verbose = true;
    public boolean fine = false;

    public UCTCharger charger;
    public volatile long chargeCount = 0;
    public long cacheSizeState = 0;
    public long cacheSizeMove = 0;
    public long nTurns = 0;

    public abstract String getTag();

    @Override
    protected final void move() {
        explore();
    }

    protected void printStats() {
        String prefix = "\t";
        System.out.println();
        System.out.println(prefix + "=== " + getTag() + " Performance Stats " + " ===");
        if (nTurns == 0)
            System.out.println("Error: No statistics available!");
        System.out.println(prefix + "\tAverage Charges Per Turn: " + (((double) chargeCount) / nTurns));
        System.out.println(prefix + "\tDistinct States Visited: " + cacheSizeState);
        System.out.println(prefix + "\tDistinct Move/State Pairs: " + cacheSizeMove);
        System.out.println();
    }

    private void updateStats() {
        chargeCount++;
        cacheSizeState = charger.sharedStateCache.size();
        cacheSizeMove = 0;
        for (Dob key : charger.actionCaches.keySet())
            cacheSizeMove += charger.actionCaches.get(key).timesTaken.size();
    }

    protected final void plan() {
        charger = new UCTCharger(Lists.newArrayList(machine.getActions(machine.getInitial()).keySet()));
        explore();
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

        nTurns++;

        while(validState()) {
            chargeCount++;
            charger.fireAndReel(state, machine);
            selected = charger.bestMove(role, state, candidateActions);
            setDecision(current.turn, selected);
            updateStats();
        }
        // end threads
        if (verbose && fine) {
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
