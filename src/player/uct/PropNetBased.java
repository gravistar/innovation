package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;
import rekkura.ggp.milleu.Player;
import rekkura.logic.model.Rule;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/2/13
 * Time: 9:30 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PropNetBased extends Player.StateBased<PropNetStateMachine> {

    @Override
    protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
        System.out.println("[PropNetStateMachine] Creating machine...");
        PropNetStateMachine machine = PropNetStateMachine.createPropNetStateMachine(Lists.<Rule>newArrayList(rules));
        System.out.println("[PropNetStateMachine] Done creating machine...");
        return machine;
    }

    @Override
    protected final void prepare() {
        this.role = machine.context.pool.dobs.submerge(role);
        plan();
    }

    protected abstract void plan();
}
