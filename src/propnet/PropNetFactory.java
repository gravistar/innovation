package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import propnet.util.Tuple2;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.state.algorithm.Topper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 5/25/13
 * Time: 11:34 AM
 * Description:
 *      Generates a vanilla propnet.
 */
public class PropNetFactory {

    public static boolean debug = false;

    /**
     * Lowest level create from rules. Returns the vanilla propnet.
     * @param rules
     * @return
     *      (generated propnet, the machine)
     */
    public static Tuple2<PropNet, GameLogicContext> createFromRules(List<Rule> rules) {
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        PropNet net = PropNetFactory.buildNet(initGrounds, topRuleOrder, machine.rta.fortre.pool, cachet);
        return new Tuple2<PropNet, GameLogicContext>(net, machine);
    }

    public static Tuple2<PropNetInterface, GameLogicContext> createForStateMachine(List<Rule> rules) {
        Tuple2<PropNet, GameLogicContext> orig = createFromRules(rules);
        return new Tuple2<PropNetInterface, GameLogicContext>(orig._1, orig._2);
    }

    /**
     * For use by the native factory
     * @param rules
     * @return
     */
    public static PropNet createFromRulesOnlyNet(List<Rule> rules) {
        return createFromRules(rules)._1;
    }

    /**
     * Does GDL specific things to prepare init, ruletta, and cachet for the propnet factory
     * Mutates cachet, ruletta in machine
     * @param machine
     * @return
     */
    public static Set<Dob> prepareMachine(BackwardStateMachine machine) {
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

        return initGrounds;
    }

    /**
     * Creates a node for a ground and adds it to prop map and net
     * @param ground
     * @param props
     * @param net
     * @return
     */
    public static Node getNodeForProp(Dob ground, Map<Dob,Node> props, Set<Node> bottom, Set<Node> net) {
        boolean exists = props.containsKey(ground);

        if (!exists) {
            Node node = Node.NodeFactory.makeOr();
            bottom.add(node);
            net.add(node);
            props.put(ground, node);
        }
        return props.get(ground);
    }

    public static Node createAndAddAND(Set<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeAnd(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static Node createAndAddNOT(Set<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeNot(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static Node createAndAddOR(Set<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeOr(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static void attachInput(Node node, Node input, Set<Node> bottom) {
        node.inputs.add(input);
        bottom.remove(input);
    }

    // For debugging
    public static Set<Node> trueBottom(Set<Node> net) {
        Set<Node> truth = Sets.newHashSet();
        for (Node a : net) {
            boolean bot = true;
            for (Node b : net)
                if (b.inputs.contains(a))
                    bot = false;
            if (bot)
                truth.add(a);
        }
        return truth;
    }

    // Can only make nodes for grounded rules
    public static void processRuleSomePositiveBody(Dob head,
                                                   List<Atom> bodies,
                                                   Cachet cachet,
                                                   Map<Dob,Node> props,
                                                   Set<Node> bottom,
                                                   Set<Node> net) {
        // generate node for head grounding
        Node headNode = getNodeForProp(head, props, bottom, net);

        // generate a bigOR that's the sole input to headNode if not done already
        if (headNode.inputs.isEmpty())
            attachInput(headNode, createAndAddOR(Sets.<Node>newHashSet(), bottom, net), bottom);

        Preconditions.checkArgument(headNode.inputs.size() == 1);
        Node bigOR = headNode.inputs.iterator().next();

        List<Dob> positiveBodyGrounds = Lists.newArrayList();
        List<Dob> negBodyGrounds = Lists.newArrayList();
        for (Atom pos : Atom.filterPositives(bodies))
            positiveBodyGrounds.add(pos.dob);

        for (Atom neg : Atom.filterNegatives(bodies))
            negBodyGrounds.add(neg.dob);


        Set<Node> posNodes = Sets.newHashSet();
        Set<Node> negNodes = Sets.newHashSet();

        List<Dob> bodyGrounds = Lists.newArrayList();
        for (Atom body : bodies)
            bodyGrounds.add(body.dob);

        for (Dob pos : positiveBodyGrounds)
            posNodes.add(getNodeForProp(pos, props, bottom, net));

        for (Dob neg : negBodyGrounds)
            negNodes.add(getNodeForProp(neg, props, bottom, net));

        // create AND for this set of body groundings
        Node AND = createAndAddAND(posNodes, bottom, net);

        // squash nots and add as inputs to the and
        if (negNodes.isEmpty() == false) {
            Node negOR = createAndAddOR(negNodes, bottom, net);
            Node negNOT = createAndAddNOT(Sets.newHashSet(negOR), bottom, net);

            // add to posAND
            attachInput(AND, negNOT, bottom);
        }

        // add to bigOR inputs
        attachInput(bigOR, AND, bottom);
    }

    public static void processRuleAllNegativeBody(Dob head,
                                                  List<Atom> body,
                                                  Cachet cachet,
                                                  Map<Dob, Node> props,
                                                  Set<Node> bottom,
                                                  Set<Node> net) {
        Node headNode = getNodeForProp(head, props, bottom, net);

        Set<Node> negPropNodes = Sets.newHashSet();
        for (Atom bodyTerm : body)
            negPropNodes.add(getNodeForProp(bodyTerm.dob, props, bottom, net));

        Node orNode = createAndAddOR(negPropNodes, bottom, net);
        Node notNode = createAndAddNOT(Sets.newHashSet(orNode), bottom, net);

        // generate a bigOR that's the sole input to headNode if not done already
        if (headNode.inputs.isEmpty())
            attachInput(headNode, createAndAddOR(Sets.<Node>newHashSet(), bottom, net), bottom);

        Preconditions.checkArgument(headNode.inputs.size() == 1);
        Node bigOR = headNode.inputs.iterator().next();
        attachInput(bigOR, notNode, bottom);
    }

    public static boolean isDoes(Dob head) {
        return head.size() > 0 && head.at(0).name.equals("does");
    }

    /**
     * Can only add nodes for grounded rules
     * @param cachet
     * @param props
     * @param bottom
     * @param net
     */
    public static void processGrounding(Dob head,
                                        List<Atom> bodies,
                                        Cachet cachet,
                                        Map<Dob,Node> props,
                                        Set<Node> bottom,
                                        Set<Node> net) {
        // stupid gdl special case
        if (bodies.isEmpty() || isDoes(head)) {
            getNodeForProp(head, props, bottom, net);
            return;
        }

        if (Atom.filterPositives(bodies).isEmpty())
            processRuleAllNegativeBody(head, bodies, cachet, props, bottom, net);
        else
            processRuleSomePositiveBody(head, bodies, cachet, props, bottom, net);
    }

    /**
     * Takes in init because there may be some dobs which don't have corresponding rules
     * @param init
     * @param rules
     *      must be in topological order
     * @param cachet
     * @return
     */
    public static PropNet buildNet(Set<Dob> init, List<Rule> rules, final Pool pool, Cachet cachet) {
        Map<Dob, Node> props = Maps.newHashMap(); // the nodes for the dobs will have to be set each turn
        Set<Node> net = Sets.newHashSet();
        Set<Node> bottom = Sets.newHashSet(); // for constructing the topological order without having to do N^2
        if (debug) {
            System.out.println("[DEBUG] Init props: " + init);
        }

        // create nodes for init
        for (Dob ground : init) {
            Preconditions.checkArgument(pool.dobs.cache.containsKey(ground.toString()));
            getNodeForProp(ground, props, bottom, net);
        }

        SetMultimap<Dob, Set<Atom>> groundings = Grounder.getValidGroundings(rules, pool, cachet);
        if (debug)
            System.out.println("[DEBUG] Done generating groundings. Building net.");

        // process the combined grounded rules
        for (Dob head : groundings.keySet()) {
            for (Set<Atom> body : groundings.get(head)) {
                List<Atom> bodies = Lists.newArrayList(body);
                processGrounding(head, bodies, cachet, props, bottom, net);
            }
        }

        List<Node> revTop = Topper.toList(Topper.topSort(toMultimap(net), bottom));
        List<Node> top = Lists.reverse(revTop);
        Preconditions.checkArgument(top.size() == net.size());
        return new PropNet(props, top);
    }

    public static ListMultimap<Node,Node> toMultimap(Set<Node> net) {
        ListMultimap<Node,Node> ret = ArrayListMultimap.create();
        for (Node node : net)
            ret.putAll(node, node.inputs);
        return ret;
    }

    public static void checkDuplicateKeys(PropNet net) {
        Multiset<String> keys = HashMultiset.create();
        for (Dob prop : net.props())
            keys.add(prop.toString());
        for (String k : keys.elementSet()) {
            Preconditions.checkArgument(keys.count(k) == 1);
        }
    }
}
