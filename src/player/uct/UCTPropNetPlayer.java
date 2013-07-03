package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;

/**
 * User: david
 * Date: 6/2/13
 * Time: 9:50 PM
 * Description:
 *      Subclasses of this (either Vanilla or Native) just need to provide how to build
 *      the propnet machine.
 */
public abstract class UCTPropNetPlayer extends UCTPlayer2<PropNetStateMachine> {
    private UCTCharger charger;

    protected final void plan() {
        charger = new UCTCharger(Lists.newArrayList(machine.getActions(machine.getInitial()).keySet()));
        System.out.println(getTag() + "Done building charger!");
        System.out.println(getTag() + "Role: " + role);
        explore();
    }

    @Override
    protected void prepare() {
        this.role = machine.context.pool.dobs.submerge(role);
        plan();
    }
}
