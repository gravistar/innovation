package player.uct;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * User: david
 * Date: 6/26/13
 * Time: 10:32 AM
 * Description:
 *      Template class for a multithreaded UCT player
 *
 */
public abstract class UCTPlayer<M extends GgpStateMachine> extends Player.StateBased<M> {
    public boolean verbose = true;
    public boolean fine = true;

    // for setting up the threaded chargers
    public List<Charger> chargers = Lists.newArrayList();
    public List<M> machines = Lists.newArrayList();
    public List<Set<Dob>> chargeStates = Lists.newArrayList();

    public Charger accum;

    // WARNING: needs to be instantiated in rules
    public List<Rule> rules = Lists.newArrayList(); // this is gross, but needed for constructing new machines

    // WARNING: needs to be instantiated in plan
    public List<Dob> roles;

    public static long fuzz = 100; // fuzz threshold in ms

    // stats
    public volatile long chargeCount = 0;
    public long cacheSizeState = 0;
    public long cacheSizeMove = 0;
    public long nTurns = 0;

    // for multithreading
    public abstract int numThreads();

    // SUPER IMPORTANT: instances MUST call shutdownNow() in reflect()
    //    public ThreadPoolExecutor chargeManager = Executors.newFixedThreadPool(numThreads());

    public ExecutorService chargeManager = numThreads() == 1 ? Executors.newSingleThreadExecutor() :
            Executors.newFixedThreadPool(numThreads());

    public abstract String getTag();

    @Override
    protected final void move() {
        System.out.println("MOVING");
        explore(config.playclock - fuzz);
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
        cacheSizeState = accum.sharedStateCache.size();
        cacheSizeMove = 0;
        for (Dob key : accum.actionCaches.keySet())
            cacheSizeMove += accum.actionCaches.get(key).timesTaken.size();
    }

    protected final void plan() {
        // setup the roles
        roles = Lists.newArrayList(machine.getActions(machine.getInitial()).keySet());

        // setup the chargers
        for (int i=0; i<numThreads(); i++)
            chargers.add(new Charger(roles));

        // setup the accumulator
        accum = new Charger(roles);

        // setup the machines
        for (int i=0; i<numThreads(); i++) {
            if (i==0) {
                machines.add(machine);
                continue;
            }
            machines.add(constructMachine(rules));
        }

        // setup the charge states
        for (int i=0; i<numThreads(); i++) {
            chargeStates.add(Sets.<Dob>newHashSet(machines.get(i).getInitial()));
        }

        // setup tasks
        List<Callable<Void>> chargeTasks = Lists.newArrayList();
        for (int i=0; i<numThreads(); i++)
            chargeTasks.add(buildChargeTask(chargers.get(i), chargeStates.get(i), machines.get(i)));

        // launch them
        try {
            chargeManager.invokeAll(chargeTasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // startclock is metagame time + first time playclock time
        long timeLimit = config.startclock - fuzz;
        explore(timeLimit);
    }

    public final void explore(long timeLimit) {
        setDecision(anyDecision());
        Game.Turn current = getTurn();
        Set<Dob> state = current.state;

        System.out.println("Exploring: " + state);

        // copy over to the states
        for (int i=0; i<numThreads(); i++) {
            Set<Dob> chargeState = chargeStates.get(i);
            synchronized (chargeState) {
                chargeState.clear();
                chargeState.addAll(state);
            }
        }

        List<Dob> candidateActions = machine.getActions(state).get(role);
        Dob selected = candidateActions.get(0);

        // reset accumulator
        accum = new Charger(roles);

        System.out.println("Turn: " + nTurns);
        nTurns++;
        try {
            Thread.sleep(timeLimit);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // interrupt and join
        for (int i=0; i<numThreads(); i++) {
            synchronized (chargers.get(i)) {
                Charger.accum(accum, chargers.get(i));
            }
        }

        selected = accum.bestMove(role, state, candidateActions);
        setDecision(current.turn, selected);

        updateStats();

        // print out the selected move
//        if (verbose && fine) {
//            Caches roleCache = accum.actionCaches.get(role);
//            StateActionPair pair = new StateActionPair(state, selected);
//            if (roleCache.explored(state, selected)) {
//                System.out.println(getTag() + " Role " + role + "] picked move " + selected +
//                        " with monte carlo goal score: " + roleCache.monteCarloScore(pair));
//            }
//            else
//                System.out.println(getTag() + " Role " + role + "] no charges completed. picking random move.");
//            System.out.println();
//        }
    }

    // super dumb/unsafe charge task - depends on above to interrupt
    // and pull out the caches
    public Callable<Void> buildChargeTask(final Charger charger,
                                          final Set<Dob> state,
                                          final M machine) {
       return new Callable<Void>() {
           @Override
           public Void call() throws Exception {
               while(true) {
                   chargeCount++;
                   synchronized (charger) {
                       synchronized (state) {
                           long start = System.currentTimeMillis();
                           charger.fireAndReel(state, machine);
                           long end = System.currentTimeMillis();
                           System.out.println("charge time: " + (end - start));
                       }
                   }
               }
           }
       };
    }
}
