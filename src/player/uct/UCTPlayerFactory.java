package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;
import propnet.PropNetInterface;
import propnet.nativecode.NativePropNet;
import propnet.util.Tuple2;
import propnet.util.Tuple3;
import propnet.vanilla.PropNetFactory;
import propnet.nativecode.NativePropNetFactory;
import propnet.nativecode.NativeUtil;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: david
 * Date: 6/26/13
 * Time: 12:01 PM
 * Description:
 *      Builds various UCT Players
 */
public class UCTPlayerFactory {

    public static UCTPlayer<BackwardStateMachine> createProverPlayer(){
        return new UCTPlayer<BackwardStateMachine>() {
            @Override
            public int numThreads() {
                return 1;
            }

            @Override
            public String getTag() {
                return "[UCT Prover]";
            }

            // unfortunately, these are copied from Player.ProverBased
            @Override
            protected BackwardStateMachine constructMachine(Collection<Rule> rules) {
                this.rules = Lists.newArrayList(rules);
                return BackwardStateMachine.createForRules(rules);
            }

            @Override
            protected final void prepare() {
                this.role = machine.prover.pool.dobs.submerge(role);
                plan();
            }

            @Override
            protected void reflect() {
                System.out.println("REFLECT");
                if (this.verbose)
                    printStats();
                chargeManager.shutdownNow();

            }
        };
    }

    public static UCTPropNetPlayer createVanillaPropNetPlayer(){
        return new UCTPropNetPlayer() {
            @Override
            public int numThreads() {
                return 1;
            }

            @Override
            public String getTag() {
                return "[UCT PropNet Vanilla]";
            }

            @Override
            protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
                List<Rule> asList = Lists.newArrayList(rules);
                this.rules = asList;
                return PropNetStateMachine.create(PropNetFactory.createForStateMachine(asList));
            }

            @Override
            protected void reflect() {
                if (this.verbose)
                    printStats();
                chargeManager.shutdownNow();
            }
        };
    }

    public static UCTPropNetPlayer createNativePropNetPlayer() {
        return new UCTPropNetPlayer() {
            @Override
            public int numThreads() {
                return 1;
            }

            @Override
            public String getTag() {
                return "[UCT PropNet Native]";
            }

            @Override
            protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
                List<Rule> asList = Lists.newArrayList(rules);
                this.rules = asList;
                return PropNetStateMachine.create(NativePropNetFactory.createForStateMachine(asList));
            }

            @Override
            protected void reflect() {
                chargeManager.shutdownNow();
                if (this.verbose)
                    printStats();
                // cleanup
                //NativeUtil.deleteGeneratedFiles();
            }
        };
    }

    public static UCTPropNetPlayer createNativePropNetPlayerThreads() {
        return new UCTPropNetPlayer() {

            Tuple2<Tuple3<String, Integer, Map<Dob, Integer>>, GameLogicContext> params;

            @Override
            public int numThreads() {
                return Runtime.getRuntime().availableProcessors();
            }

            @Override
            public String getTag() {
                return "[UCT PropNet Native " + numThreads() + " Threads]";
            }

            @Override
            protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
                this.rules = Lists.newArrayList(rules);
                if (params == null)
                    params = NativePropNetFactory.compileFromRulesAllParams(this.rules);
                NativePropNet net = NativePropNetFactory.getCompiledNet(params._1);
                Tuple2<PropNetInterface, GameLogicContext> pnsmParams = new Tuple2<PropNetInterface, GameLogicContext>
                        (net, params._2);
                return PropNetStateMachine.create(pnsmParams);
            }

            @Override
            protected void reflect() {
                chargeManager.shutdownNow();
                if (this.verbose)
                    printStats();
            }
        };

    }

}
