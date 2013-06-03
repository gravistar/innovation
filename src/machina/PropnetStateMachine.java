package machina;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import propnet.Node;
import propnet.PropNet;
import propnet.PropNetFactory;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.util.Colut;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/31/13
 * Time: 8:12 PM
 *
 * A simple in-memory propnet state machine.
 * Note: doeses and legals separate
 *       bases and nexts separate
 * This is done to keep the propnet a DAG.  This shouldn't be bad because
 * number of bases/legals is not that high.
 *
 * Potential hotspot:
 *  we have to advance twice because of the bases/nexts
 *
 */
public class PropNetStateMachine implements GgpStateMachine{
    public PropNet net;
    public Map<Dob,Dob> nextToBase;
    public Set<Dob> initBases;
    public Set<Dob> legals;
    public Set<Dob> alwaysTrue;
    public Set<Dob> goals;
    public Set<Dob> doeses;
    public GameLogicContext context;
    public boolean verbose = false;

    // TODO: is there a better way to set up the latches? this might be ok though
    private PropNetStateMachine(PropNet net, GameLogicContext context, Set<Dob> alwaysTrue) {
        this.net = net;
        this.context = context;

        // setup all these things
        this.alwaysTrue = alwaysTrue;
        legals = findDobs(net.props.keySet(), context.LEGAL);
        goals = findDobs(net.props.keySet(), context.GOAL);
        doeses = findDobs(net.props.keySet(), context.DOES);

        Set<Dob> bases = findDobs(net.props.keySet(), context.TRUE);
        Set<Dob> nexts = findDobs(net.props.keySet(), context.NEXT);

        initBases = findInitBases(net.props.keySet(), bases, context.INIT);
        nextToBase = mapNextToBase(nexts, bases);
    }

    public void printMappings() {
        System.out.println("[INIT] " + context.INIT);
        System.out.println("[LATCHES] " + alwaysTrue);
        System.out.println("[LEGALS] " + legals);
        System.out.println("[GOALS] " + goals);
        System.out.println("[DOESES] " + doeses);
        System.out.println("[NEXT TO BASE] " + nextToBase);
        System.out.println("[INIT BASES] " + initBases);
    }

    public static Map<Dob,Dob> mapNextToBase(Set<Dob> nexts, Set<Dob> bases) {
        Map<Dob, Dob> ret = Maps.newHashMap();
        for (Dob next : nexts) {
            for (Dob base : bases) {
                if (Dob.compare(next.at(1), base.at(1)) == 0) {
                    ret.put(next, base);
                    break;
                }
            }
        }
        return ret;
    }

    // seems hacky that i should even do this
    public static Set<Dob> findInitBases(Set<Dob> grounds, Set<Dob> bases, Dob initName) {
        // get grounds with init
        Set<Dob> inits = Sets.newHashSet();
        for (Dob ground : grounds) {
            if (ground.size() < 2)
                continue;
            if (Dob.compare(ground.at(0), initName) == 0)
                inits.add(ground);
        }

        Set<Dob> ret = Sets.newHashSet();
        for (Dob init : inits) {
            for (int i=0; i<init.size(); i++) {
                for (Dob base : bases) {
                    if (Dob.compare(base.at(1), init.at(i)) == 0)
                        ret.add(base);
                }
            }
        }
        return ret;
    }

    public static Set<Dob> findDobs(Set<Dob> grounds, Dob prefix) {
        Set<Dob> ret = Sets.newHashSet();
        for (Dob ground : grounds) {
            if (ground.size() == 0)
                continue;
            if (Dob.compare(ground.at(0), prefix) == 0)
                ret.add(ground);
        }
        return ret;
    }

    // piggybacks off the backward state machine
    public static PropNetStateMachine createPropNetStateMachine(List<Rule> rules) {
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Ruletta ruletta = machine.rta;
        GameLogicContext context = machine;
        StratifiedProver prover = machine.prover;
        Pool pool = prover.pool;

        // get the initial grounds
        Set<Dob> initGrounds = prover.proveAll(Lists.<Dob>newArrayList());

        // convert inputs to does
        initGrounds = ProverStateMachine.submersiveReplace(initGrounds, context.INPUT_UNIFY, pool);

        // convert base to trues
        initGrounds = ProverStateMachine.submersiveReplace(initGrounds, context.BASE_UNIFY, pool);

        Cachet cachet = new Cachet(ruletta);
        cachet.storeAllGround(initGrounds);
        PropNet net = PropNetFactory.buildNet(ruletta, cachet);
        Set<Dob> alwaysTrue = findAlwaysTrue(context, initGrounds);
        return new PropNetStateMachine(net, context, alwaysTrue);
    }

    /**
     * alwaysTrue = InitGrounds - {terminal} - {init} - {true} - {does} - {goal}
     * @param initGrounds
     * @return
     */
    public static Set<Dob> findAlwaysTrue(GameLogicContext context, Set<Dob> initGrounds) {
        Set<Dob> alwaysTrue = initGrounds;
        alwaysTrue = Colut.difference(alwaysTrue, findDobs(initGrounds, context.TERMINAL));
        alwaysTrue = Colut.difference(alwaysTrue, findDobs(initGrounds, context.INIT));
        alwaysTrue = Colut.difference(alwaysTrue, findDobs(initGrounds, context.TRUE));
        alwaysTrue = Colut.difference(alwaysTrue, findDobs(initGrounds, context.DOES));
        alwaysTrue = Colut.difference(alwaysTrue, findDobs(initGrounds, context.GOAL));
        return alwaysTrue;
    }

    @Override
    public Set<Dob> getInitial() {
        net.onBases = initBases;
        setTrues();
        net.advance();
        return extractState();
    }

    @Override
    public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
        return extractLegals();
    }

    public ListMultimap<Dob,Dob> extractLegals() {
        ListMultimap<Dob,Dob> ret = ArrayListMultimap.create();
        for (Dob legal : legals) {
            if (net.props.get(legal).val) {
                ret.put(legal.at(1), Unifier.replace(legal, context.LEGAL_UNIFY));
            }
        }
        return ret;
    }

    public boolean checkTerminal() {
        return net.props.get(context.TERMINAL).val;
    }

    public void setTrues() {
        // active latches
        for (Dob latch : alwaysTrue)
            net.props.get(latch).val = true;
        // turn on cached bases
        for (Dob base : net.onBases)
            net.props.get(base).val = true;
    }

    public void wipeBases() {
        for (Dob base : nextToBase.values())
            net.props.get(base).val = false;
    }

    public Set<Dob> advance(Set<Dob> state, Map<Dob,Dob> actions) {
        net.wipe();
        applyState(state);
        applyActions(actions);
        net.advance();
        // ---- done to convert next to true...
        net.onBases = getOnBases();
        net.wipe();
        setTrues();
        net.advance();
        // -----------------------
        return extractState();
    }

    public Set<Dob> advance(Map<Dob,Dob> actions) {
        net.wipe();
        setTrues();
        net.onBases.clear();
        applyActions(actions);
        net.advance();
        net.onBases = getOnBases();
        // do again to apply nexts
        net.wipe();
        setTrues();
        net.advance();
        return extractState();
    }

    public Set<Dob> getOnBases() {
        Set<Dob> ret = Sets.newHashSet();
        for (Dob next : nextToBase.keySet())
            if (net.props.get(next).val) {
                Dob base = nextToBase.get(next);
                ret.add(base);
            }
        return ret;
    }

    @Override
    public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
        return advance(state, actions);
    }

    public void applyActions(Map<Dob, Dob> actions) {
        for (Dob role : actions.keySet()) {
            Dob action = actions.get(role);
            for (Dob does : doeses) {
                if (Dob.compare(does, action) == 0)
                    net.props.get(does).val = true;
            }
        }
    }

    // just gives all the props that are true
    public Set<Dob> extractState() {
        Set<Dob> state = Sets.newHashSet();
        for (Dob prop : net.props.keySet()) {
            if (net.props.get(prop).val)
                state.add(prop);
        }
        return state;
    }

    // print props and values in topographic order
    public static void printTopographicProps(PropNet net) {
        Map<Node,Dob> invProp = net.invProps();
        for (Node node : net.tnet) {
            if (invProp.containsKey(node)) {
                Dob prop = invProp.get(node);
                System.out.println("[PROP " + prop + "] VALUE: " + node.val);
            }
        }
        System.out.println();
    }

    public void applyState(Set<Dob> state) {
        for (Dob prop : state)
            net.props.get(prop).val = true;
    }

    // WARNING: this assumes isTerminal is used right
    // isTerminal(state) -> state = nextState(state,actions) -> isTerminal(state)
    @Override
    public boolean isTerminal(Set<Dob> state) {
        return net.props.get(context.TERMINAL).val;
    }

    @Override
    public Map<Dob,Integer> getGoals(Set<Dob> state) {
        return extractGoals();
    }

    public Map<Dob, Integer> extractGoals() {
        Map<Dob,Integer> ret = Maps.newHashMap();
        for (Dob goal : goals) {
            if (net.props.get(goal).val) {
                Preconditions.checkArgument(goal.at(0) == context.GOAL);
                Dob role = goal.at(1);
                Dob value = goal.at(2);
                Preconditions.checkArgument(!ret.containsKey(role));
                ret.put(role, Integer.parseInt(value.name));
            }
        }
        return ret;
    }
}
