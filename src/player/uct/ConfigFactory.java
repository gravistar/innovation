package player.uct;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import machina.PropNetStateMachine;
import propnet.PropNetInterface;
import propnet.nativecode.NativePropNetFactory;
import propnet.util.PropNetUtil;
import propnet.util.Tuple2;
import propnet.vanilla.PropNet;
import propnet.vanilla.PropNetFactory;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * User: david
 * Date: 7/13/13
 * Time: 12:45 PM
 * Description:
 *      Builds different UCT player configurations
 */
public class ConfigFactory {
    // PRESET CONFIGS
    public static ConfigInterface singleProverConfig(final List<Rule> rules,
                                                     final ExecutorService buildManager) {
        int numThreads = 1;
        KaskadeLevel level = KaskadeLevel.PROVER;
        String tag = getTag(numThreads, level);
        return kaskadeConfig(rules,
                buildManager,
                tag,
                UCTStatics.fuzzRisky,
                Level.INFO,
                numThreads,
                UCTStatics.accumShort,
                level);
    }

    public static ConfigInterface singlePropNetVanillaConfig(final List<Rule> rules,
                                                             final ExecutorService buildManager) {
        int numThreads = 1;
        KaskadeLevel level = KaskadeLevel.VANILLA;
        String tag = getTag(numThreads, level);
        return kaskadeConfig(rules,
                buildManager,
                tag,
                UCTStatics.fuzzRisky,
                Level.INFO,
                numThreads,
                UCTStatics.accumShort,
                level);
    }

    public static ConfigInterface singlePropNetNativeConfig(final List<Rule> rules,
                                                            final ExecutorService buildManager) {
        int numThreads = 1;
        KaskadeLevel level = KaskadeLevel.NATIVE;
        String tag = getTag(numThreads, level);
        return kaskadeConfig(rules,
                buildManager,
                tag,
                UCTStatics.fuzzRisky,
                Level.INFO,
                numThreads,
                UCTStatics.accumShort,
                level);
    }

    // PRODUCTION CONFIG
    public static ConfigInterface fullKaskadeConfig(final List<Rule> rules,
                                                    final ExecutorService buildManager) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        KaskadeLevel level = KaskadeLevel.NATIVE;
        String tag = getTag(numThreads, level);
        return kaskadeConfig(rules,
                buildManager,
                tag,
                UCTStatics.fuzzRisky,
                Level.INFO,
                numThreads,
                UCTStatics.accumProduction,
                level);
    }

    // How much to kaskade??
    public static enum KaskadeLevel {
        PROVER(1),
        VANILLA(2),
        NATIVE(3);

        public final int id;

        KaskadeLevel(int id) {
            this.id = id;
        }
    }

    public static String getTag(int numThreads, KaskadeLevel level) {
        String type = "";
        if (level == KaskadeLevel.PROVER)
            type = "Prover";
        if (level == KaskadeLevel.VANILLA)
            type = "PropNet Vanilla";
        if (level == KaskadeLevel.NATIVE)
            type = "PropNet Native";

        return "[UCT " + type + " " + numThreads + " Threads]";
    }

    // builds the vanilla propnet
    public static Callable<Tuple2<PropNet, GameLogicContext>> vanillaBuildTask(
            final List<Rule> rules) {
        return new Callable<Tuple2<PropNet, GameLogicContext>>() {
            @Override
            public Tuple2<PropNet, GameLogicContext> call() throws Exception {
                Thread.currentThread().setPriority(UCTStatics.ThreadPriority.BUILD.level);
                Tuple2<PropNet, GameLogicContext> ret = PropNetFactory.createFromRules(rules);
                return ret;
            }
        };
    }

    // compiles a vanilla propnet to a c source file
    // depends on the vanilla build task finishing first
    public static Callable<NativePropNetFactory.NativeParam> nativeBuildTask(
            final Future<Tuple2<PropNet, GameLogicContext>> vanillaFuture) {
         return new Callable<NativePropNetFactory.NativeParam>() {
                    @Override
                    public NativePropNetFactory.NativeParam call() throws Exception {
                        Tuple2<PropNet, GameLogicContext> vanillaNetOut = vanillaFuture.get();
                        Thread.currentThread().setPriority(UCTStatics.ThreadPriority.BUILD.level);
                        // gross
                        PropNet vanilla = vanillaNetOut._1;
                        NativePropNetFactory.NativeParam nativeParams = NativePropNetFactory.compile(vanilla);
                        return nativeParams;
                    }
                };
    }

    // copying the propnet can take a while, so launch it as an async task during builder
    public static Callable<GgpStateMachine> copyPropNetVanillaBuildTask(
            final Future<Tuple2<PropNet, GameLogicContext>> vanillaPrototypeFuture,
            final Pool copyPool,
            final GameLogicContext copyContext) {

        return new Callable<GgpStateMachine>() {
            @Override
            public GgpStateMachine call() throws Exception {
                Tuple2<PropNet, GameLogicContext> vanillaPrototype = vanillaPrototypeFuture.get();
                Thread.currentThread().setPriority(UCTStatics.ThreadPriority.COPY.level);
                // read only usage of vanilla
                PropNet toCopy = vanillaPrototype._1;
                PropNet copy = PropNetUtil.copyPropNetVanilla(toCopy, copyPool);
                return PropNetStateMachine.create(new Tuple2<PropNetInterface, GameLogicContext>(copy, copyContext));
            }
        };
    }

    public static Callable<GgpStateMachine> copyPropNetNativeBuildTask(
            final Future<NativePropNetFactory.NativeParam> nativeParamFuture,
            final Pool copyPool,
            final GameLogicContext copyContext) {
        return new Callable<GgpStateMachine>() {
            @Override
            public GgpStateMachine call() throws Exception {
                Thread.currentThread().setPriority(UCTStatics.ThreadPriority.COPY.level);

                // make a next game logic context..
                NativePropNetFactory.NativeParam nativeParam = nativeParamFuture.get();

                // update this to use submerged indices
                Map<Dob,Integer> copyIndices = Maps.newHashMap();
                Map<Dob,Integer> toCopy = nativeParam.propIndices;
                for (Dob key : toCopy.keySet())
                    copyIndices.put(copyPool.dobs.submerge(key), toCopy.get(key));

                // for the love of god make a new param
                NativePropNetFactory.NativeParam nativeParamCopy =
                        new NativePropNetFactory.NativeParam(nativeParam.fullClassName,
                                nativeParam.size,
                                copyIndices);

                // pnsm params
                Tuple2<PropNetInterface, GameLogicContext> pnsmParam =
                        new Tuple2<PropNetInterface, GameLogicContext>
                                (NativePropNetFactory.getCompiledNet(nativeParamCopy), copyContext);

                return PropNetStateMachine.create(pnsmParam);
            }
        };
    }

    // <3 kaskade
    public static ConfigInterface kaskadeConfig(final List<Rule> rules,
                                                final ExecutorService buildManager,
                                                final String tag,
                                                final int fuzz,
                                                final Level logLevel,
                                                final int numThreads,
                                                final int accumDuration,
                                                final KaskadeLevel kaskadeLevel) {

        // create the main
        final BackwardStateMachine machineMain = BackwardStateMachine.createForRules(rules);
        final List<Dob> rolesMain = Lists.newArrayList(machineMain.getActions(machineMain.getInitial()).keySet());

        // create the chargers
        final List<Future<GgpStateMachine>> machinesCharger = Lists.newArrayList();
        final List<Charger> chargers = Lists.newArrayList();
        final List<Pool> poolsCharger = Lists.newArrayList();

        // BUILD THE STUFF!!
        if (kaskadeLevel.id >= KaskadeLevel.PROVER.id) {
            // create provers
            for (int i=0; i<numThreads; i++) {
                final BackwardStateMachine machineCharger = BackwardStateMachine.createForRules(rules);
                final Pool poolCharger = machineCharger.prover.pool;
                final List<Dob> rolesCharger = Lists.newArrayList(
                        machineCharger.getActions(machineCharger.getInitial()).keySet());
                // add trivial futures
                machinesCharger.add(new Future<GgpStateMachine>() {
                    @Override
                    public boolean cancel(boolean b) {
                        return false;
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }

                    @Override
                    public boolean isDone() {
                        return true;
                    }

                    @Override
                    public GgpStateMachine get() throws InterruptedException, ExecutionException {
                        return machineCharger;
                    }


                    @Override
                    public GgpStateMachine get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                        return machineCharger;
                    }
                });
                poolsCharger.add(poolCharger);
                chargers.add(new Charger(rolesCharger));
            }
        }

        // VANILLAS
        if (kaskadeLevel.id >= KaskadeLevel.VANILLA.id){
            final Future<Tuple2<PropNet, GameLogicContext>> vanillaFuture = buildManager.submit(
                    vanillaBuildTask(rules));

            // create vanillas
            for (int i=0; i<numThreads; i++) {
                // make a new context, copy the vanilla net
                BackwardStateMachine copyContext = BackwardStateMachine.createForRules(rules);
                Pool copyPool = copyContext.prover.pool;

                // get the roles
                List<Dob> copyRoles = Lists.newArrayList(copyContext.getActions(copyContext.getInitial()).keySet());

                // submit the copy tasks
                Future<GgpStateMachine> copyFuture = buildManager.submit(
                        copyPropNetVanillaBuildTask(vanillaFuture, copyPool, copyContext));

                // add the stuff
                machinesCharger.add(copyFuture);
                poolsCharger.add(copyPool);
                chargers.add(new Charger(copyRoles));
            }

            // NATIVES
            if (kaskadeLevel.id >= KaskadeLevel.NATIVE.id) {
                final Future<NativePropNetFactory.NativeParam> nativeParamFuture = buildManager.submit(
                        nativeBuildTask(vanillaFuture));

                // create natives
                for (int i=0; i<numThreads; i++) {
                    // make a new context, copy the vanilla net
                    BackwardStateMachine copyContext = BackwardStateMachine.createForRules(rules);
                    Pool copyPool = copyContext.prover.pool;

                    // get the roles
                    List<Dob> copyRoles = Lists.newArrayList(copyContext.getActions(copyContext.getInitial()).keySet());

                    // submit the copy task
                    Future<GgpStateMachine> copyFuture = buildManager.submit(
                            copyPropNetNativeBuildTask(nativeParamFuture, copyPool, copyContext));

                    // add the stuff
                    machinesCharger.add(copyFuture);
                    poolsCharger.add(copyPool);
                    chargers.add(new Charger(copyRoles));
                }
            }
        }

        // make the actual config
        return new ConfigInterface() {
            @Override
            public String getTag() {
                return tag;
            }

            @Override
            public int getFuzz() {
                return fuzz;
            }

            @Override
            public int numThreads() {
                return numThreads;
            }

            @Override
            public int getAccumDuration() {
                return accumDuration;
            }

            @Override
            public Level getLoggingLevel() {
                return logLevel;
            }

            @Override
            public List<Future<GgpStateMachine>> createChargeMachines() {
                return Lists.reverse(machinesCharger);
            }

            @Override
            public List<Charger> createChargers() {
                return Lists.reverse(chargers);
            }

            @Override
            public List<Pool> createChargePools() {
                return Lists.reverse(poolsCharger);
            }

            @Override
            public GgpStateMachine createMainMachine() {
                return machineMain;
            }

            @Override
            public Pool getAccumulatorPool() {
                return machineMain.prover.pool;
            }

            @Override
            public Charger getAccumulator() {
                return new Charger(rolesMain);
            }
        };
    }
}
