package player.uct;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * User: david
 * Date: 6/26/13
 * Time: 10:32 AM
 * Description:
 *      Template class for a multithreaded UCT player.
 *
 *      Warning: Basically all the params are set
 *          when <code>constructMachine()</code> is called.
 *          It's basically acting like the constructor.
 *
 */
public abstract class UCTPlayer extends Player.StateBased<GgpStateMachine> {

    public ConfigInterface configUCT;
    public String tag;

    // General params
    public int numThreads;
    public int fuzz;
    public Level logLevel;

    // Depth chargers
    public List<Charger> chargers;
    public List<Future<GgpStateMachine>> machinesCharger; // can be async
    public List<Pool> poolsCharger;

    public Pool accumPool;
    public Charger accum;

    // Taskmaster
    public ExecutorService chargeManager; // initialized in prepare()
                                          // shutdownNow() should be called in reflect

    // Statistics
    public volatile long chargeCount = 0;
    public long cacheSizeState = 0;
    public long cacheSizeMove = 0;

    public abstract ConfigInterface buildConfig(List<Rule> rules);

    @Override
    protected GgpStateMachine constructMachine(Collection<Rule> rules) {
        configUCT = buildConfig(Lists.newArrayList(rules));

        // Initialize all the params! (except main machine)
        tag = configUCT.getTag();
        numThreads = configUCT.numThreads();
        fuzz = configUCT.getFuzz();
        logLevel = configUCT.getLoggingLevel();

        // init masters
        accumPool = configUCT.getAccumulatorPool();
        accum = configUCT.getAccumulator();

        // init slave chargers
        chargers = configUCT.createChargers();
        machinesCharger = configUCT.createChargeMachines();
        poolsCharger = configUCT.createChargePools();

        return configUCT.createMainMachine();
    }

    @Override
    protected void prepare() {
        chargeManager = numThreads == 1 ? Executors.newSingleThreadExecutor() :
                Executors.newFixedThreadPool(numThreads);

        System.out.println("In prepare");

        // check everything is initialized
        Preconditions.checkNotNull(accumPool);
        Preconditions.checkNotNull(accum);

        Preconditions.checkNotNull(chargers);
        Preconditions.checkNotNull(machinesCharger);
        Preconditions.checkNotNull(poolsCharger);

        // make sure the role is in here
        this.role = accumPool.dobs.submerge(role);

        // startclock is metagame time + first time playclock time
        long timeLimit = config.startclock - fuzz;
        explore(timeLimit);
    }

    @Override
    protected final void move() {
        explore(config.playclock - fuzz);
    }

    protected void printStats() {
        String prefix = "\t";
        System.out.println();
        System.out.println(prefix + "=== " + tag + " Performance Stats " + " ===");
        int turn = getTurn().turn;
        if (turn == 0)
            System.out.println("Error: No statistics available!");
        System.out.println(prefix + "\tAverage Charges Per Turn: " + (((double) chargeCount) / turn));
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


    // duration has fuzz subtracted already
    public final void explore(long duration) {
        long startTime = System.currentTimeMillis();
        setDecision(anyDecision());
        String threadPrefix = "Thread: " + Thread.currentThread().getId() + " ";
        if (logLevel == Level.INFO) {
            System.out.println(threadPrefix + "Turn: " + getTurn().turn);
            System.out.println(threadPrefix + "Init Decision: " + getDecision(getTurn().turn));
        }


        Game.Turn current = getTurn();
        Set<Dob> state = current.state;
        List<Dob> candidateActions = machine.getActions(state).get(role);

        // reset accumulator
        accum.clear();

        // setup tasks
        long chargeDuration = duration;
        long chargeStop = System.currentTimeMillis() + chargeDuration;
        List<Callable<Void>> chargeTasks = Lists.newArrayList();

        List<GgpStateMachine> machinesUse = Lists.newArrayList();
        List<Charger> chargersUse = Lists.newArrayList();
        List<Pool> poolsUse = Lists.newArrayList();

        // get the first numThreads machines for charging
        for (int i=0; i<machinesCharger.size(); i++) {
            if (machinesUse.size() == numThreads)
                break;
            Future<GgpStateMachine> machineFuture = machinesCharger.get(i);
            if (!machineFuture.isCancelled() && machineFuture.isDone()) {
                try {
                    machinesUse.add(machineFuture.get());
                    chargersUse.add(chargers.get(i));
                    poolsUse.add(poolsCharger.get(i));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        // build the tasks
        for (int i=0; i<numThreads; i++) {
            // translate the state
            Set<Dob> submergedState = Sets.newHashSet();
            for (Dob truth : state)
                submergedState.add(poolsUse.get(i).dobs.submerge(truth));
            chargeTasks.add(buildChargeTask(chargersUse.get(i),
                    submergedState, machinesUse.get(i), chargeStop));
        }

        // launch them
        try {
            List<Future<Void>> results = chargeManager.invokeAll(chargeTasks, chargeDuration, TimeUnit.MILLISECONDS);

            // join
            for (Future<Void> result : results)
                if (!result.isCancelled() && result.isDone())
                    try {
                        result.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long actualChargerStop = System.currentTimeMillis();

        // accumulate!
        accumForRoleState(chargersUse,
                poolsUse,
                state,
                candidateActions);
//        long accumStop = actualChargerStop + accumDuration;
//        try {
//            chargeManager.invokeAll(Lists.newArrayList(
//                    buildAccumulateTask(accum,
//                                        accumPool,
//                                        chargersUse,
//                                        poolsUse,
//                                        accumStop,
//                                        state,
//                                        candidateActions)),
//                    accumDuration, TimeUnit.MILLISECONDS);
//            // pick the best move
//            synchronized (accum) {
//                Dob selected = accum.bestMove(role, state, candidateActions);
//                setDecision(current.turn, selected);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        long actualAccumStop = System.currentTimeMillis();

        // POTENTIALLY PRUNE HERE

        if (logLevel == Level.INFO) {
            System.out.println(threadPrefix + " Fuzz: " + fuzz);
            System.out.println(threadPrefix + " State: " + state);
            System.out.println(threadPrefix + " Decision: " + getDecision(current.turn));
            System.out.println(threadPrefix + " Total decision time: " + (actualAccumStop - startTime) + " ms to decide out of " + config.playclock);
            System.out.println(threadPrefix + " Charger time: " + (actualChargerStop - startTime) + " ms");
            System.out.println(threadPrefix + " Charger time allotted: " + chargeDuration);
            System.out.println(threadPrefix + " Accum time: " + (actualAccumStop - actualChargerStop) + " ms");
            accum.printActionStats(role, state, candidateActions);
        }
        updateStats();

//        // print out the selected move
//        if (logLevel == Level.INFO) {
//            Caches roleCache = accum.actionCaches.get(role);
//            StateActionPair pair = new StateActionPair(state, selected);
//            if (roleCache.explored(state, selected)) {
//                System.out.println(tag + " [ Thread: " + Thread.currentThread().getId() + " Role " + role + "] picked move " + selected +
//                        " with monte carlo goal score: " + roleCache.monteCarloScore(pair));
//            }
//            else
//                System.out.println(tag + " [ Thread: " + Thread.currentThread().getId() + " Role " + role + "] no charges completed. picking random move.");
//            System.out.println();
//        }
    }

    public Callable<Void> buildChargeTask(final Charger charger,
                                          final Set<Dob> state,
                                          final GgpStateMachine machine,
                                          final long stopTime) {
       return new Callable<Void>() {
           @Override
           public Void call() throws Exception {
               while(System.currentTimeMillis() < stopTime) {
                   chargeCount++;
                   synchronized (charger) {
                           charger.fireAndReel(state, machine);
                   }
               }
               return null;
           }
       };
    }

    // no time limit
    public void accumForRoleState(List<Charger> chargersToAdd,
                                  List<Pool> poolsToAdd,
                                  Set<Dob> state,
                                  List<Dob> actions ) {
        synchronized (accum) {
            for (int i=0; i<chargersToAdd.size(); i++) {
                Charger toAdd = chargersToAdd.get(i);
                Pool toAddPool = poolsToAdd.get(i);
                Dob toAddRole = toAddPool.dobs.submerge(role);
                Set<Dob> toAddState = Sets.newHashSet(toAddPool.dobs.submerge(state));
                List<Dob> toAddActions = toAddPool.dobs.submerge(actions);
                synchronized (toAdd) {
                     Charger.accumForRoleState(accum,
                            toAdd,
                            accumPool,
                            toAddRole,
                            toAddState,
                            toAddActions);
                }
            }
        }
    }


    /**
     * Accumulates the results across each of the chargers.
     * Since accumulation can take a bit of time, the caches associated with
     * the player's role are processed first.
     *
     * @param accum
     * @param chargersToAdd
     * @param poolToAdd
     * @param stopTime
     * @return
     */
    public Callable<Charger> buildAccumulateTask(final Charger accum,
                                                 final Pool accumPool,
                                                 final List<Charger> chargersToAdd,
                                                 final List<Pool> poolToAdd,
                                                 final long stopTime,
                                                 final Set<Dob> state,
                                                 final List<Dob> actions) {
        return new Callable<Charger>() {
            @Override
            public Charger call() throws Exception {
                synchronized (accum) {
                    for (int i=0; i<chargersToAdd.size(); i++) {
                        Charger toAdd = chargersToAdd.get(i);
                        Pool toAddPool = poolToAdd.get(i);
                        Dob toAddRole = toAddPool.dobs.submerge(role);
                        Set<Dob> toAddState = Sets.newHashSet(toAddPool.dobs.submerge(state));
                        List<Dob> toAddActions = toAddPool.dobs.submerge(actions);
                        synchronized (toAdd) {
                            boolean timeout = Charger.accumForRoleState(accum,
                                    toAdd,
                                    accumPool,
                                    stopTime,
                                    toAddRole,
                                    toAddState,
                                    toAddActions);
                            if (timeout)
                                return accum;
                        }
                    }
                }

                return accum;
            }
        };
    }

    /**
     * Orders the roles such that first is before any others
     * @param first
     *      role that should go first
     * @param roles
     * @return
     */
    public List<Dob> orderRoles(Dob first, Collection<Dob> roles) {
        List<Dob> rest = Lists.newArrayList(roles);
        rest.remove(first);
        List<Dob> ret = Lists.newArrayList();
        ret.add(first);
        ret.addAll(rest);
        return ret;
    }
}
