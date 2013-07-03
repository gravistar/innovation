package player.uct;

import com.google.common.collect.Lists;
import machina.PropNetStateMachine;
import propnet.PropNetFactory;
import rekkura.logic.model.Rule;

import java.util.Collection;
import java.util.List;

/**
 * User: david
 * Date: 6/27/13
 * Time: 10:17 PM
 * Description:
 */
public class UCTPropNetPlayerVanilla extends UCTPropNetPlayer{
    @Override
    public String getTag() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected PropNetStateMachine constructMachine(Collection<Rule> rules) {
        List<Rule> theRules = Lists.newArrayList(rules);
        return PropNetStateMachine.create(PropNetFactory.createForStateMachine(theRules));
    }

    @Override
    protected void prepare() {

    }
}
