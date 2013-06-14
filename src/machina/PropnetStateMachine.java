package machina;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import propnet.Node;
import propnet.PropNet;
import propnet.PropNetFactory;
import propnet.PropNetInterface;
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
import rekkura.state.algorithm.Topper;
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
    public PropNetInterface net;
    public Map<Dob,Dob> nextToBase;
    public Map<Dob,Dob> legalToDoes;
    public Set<Dob> initBases;
    public Set<Dob> legals;
    public Set<Dob> alwaysTrue;
    public Set<Dob> goals;
    public Set<Dob> doeses;
    public GameLogicContext context;
    public boolean verbose = false;

    // TODO: is there a better way to set up the latches? this might be ok though
    private PropNetStateMachine(PropNetInterface net, GameLogicContext context, Set<Dob> alwaysTrue) {
        this.net = net;
        this.context = context;

        // setup all these things
        Colut.remove(alwaysTrue, context.TERMINAL);

        this.alwaysTrue = alwaysTrue;
        legals = findDobs(net.props(), context.LEGAL);
        goals = findDobs(net.props(), context.GOAL);
        doeses = findDobs(net.props(), context.DOES);

        Set<Dob> bases = findDobs(net.props(), context.TRUE);
        Set<Dob> nexts = findDobs(net.props(), context.NEXT);

        initBases = findInitBases(net.props(), bases, context.INIT);
        nextToBase = constructDobMapping(nexts, bases, nextBaseEquals);
        legalToDoes = constructDobMapping(legals, doeses, legalDoesEqual);
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

    public static interface DobEqualsFn {
        public boolean equals(Dob left, Dob right);
    }

    public static DobEqualsFn nextBaseEquals = new DobEqualsFn() {
        @Override
        public boolean equals(Dob left, Dob right) {
            return Dob.compare(left.at(1), right.at(1)) == 0;
        }
    };

    public static DobEqualsFn legalDoesEqual = new DobEqualsFn() {
        @Override
        public boolean equals(Dob left, Dob right) {
            return (Dob.compare(left.at(1), right.at(1)) == 0)
                    && (Dob.compare(left.at(2), right.at(2)) == 0);
        }
    };

    public static Map<Dob,Dob> constructDobMapping(Set<Dob> keys, Set<Dob> values, DobEqualsFn equalsFn) {
        Map<Dob, Dob> ret = Maps.newHashMap();
        for (Dob key : keys) {
            for (Dob value : values) {
                if (equalsFn.equals(key, value)) {
                    ret.put(key, value);
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

    /**
     * This is gross because code is duplicated with PropNetFactory.createFromRules
     * but unfortunately java doesn't have pattern matching
     * @param rules
     * @return
     */
    public static PropNetStateMachine createPropNetStateMachine(List<Rule> rules) {
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        PropNet net = PropNetFactory.buildNet(initGrounds, topRuleOrder, machine.rta.fortre.pool, cachet);
        Set<Dob> alwaysTrue = findAlwaysTrue(machine, initGrounds);
        return new PropNetStateMachine(net, machine, alwaysTrue);
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
        net.wipe();
        applyLatches();
        applyState(initBases);
        // there is no next one. special case
        return Colut.union(initBases, extractTrues());
    }

    @Override
    public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
        net.wipe();
        applyLatches();
        applyState(state);
        net.advance();
        return extractLegals();
    }

    public ListMultimap<Dob,Dob> extractLegals() {
        ListMultimap<Dob,Dob> ret = ArrayListMultimap.create();
        for (Dob legal : legals) {
            if (net.val(legal)) {
                ret.put(legal.at(1), legalToDoes.get(legal));
            }
        }
        return ret;
    }

    public Set<Dob> advance(Set<Dob> state, Map<Dob,Dob> actions) {
        net.wipe();
        applyLatches();
        applyState(state);
        applyActions(actions);
        net.advance();
        return extractNexts();
    }

    public Set<Dob> extractTrues() {
        Set<Dob> ret = Sets.newHashSet();
        for (Dob truth : nextToBase.values())
            if (net.val(truth))
                ret.add(truth);
        return ret;
    }

    public Set<Dob> extractNexts() {
        Set<Dob> ret = Sets.newHashSet();
        for (Dob next : nextToBase.keySet())
            if (net.val(next)) {
                Dob base = nextToBase.get(next);
                ret.add(base);
            }
        return ret;
    }

    @Override
    public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
        return advance(state, actions);
    }

    public void applyLatches() {
        for (Dob latch : alwaysTrue)
            net.set(latch, true);
    }

    public void applyState(Set<Dob> state) {
        for (Dob truth : state)
            net.set(truth, true);
    }

    public void applyActions(Map<Dob, Dob> actions) {
        for (Dob role : actions.keySet()) {
            Dob action = actions.get(role);
            for (Dob does : doeses) {
                if (Dob.compare(does, action) == 0)
                    net.set(does, true);
            }
        }
    }

    // just gives all the props that are true
    public Set<Dob> extractState() {
        Set<Dob> state = Sets.newHashSet();
        for (Dob prop : net.props()) {
            if (net.val(prop))
                state.add(prop);
        }
        return state;
    }

    // WARNING: this assumes isTerminal is used right
    // isTerminal(state) -> state = nextState(state,actions) -> isTerminal(state)
    @Override
    public boolean isTerminal(Set<Dob> state) {
        net.wipe();
        applyLatches();
        applyState(state);
        net.advance();
        return net.val(context.TERMINAL);
    }

    @Override
    public Map<Dob,Integer> getGoals(Set<Dob> state) {
        return extractGoals();
    }

    public Map<Dob, Integer> extractGoals() {
        Map<Dob,Integer> ret = Maps.newHashMap();
        for (Dob goal : goals) {
            if (net.val(goal)) {
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
