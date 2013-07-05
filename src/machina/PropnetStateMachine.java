package machina;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import propnet.vanilla.PropNetFactory;
import propnet.PropNetInterface;
import propnet.util.Tuple2;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.util.Colut;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 5/31/13
 * Time: 8:12 PM
 * Description:
 *      A state machine that is backed by a PropNetInterface. It can use either native or vanilla.
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

    private PropNetStateMachine(PropNetInterface net, GameLogicContext context) {
        this.net = net;
        this.context = context;
        this.alwaysTrue = getSubmerged(filterLatches(Rule.ruleHeads(context.staticRules), context), net.props());
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

    public static boolean isLatch(Dob latch, GameLogicContext context) {
        if (latch.size() == 0) {
            System.out.println("Weird latch: " + latch);
            return true;
        }
        if (latch.at(0).equals(context.INIT))
            return false;
        if (latch.at(0).equals(context.DOES))
            return false;
        if (latch.at(0).equals(context.BASE))
            return false;
        return true;
    }

    public static Set<Dob> filterLatches(Set<Dob> latches, GameLogicContext context) {
        Set<Dob> ret = Sets.newHashSet();
        for (Dob latch : latches) {
            if (isLatch(latch, context))
                ret.add(latch);
        }
        return ret;
    }

    public static Set<Dob> getSubmerged(Iterable<Dob> toFind, Set<Dob> findFrom) {
        Map<String, Dob> names = Maps.newHashMap();
        for (Dob d : findFrom)
            names.put(d.toString(), d);

        Set<Dob> ret = Sets.newHashSet();
        for (Dob d : toFind)
            if (names.containsKey(d.toString()))
                ret.add(names.get(d.toString()));
        return ret;
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
     * @param rules
     * @return
     */
    public static PropNetStateMachine fromRules(List<Rule> rules) {
        return create(PropNetFactory.createForStateMachine(rules));
    }

    /**
     * Create a pnsm from required components
     * @param param
     * @return
     */
    public static PropNetStateMachine create(Tuple2<PropNetInterface, GameLogicContext> param) {
        PropNetInterface net = param._1;
        GameLogicContext context = param._2;
        Preconditions.checkNotNull(net);
        Preconditions.checkNotNull(context);
        return new PropNetStateMachine(net, context);
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
        advance(state, Maps.<Dob,Dob>newHashMap());
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

    public void advance(Set<Dob> state, Map<Dob,Dob> actions) {
        net.wipe();
        applyLatches();
        applyState(state);
        applyActions(actions);
        net.advance();
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
        advance(state, actions);
        return extractNexts();
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

    @Override
    public boolean isTerminal(Set<Dob> state) {
        advance(state, Maps.<Dob,Dob>newHashMap());
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
