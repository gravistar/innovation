package player.uct;

import machina.PropNetStateMachine;

/**
 * User: david
 * Date: 6/2/13
 * Time: 9:50 PM
 * Description:
 *      Subclasses of this (either Vanilla or Native) just need to provide how to build
 *      the propnet machine.
 */
public abstract class UCTPropNetPlayer extends UCTPlayer<PropNetStateMachine> {
    @Override
    protected void prepare() {
        this.role = machine.context.pool.dobs.submerge(role);
        plan();
    }
}
