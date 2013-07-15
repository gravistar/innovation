package player.uct;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import rekkura.logic.model.Dob;
import rekkura.logic.structure.Pool;
import rekkura.state.model.StateMachine;
import util.MapUtil;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the dropping of depth charges and updating the caches.
 * Also used to select the best move.
 */
class Charger {
    public boolean verbose = false;
    public Map<Dob, Caches> actionCaches;
    public Map<Set<Dob>, Integer> sharedStateCache;

    // history stacks
    public Deque<Set<Dob>> stateHistory = Lists.newLinkedList(); // state hashes
    public Deque<Map<Dob, Dob>> moveHistory = Lists.newLinkedList();

    public Charger(List<Dob> roles) {
        sharedStateCache = Maps.newHashMap();
        actionCaches = Maps.newHashMap();
        for (Dob role : roles)
            actionCaches.put(role, new Caches(sharedStateCache));
    }

    public void clear() {
        sharedStateCache.clear();
        for (Dob role : actionCaches.keySet())
            actionCaches.get(role).clear();
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

        if (verbose)
            System.out.println("[Charge Goals]: " + goals);

        reel(goals);
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
                Caches uctCache = actionCaches.get(role);
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
            MapUtil.incInt(sharedStateCache, curState, 1);

            // update move count for each role
            for (Dob role : curJointMoves.keySet()) {
                Dob action = curJointMoves.get(role);
                double discountedGoal = discMultiplier * goals.get(role);
                Caches cache = actionCaches.get(role);
                
                cache.updateTransitionCaches(curState, action, discountedGoal);
            }

            // discount the farther away we are
            discMultiplier *= UCTStatics.discFactor;
        }
    }

    public void printActionStats(Dob role, Set<Dob> state, List<Dob> actions) {
        System.out.println("Printing Action Stats: ");
        for (Dob action : actions) {
            actionCaches.get(role).printStateActionInfo(state, action);
        }
        System.out.println();
    }

    /**
     * For accum without time limit
     * @param accum
     * @param toAdd
     * @param accumPool
     * @param role
     *      must be in toAdd's pool
     * @param state
     *      must be in toAdd's pool
     * @param actions
     *      must be in toAdd's pool
     */
    public static void accumForRoleState(Charger accum,
                                         Charger toAdd,
                                         Pool accumPool,
                                         Dob role,
                                         Set<Dob> state,
                                         List<Dob> actions) {
        accumForRoleState(accum,
                          toAdd,
                          accumPool,
                          UCTStatics.forbiddenTimeout,
                          role,
                          state,
                          actions
                       );
    }

    /**
     *
     * @param accum
     * @param toAdd
     * @param accumPool
     * @param stopTime
     * @param role
     *      must be in toAdd's pool
     * @param state
     *      must be in toAdd's pool
     * @param actions
     *      must be in toAdd's pool
     * @return
     */
    public static boolean accumForRoleState(Charger accum,
                                           Charger toAdd,
                                           Pool accumPool,
                                           long stopTime,
                                           Dob role,
                                           Set<Dob> state,
                                           List<Dob> actions) {
        Preconditions.checkArgument(toAdd.actionCaches.containsKey(role));
        Dob accumRole = accumPool.dobs.submerge(role);
        Set<Dob> accumState = Sets.newHashSet(accumPool.dobs.submerge(state));

        // find caches for our role
        Caches addTo = accum.actionCaches.get(accumRole);
        Caches addFrom = toAdd.actionCaches.get(role);

        for (Dob action : actions) {
            StateActionPair sa = new StateActionPair(state, action);
            if (!addFrom.timesTaken.containsKey(sa) || !addFrom.goalScoreTotal.containsKey(sa))
                continue;

            // make accum key
            Dob accumAction = accumPool.dobs.submerge(action);
            StateActionPair accumSa = new StateActionPair(accumState, accumAction);

            // increment
            MapUtil.incDouble(addTo.goalScoreTotal,
                                accumSa,
                                addFrom.goalScoreTotal.get(sa));
            MapUtil.incInt(addTo.timesTaken,
                                accumSa,
                                addFrom.timesTaken.get(sa));

            if (stopTime != UCTStatics.forbiddenTimeout && System.currentTimeMillis() > stopTime)
                return true;
        }
        return false;
    }

    /**
     *
     * @param accum
     * @param toAdd
     * @param stopTime
     * @param role
     *      role to examine. must be in toAdd's pool
     * @return
     *      true if there's timeout, false otherwise
     */
    public static boolean accumForRole(Charger accum,
                                       Charger toAdd,
                                       Pool accumPool,
                                       long stopTime,
                                       Dob role) {

        Preconditions.checkArgument(toAdd.actionCaches.containsKey(role));
        Dob submergedRole = accumPool.dobs.submerge(role);

        if (!accum.actionCaches.containsKey(submergedRole))
            accum.actionCaches.put(submergedRole,
                    new Caches(accum.sharedStateCache));

        Caches addTo = accum.actionCaches.get(submergedRole);
        Caches addFrom = toAdd.actionCaches.get(submergedRole);

        // accumulate the state-action pair caches
        for (StateActionPair sa : addFrom.goalScoreTotal.keySet()) {
            // only add ones for which we have both
            if (!addFrom.timesTaken.containsKey(sa))
                continue;
            StateActionPair submergedSa = new StateActionPair(Sets.newHashSet(accumPool.dobs.submerge(sa.getState())),
                    accumPool.dobs.submerge(sa.getAction()));
            MapUtil.incDouble(addTo.goalScoreTotal, submergedSa,
                    addFrom.goalScoreTotal.get(sa));
            MapUtil.incInt(addTo.timesTaken, submergedSa,
                    addFrom.timesTaken.get(sa));;
            if (System.currentTimeMillis() > stopTime)
                return true;
        }
        return false;
    }

    // pool is needed for translation
    // roleOrder should be dobs in toAdd's corresponding pool
    // returns true if there's a timeout, false otherwise
    public static boolean accum(Charger accum,
                             Charger toAdd,
                             Pool accumPool,
                             long stopTime,
                             List<Dob> roleOrder) {

        // accumulate the state caches
        for (Set<Dob> state : toAdd.sharedStateCache.keySet()) {
            Set<Dob> submergedState = Sets.newHashSet(accumPool.dobs.submerge(state));
            MapUtil.incInt(accum.sharedStateCache, submergedState,
                    toAdd.sharedStateCache.get(state));
            if (System.currentTimeMillis() > stopTime)
                return true;
        }

        for (Dob role : roleOrder){
            Preconditions.checkArgument(toAdd.actionCaches.keySet().contains(role));
            boolean timeout = accumForRole(accum, toAdd, accumPool, stopTime, role);
            if (timeout)
                return true;
        }
        return false;
    }
}