package player.uct;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import rekkura.logic.model.Dob;
import rekkura.state.model.StateMachine;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the dropping of depth charges and updating the caches.
 * Also used to select the best move.
 */
class UCTCharger {
    public Map<Dob, UCTCache> actionCaches;
    public Map<Set<Dob>, Integer> sharedStateCache;

    // history stacks
    public Deque<Set<Dob>> stateHistory = Lists.newLinkedList(); // state hashes
    public Deque<Map<Dob, Dob>> moveHistory = Lists.newLinkedList();

    public UCTCharger(List<Dob> roles) {
        sharedStateCache = Maps.newHashMap();
        actionCaches = Maps.newHashMap();
        for (Dob role : roles)
            actionCaches.put(role, new UCTCache(sharedStateCache));
    }

    public Dob bestMove(Dob role, Set<Dob> state, List<Dob> candidateActions) {
        Preconditions.checkArgument(actionCaches.containsKey(role));
        return actionCaches.get(role).bestAction(state, candidateActions);
    }

    public void clearHistory() {
        stateHistory.clear();
        moveHistory.clear();
    }

    public void fireAndReel(Set<Dob> state, StateMachine<Set<Dob>, Dob> machine) {
        // clear in case of leftover
        clearHistory();
        Set<Dob> terminal = fire(state, machine);

        Preconditions.checkArgument(machine.isTerminal(terminal));
        
        // figure out the goals
        Map<Dob,Integer> goals = machine.getGoals(terminal);

        reel(goals);
    }

    public void incrementStateCount(Set<Dob> state) {
        // Update state cache
        int stateCount = 1;
        if (sharedStateCache.containsKey(state))
            stateCount += sharedStateCache.get(state);
        sharedStateCache.put(state, stateCount);
    }
    
    // returns the terminal state
    public Set<Dob> fire(Set<Dob> state, StateMachine<Set<Dob>, Dob> machine) {
        // do actual player.uct charge
        while (!machine.isTerminal(state)) {

            ListMultimap<Dob, Dob> candidateActions = machine.getActions(state);
            Map<Dob, Dob> jointActions = Maps.newHashMap();
            
            // for each role, pick the best player.uct move
            for (Dob role : candidateActions.keySet()) {
                Preconditions.checkArgument(actionCaches.containsKey(role));
                UCTCache uctCache = actionCaches.get(role);
                Dob nextAction = uctCache.nextAction(state, candidateActions.get(role));
                jointActions.put(role, nextAction);
            }
            
            // add to history
            stateHistory.push(state);
            moveHistory.push(jointActions);

            // advance state
            state = machine.nextState(state, jointActions);
        }
        Preconditions.checkArgument(machine.isTerminal(state));
        return state;
    }

    // updates the caches on the way up
    private void reel(Map<Dob,Integer> goals) {
    	double discMultiplier = 1.0;
        // update the state caches by going backward up history
        Preconditions.checkArgument(stateHistory.size() == moveHistory.size());
        while (!stateHistory.isEmpty()) {
            Set<Dob> curState = stateHistory.pop();
            Map<Dob,Dob> curJointMoves = moveHistory.pop();

            // update state count once
            incrementStateCount(curState);
            
            // update move count for each role
            for (Dob role : curJointMoves.keySet()) {
                Dob action = curJointMoves.get(role);
                double discountedGoal = discMultiplier * goals.get(role);
                UCTCache cache = actionCaches.get(role);
                
                cache.updateTransitionCaches(curState, action, discountedGoal);
            }

            // discount the farther away we are
            discMultiplier *= UCTStatics.discFactor;
        }
    }
}