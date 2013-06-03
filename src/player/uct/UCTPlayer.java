package player.uct;

import com.google.common.collect.Lists;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/18/13
 * Time: 11:09 PM
 *
 * This is a first stab at a UCT player. It will use the prover state machine first.
 * TODO: the UCT state caches and the UCT fire should be genericized.
 *
 *
 * For games of different sizes, I want to see how many UCT charges per second can be
 * dropped using the prover state machine.  I also want to see the confidence of the
 * moves selected for each of these different games.
 */
public class UCTPlayer extends Player.ProverBased{

    public static Random rand = new Random();
    public static final double discFactor = 0.999;
    private static boolean verbose = true;
    private static boolean fine = false;
    
    // gross
    private UCTCharger charger;

    @Override
    protected void plan() {
    	charger = new UCTCharger(Lists.newArrayList(machine.getActions(machine.getInitial()).keySet()));
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

    private void explore() {
        setDecision(anyDecision());

        Game.Turn current = getTurn();
        Set<Dob> state = current.state;
        List<Dob> candidateActions = machine.getActions(state).get(role);
        
        int chargeCount = 0;
        Dob selected = candidateActions.get(0);
        StateActionPair pair = new StateActionPair(state, selected);
    	UCTCache roleCache = charger.actionCaches.get(role);
    	
    	// drop charges
        while(validState()) {
        	chargeCount++;
        	charger.fireAndReel(state, machine);
        	selected = charger.bestMove(role, state, candidateActions);
            setDecision(current.turn, selected);
            if (fine){
            	pair = new StateActionPair(state, selected);
            	
            	System.out.println("[UCT Role: " + role + "] FIRING CHARGE " + chargeCount + "==============");
             	
            	if (roleCache.explored(state, selected))
            		System.out.println("[UCT Role: " + role + "] Charge picked move " + selected + " with monte carlo score: " + roleCache.monteCarloScore(pair));
            	else
            		System.out.println("[UCT Role: " + role + "] ERROR: picking unexplored move");
            	System.out.println();
            }
        }

        if (verbose) {
            if (roleCache.explored(state, selected)) {
                System.out.println("[UCT Charge Count: " + chargeCount + "]");
        	    System.out.println("[UCT Role: " + role + "] picked move " + selected + " with monte carlo goal score: " + roleCache.monteCarloScore(pair));
            }
            else
        	    System.out.println("[UCT Role: " + role + "] no charges completed. picking random move.");
            System.out.println();
        }
    }
}
