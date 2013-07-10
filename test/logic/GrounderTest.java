package logic;

import com.google.common.collect.Sets;
import org.junit.Test;
import propnet.vanilla.PropNetFactory;
import propnet.vanilla.Statics;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Cachet;
import rekkura.state.algorithm.Topper;

import java.util.List;
import java.util.Set;

/**
 * User: david
 * Date: 7/8/13
 * Time: 9:43 PM
 * Description:
 *     More for debugging purposes
 */
public class GrounderTest {

    @Test
    public void tictacheaven() {
        List<Rule> rules = Statics.rulesForGame("tictacheavenfc.kif");
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        Set<Dob> known = Sets.newHashSet(initGrounds);
        Grounder.getValidGroundings(topRuleOrder, known, machine.rta.fortre.pool, cachet);
    }

    @Test
    public void buttonsandlights() {
        List<Rule> rules = Statics.rulesForGame("bestbuttonsandlights.kif");
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        Set<Dob> known = Sets.newHashSet(initGrounds);
        Grounder.getValidGroundings(topRuleOrder, known, machine.rta.fortre.pool, cachet);
    }

    @Test
    public void dualconnect4() {
        List<Rule> rules = Statics.rulesForGame("dualconnect4.kif");
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        Set<Dob> known = Sets.newHashSet(initGrounds);
        Grounder.getValidGroundings(topRuleOrder, known, machine.rta.fortre.pool, cachet);
    }

}
