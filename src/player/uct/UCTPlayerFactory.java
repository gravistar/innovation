package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;
import propnet.PropNetFactory;
import propnet.nativecode.NativePropNetFactory;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.logic.model.Rule;

import java.util.Collection;
import java.util.List;

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
            public String getTag() {
                return "[UCT Prover]";
            }

            // unfortunately, these are copied from Player.ProverBased
            @Override
            protected BackwardStateMachine constructMachine(Collection<Rule> rules) {
                return BackwardStateMachine.createForRules(rules);
            }

            @Override
            protected final void prepare() {
                this.role = machine.prover.pool.dobs.submerge(role);
                plan();
            }
        };
    }

    public static UCTPropNetPlayer createVanillaPropNetPlayer(){
        return new UCTPropNetPlayer() {
            @Override
            public String getTag() {
                return "[UCT PropNet Vanilla]";
            }

            @Override
            protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
                List<Rule> asList = Lists.newArrayList(rules);
                return PropNetStateMachine.create(PropNetFactory.createForStateMachine(asList));
            }
        };
    }

    // reflect should have cleanup?
    public static UCTPropNetPlayer createNativePropNetPlayer() {
        return new UCTPropNetPlayer() {
            @Override
            public String getTag() {
                return "[UCT PropNet Native]";
            }

            @Override
            protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
                List<Rule> asList = Lists.newArrayList(rules);
                return PropNetStateMachine.create(NativePropNetFactory.createForStateMachine(asList));
            }
        };
    }

}
