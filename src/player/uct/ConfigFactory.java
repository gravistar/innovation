package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;
import propnet.vanilla.PropNetFactory;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * User: david
 * Date: 7/13/13
 * Time: 12:45 PM
 * Description:
 *      Builds different UCT player configurations
 */
public class ConfigFactory {

    public static ConfigInterface singleProverConfig(final List<Rule> rules) {
        final int numThreads = 1;

        // Create the main
        final BackwardStateMachine machineMain = BackwardStateMachine.createForRules(rules);
        final List<Dob> rolesMain = Lists.newArrayList(machineMain.getActions(machineMain.getInitial()).keySet());

        // Create the chargers
        final List<Future<GgpStateMachine>> machinesCharger = Lists.newArrayList();
        final List<Charger> chargers = Lists.newArrayList();
        final List<Pool> poolsCharger = Lists.newArrayList();

        for (int i=0; i<numThreads; i++) {
            // create the stuff
            final BackwardStateMachine machineCharger = BackwardStateMachine.createForRules(rules);
            final Pool pool = machineCharger.prover.pool;
            List<Dob> rolesCharger = Lists.newArrayList(machineCharger.getActions(
                    machineCharger.getInitial()).keySet());
            final Charger charger = new Charger(rolesCharger);

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
            poolsCharger.add(pool);
            chargers.add(charger);
        }

        return new ConfigInterface() {
            @Override
            public String getTag() {
                return "[UCT Prover 1 Thread]";
            }

            @Override
            public int getFuzz() {
                return UCTStatics.fuzzSuperRisky;
            }

            @Override
            public int numThreads() {
                return numThreads;
            }

            @Override
            public Level getLoggingLevel() {
                return Level.INFO;
            }


            @Override
            public List<Charger> createChargers() {
                return chargers;
            }

            @Override
            public List<Future<GgpStateMachine>> createChargeMachines() {
                return machinesCharger;
            }

            @Override
            public List<Pool> createChargePools() {
                return poolsCharger;
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

    public static ConfigInterface singlePropNetVanillaConfig(final List<Rule> rules) {
        final int numThreads = 1;

        // Create the main
        final BackwardStateMachine machineMain = BackwardStateMachine.createForRules(rules);
        //final PropNetStateMachine machineMain = PropNetStateMachine.create(PropNetFactory.createForStateMachine(rules));
        final List<Dob> rolesMain = Lists.newArrayList(machineMain.getActions(machineMain.getInitial()).keySet());

        // Create the chargers
        final List<Future<GgpStateMachine>> machinesCharger = Lists.newArrayList();
        final List<Charger> chargers = Lists.newArrayList();
        final List<Pool> poolsCharger = Lists.newArrayList();

        for (int i=0; i<numThreads; i++) {
            // create the stuff
            final PropNetStateMachine machineCharger = PropNetStateMachine.create(PropNetFactory.createForStateMachine(rules));
            final Pool pool = machineCharger.context.pool;
            List<Dob> rolesCharger = Lists.newArrayList(machineCharger.getActions(
                    machineCharger.getInitial()).keySet());
            final Charger charger = new Charger(rolesCharger);

            // set verbosity
            charger.verbose = false;

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
            poolsCharger.add(pool);
            chargers.add(charger);
        }

        return new ConfigInterface() {
            @Override
            public String getTag() {
                return "[UCT PropNet 1 Thread]";
            }

            @Override
            public int getFuzz() {
                return UCTStatics.fuzzSuperRisky;
            }

            @Override
            public int numThreads() {
                return 1;
            }

            @Override
            public Level getLoggingLevel() {
                return Level.INFO;
            }

            @Override
            public List<Charger> createChargers() {
                return chargers;
            }

            @Override
            public List<Future<GgpStateMachine>> createChargeMachines() {
                return machinesCharger;
            }

            @Override
            public List<Pool> createChargePools() {
                return poolsCharger;
            }

            @Override
            public GgpStateMachine createMainMachine() {
                return machineMain;
            }

            @Override
            public Pool getAccumulatorPool() {
                return machineMain.prover.pool;
                //return machineMain.context.pool;
            }

            @Override
            public Charger getAccumulator() {
                return new Charger(rolesMain);
            }
        };
    }

}
