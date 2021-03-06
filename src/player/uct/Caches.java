package player.uct;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import rekkura.logic.model.Dob;
import util.MapUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Meant to provide the following caches to a particular role:
 *
 * <ul>
 * <li> Number of times player was in state s and took action a (N(s,a))
 * <li> Monte carlo score for when player is in state s and takes action a.
 *      (Note: this is just the sum of the discounted goals normalized by N(s,a))
 * <li> Number of times a state was visited. <em>This is shared between all the roles</em>
 * </ul>
 *
 */
class Caches {
    public Map<StateActionPair, Double> goalScoreTotal = Maps.newHashMap();
    public Map<StateActionPair, Integer> timesTaken = Maps.newHashMap();
    public Map<Set<Dob>, Integer> timesVisited = Maps.newHashMap();

    // For sharing a single state visited cache among multiple roles
    public Caches(Map<Set<Dob>, Integer> timesVisited) {
        this.timesVisited = timesVisited;
    }

    public void clear() {
        goalScoreTotal.clear();
        timesTaken.clear();
        timesVisited.clear();
    }

    public void printStateActionInfo(Set<Dob> state, Dob action) {
        StateActionPair key = new StateActionPair(state, action);
        if (!goalScoreTotal.containsKey(key)) {
            System.out.println("\t Action: " + action + " Avg Score: " + 0.0 + " Times Taken: " + 0);
            return;
        }
        Preconditions.checkArgument(timesTaken.containsKey(key));
        System.out.println("\tAction: " + action + " Avg Score: " + goalScoreTotal.get(key) / timesTaken.get(key) +
                " Times Taken: " + timesTaken.get(key));
    }

    public void updateTransitionCaches(Set<Dob> state, Dob action, double discountedGoalValue) {
        StateActionPair key = new StateActionPair(state, action);
        MapUtil.incDouble(goalScoreTotal, key, discountedGoalValue);
        MapUtil.incInt(timesTaken, key, 1);
    }

    // taken this action in this state?
    public boolean explored(Set<Dob> state, Dob action) {
    	return timesTaken.containsKey(new StateActionPair(state, action));
    }
    
    public boolean explored(StateActionPair key) {
    	return timesTaken.containsKey(key);
    }
    
    public List<Dob> unexplored(Set<Dob> state, List<Dob> candidateActions) {
    	List<Dob> ret = Lists.newArrayList();
    	for (Dob action : candidateActions) {
    		if (!explored(state, action))
    			ret.add(action);
    	}
    	return ret;
    }
    
    /**
     * Returns the action with the best expected monte carlo score
     * in this state or a random unexplored one if that score is 0
     * @param state
     * @param candidateActions
     * @return
     */
    public Dob bestAction(Set<Dob> state, List<Dob> candidateActions) {
    	Preconditions.checkArgument(!candidateActions.isEmpty());
    	double bestScore = -1.0;
    	List<Dob> bestActions = Lists.newArrayList(
                UCTStatics.randomAction(candidateActions));
    	
    	for (Dob action : candidateActions) {
    		StateActionPair key = new StateActionPair(state, action);
    		// ignore unexplored
    		if (!explored(key))
    			continue;
    		double score = monteCarloScore(key);
    		if (score > bestScore) {
    			bestActions = Lists.newArrayList(action);
    			bestScore = score;
    		} else if (score == bestScore) {
    			bestActions.add(action);
    		}
    	}
   	    return UCTStatics.randomAction(bestActions);
    }
    
    /**
     * Selects an action from the candidates according to the following rule:
     * <ul>
     * <li> If there are unexplored actions, pick a random one of those.
     * <li> Otherwise, pick the action that has the highest UCT score. If there are multiple
     *      of these, pick one at random.
     * </ul>
     * @param state
     *      state we're currently in
     * @param candidateActions
     *      actions that can be considered
     * @return
     */
    public Dob nextAction(Set<Dob> state, List<Dob> candidateActions) {
        Preconditions.checkArgument(!candidateActions.isEmpty());

        // check if there any unexplored
        List<Dob> unexplored = Lists.newArrayList();
        for (Dob action : candidateActions) {
            StateActionPair key = new StateActionPair(state, action);
            if (!timesTaken.containsKey(key))
                unexplored.add(action);
        }

        if (!unexplored.isEmpty())
            return UCTStatics.randomAction(unexplored);

        // if we get here, every action has been explored
        double bestScore = -1.0;
        List<Dob> bestActions = Lists.newArrayList();
        for (Dob action : candidateActions) {
            StateActionPair key = new StateActionPair(state, action);
            double score = uctScore(key);
            if (score > bestScore) {
                bestActions = Lists.newArrayList(action);
                bestScore = score;
            } else if (score == bestScore) {
                bestActions.add(action);
            }
        }
        return UCTStatics.randomAction(bestActions);
    }

    private double uctScore(StateActionPair saPair) {
        return monteCarloScore(saPair) + UCTStatics.C*uctBonus(saPair);
    }

    public double monteCarloScore(StateActionPair saPair) {
        return goalScoreTotal.get(saPair) / timesTaken.get(saPair);
    }

    private double uctBonus(StateActionPair saPair) {
        int timesVisitedState = timesVisited.get(saPair.getState());
        double UCTBonus = Math.sqrt(Math.log(timesVisitedState) / timesTaken.get(saPair));
        return UCTBonus;
    }

    @Override
    public String toString() {
        String prefix = "[UCT Cache]======\n";
        String scoreTotalStr = "\tScore Total: " + goalScoreTotal +  "\n";
        String timesTakenStr = "\tTimes taken: " + timesTaken + "\n";
        String timesVisitedStr = "\tTimes visited: " + timesVisited + "\n";
        return prefix + scoreTotalStr + timesTakenStr + timesVisitedStr;
    }
    
}