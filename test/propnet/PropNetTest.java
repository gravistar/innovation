package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.format.KifFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.test.ggp.SimpleGames;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/30/13
 * Time: 10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetTest {

    @Test
    public void negativeGroundedBodyTest() {
        String [] raw = {
            "(<= (prop1 x) (prop2 y) (not (prop3 z)))",
            "(<= (prop2 y))",
            "(<= (prop3 z))"
        };
        List<Rule> rules = KifFormat.genericStringsToRules(raw);

        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Ruletta ruletta = machine.rta;
        GameLogicContext context = machine;
        StratifiedProver prover = machine.prover;
        Pool pool = prover.pool;
        Set<Dob> initGrounds = prover.proveAll(Lists.<Dob>newArrayList());
        Cachet cachet = new Cachet(ruletta);
        cachet.storeAllGround(initGrounds);

        Map<Dob,Node> props = Maps.newHashMap();
        Set<Node> net = Sets.newHashSet();

        Rule neg = null;
        for (Rule rule : ruletta.allRules) {
            if (!initGrounds.contains(rule.head.dob))
                neg = rule;
        }

        //PropNetFactory.processAllNegativeBody(Preconditions.checkNotNull(neg), cachet, props, net);
        //System.out.println("NEGATIVE TEST NET");
        //System.out.println(net);
    }

    @Test
    public void printLightsOnPropnet() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();

        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Ruletta ruletta = machine.rta;
        GameLogicContext context = machine;
        StratifiedProver prover = machine.prover;
        Pool pool = prover.pool;

        // get the initial grounds
        Set<Dob> initGrounds = prover.proveAll(Lists.<Dob>newArrayList());

        System.out.println("[INIT GROUNDS] " + initGrounds);

        // convert inputs to does
        initGrounds = ProverStateMachine.submersiveReplace(initGrounds, context.INPUT_UNIFY, pool);

        // convert base to trues
        initGrounds = ProverStateMachine.submersiveReplace(initGrounds, context.BASE_UNIFY, pool);

        System.out.println("[INIT GROUNDS AFTER UNIFIES] " + initGrounds);

        Cachet cachet = new Cachet(ruletta);
        cachet.storeAllGround(initGrounds);

        PropNet net = PropNetFactory.buildNet(ruletta, cachet);

        System.out.println("==== LIGHTS ON NET ====");
        System.out.println(net);
    }


}
