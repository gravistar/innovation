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
    public int accumDuration;
    public int nPolls = 3;                                // number of start clock explores

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
    public volatile long turnChargeCount = 0;

    public abstract ConfigInterface buildConfig(List<Rule> rules);

    @Override
    protected GgpStateMachine constructMachine(Collection<Rule> rules) {
        configUCT = buildConfig(Lists.newArrayList(rules));

        // Initialize all the params! (except main machine)
        tag = configUCT.getTag();
        numThreads = configUCT.numThreads();
        fuzz = configUCT.getFuzz();
        logLevel = configUCT.getLoggingLevel();
        accumDuration = configUCT.getAccumDuration();

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

        // check everything is initialized
        Preconditions.checkNotNull(accumPool);
        Preconditions.checkNotNull(accum);

        Preconditions.checkNotNull(chargers);
        Preconditions.checkNotNull(machinesCharger);
        Preconditions.checkNotNull(poolsCharger);

        // make sure accumulator can see our role
        this.role = accumPool.dobs.submerge(role);

        // startclock is metagame time + first time playclock time
        long chargeTime = config.startclock - accumDuration - fuzz;

        // poll at intervals to see if state machine finished
        long duration = chargeTime / nPolls;
        for (int i=0; i<nPolls; i++) {
            // only accumulate the last one
            boolean accum = i == nPolls - 1;
            explore(duration, accum);
        }
    }

    @Override
    protected final void move() {
        long chargeTime = config.playclock - accumDuration - fuzz;
        explore(chargeTime, true);
    }

    // duration has fuzz subtracted already
    public final void explore(long chargeDuration, boolean accumulate) {
        Thread.currentThread().setPriority(UCTStatics.ThreadPriority.MAIN.level);
        turnChargeCount = 0;
        List<Integer> machineIndices = Lists.newArrayList();
        long startTime = System.currentTimeMillis();
        setDecision(anyDecision());
        String threadPrefix = "Thread: " + Thread.currentThread().getId() + " ";
        if (logLevel == Level.INFO) {
            System.out.println(threadPrefix + " Turn: " + getTurn().turn);
            System.out.println(threadPrefix + " Init Decision: " + getDecision(getTurn().turn));
        }

        Game.Turn current = getTurn();
        Set<Dob> state = current.state;
        List<Dob> candidateActions = machine.getActions(state).get(role);

        // reset accumulator
        accum.clear();

        // setup tasks
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
                    machineIndices.add(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        long machineChargerUpdateStop = System.currentTimeMillis();
        long actualChargeDuration = chargeStop - machineChargerUpdateStop;
        long actualChargeStop = machineChargerUpdateStop + actualChargeDuration;

        // build the tasks
        for (int i=0; i<numThreads; i++) {
            // translate the state
            Set<Dob> submergedState = Sets.newHashSet();
            for (Dob truth : state)
                submergedState.add(poolsUse.get(i).dobs.submerge(truth));
            chargeTasks.add(buildChargeTask(chargersUse.get(i),
                    submergedState, machinesUse.get(i), actualChargeStop));
        }

        // launch them
        try {
            List<Future<Void>> results = chargeManager.invokeAll(chargeTasks,
                    actualChargeDuration, TimeUnit.MILLISECONDS);

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

        if (!accumulate)
            return;

        long accumStop = actualChargerStop + accumDuration;

        // accumulate!
        accumForRoleState(chargersUse,
                poolsUse,
                state,
                candidateActions,
                accumStop);

        // set decision!!
        setDecision(current.turn, accum.bestMove(role, state, candidateActions));

        long actualAccumStop = System.currentTimeMillis();

        if (logLevel == Level.INFO) {
            long startClockLimit = ( config.startclock - accumDuration - fuzz ) / nPolls + accumDuration + fuzz;
            long timeLimit = getTurn().turn == 0 ? startClockLimit : config.playclock;
            System.out.println(threadPrefix + " State: " + state);
            System.out.println(threadPrefix + " Charger machines used: " + machineIndices);
            System.out.println(threadPrefix + " Final Decision: " + getDecision(current.turn));
            System.out.println(threadPrefix + " Total decision time: " + (actualAccumStop - startTime) +
                    " ms out of " + timeLimit);
            System.out.println(threadPrefix + " State machine update time: " + (machineChargerUpdateStop - startTime) + " ms");
            System.out.println(threadPrefix + " Charger time: " + (actualChargerStop - machineChargerUpdateStop) +
                    " ms out of " + chargeDuration);
            System.out.println(threadPrefix + " Accum time: " + (actualAccumStop - actualChargerStop) +
                    " ms out of " + accumDuration);
            System.out.println(threadPrefix + " Charges this turn: " + turnChargeCount);
            accum.printActionStats(role, state, candidateActions);
            printCacheSizes(chargersUse, threadPrefix);
        }
        // POTENTIALLY PRUNE CACHES HERE
    }

    public Callable<Void> buildChargeTask(final Charger charger,
                                          final Set<Dob> state,
                                          final GgpStateMachine machine,
                                          final long stopTime) {
       return new Callable<Void>() {
           @Override
           public Void call() throws Exception {
               Thread.currentThread().setPriority(UCTStatics.ThreadPriority.CHARGE.level);
               while(System.currentTimeMillis() < stopTime) {
                   synchronized (charger) {
                           charger.fireAndReel(state, machine, stopTime);
                   }
                   turnChargeCount++;
               }
               return null;
           }
       };
    }

    public void printCacheSizes(List<Charger> chargers, String threadPrefix) {
        int totalSize = 0;
        for (Charger charger : chargers) {
            synchronized (charger) {
                totalSize += charger.sharedStateCache.size();
                for (Dob role : charger.actionCaches.keySet()) {
                    Caches cache = charger.actionCaches.get(role);
                    totalSize += cache.goalScoreTotal.size();
                    totalSize += cache.timesTaken.size();
                }
            }
        }
        System.out.println(threadPrefix + " Total cache entries: " + totalSize);
    }

    // stop time
    public void accumForRoleState(List<Charger> chargersToAdd,
                                  List<Pool> poolsToAdd,
                                  Set<Dob> state,
                                  List<Dob> actions,
                                  long stopTime) {
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
                        stopTime,
                        toAddRole,
                        toAddState,
                        toAddActions);
            }
        }
    }
}
