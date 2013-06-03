package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.state.algorithm.Topper;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/25/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetFactory {

    public static boolean debug = false;

    public static PropNet createFromRules(List<Rule> rules) {
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
        if (debug)
            System.out.println("[INIT GROUNDS] " + initGrounds);

        Cachet cachet = new Cachet(ruletta);
        cachet.storeAllGround(initGrounds);
        PropNet net = PropNetFactory.buildNet(ruletta, cachet);
        return net;
    }

    /**
     * @param rule
     * @param bodyGrounding
     *      elements correspond to order in rule.body
     * @return
     */
    public static List<Dob> filterPositiveGrounds(Rule rule, List<Dob> bodyGrounding) {
        Preconditions.checkArgument(rule.body.size() == bodyGrounding.size());
        List<Dob> positiveGrounds = Lists.newArrayList();
        for (int i=0; i<rule.body.size(); i++)
            if (rule.body.get(i).truth)
                positiveGrounds.add(bodyGrounding.get(i));
        return positiveGrounds;
    }

    /**
     * @param rule
     * @param bodyGrounding
     *      elements correspond to order in rule.body
     * @return
     */
    public static List<Dob> filterNegativeGrounds(Rule rule, List<Dob> bodyGrounding) {
        return Lists.newArrayList(Colut.difference(bodyGrounding, filterPositiveGrounds(rule, bodyGrounding)));
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

    public static Node createAndAddAND(List<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeAnd(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static Node createAndAddNOT(List<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeNot(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static Node createAndAddOR(List<Node> inputs, Set<Node> bottom, Set<Node> net) {
        Node ret = Node.NodeFactory.makeOr(inputs);
        bottom.removeAll(inputs);
        net.add(ret);
        return ret;
    }

    public static void negBodyRulePrecon(Atom head, ImmutableList<Atom> body, Cachet cachet) {
        Preconditions.checkArgument(cachet.unisuccess.containsKey(head.dob));
        Preconditions.checkArgument(cachet.unisuccess.get(head.dob).contains(head.dob));
        Preconditions.checkArgument(cachet.unisuccess.get(head.dob).size() == 1);

        for (Atom bodyTerm : body) {
            // all bodies are negative and grounded
            Preconditions.checkArgument(!bodyTerm.truth);
            Preconditions.checkArgument(cachet.unisuccess.containsKey(bodyTerm.dob));
            Preconditions.checkArgument(cachet.unisuccess.get(bodyTerm.dob).contains(bodyTerm.dob));
            Preconditions.checkArgument(cachet.unisuccess.get(bodyTerm.dob).size() == 1);
        }
    }

    public static void attachInput(Node node, Node input, Set<Node> bottom) {
        node.inputs.add(input);
        bottom.remove(input);
    }

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

    public static void processAllNegativeBody(Atom head, ImmutableList<Atom> body,
                                              Cachet cachet,
                                              Map<Dob, Node> props,
                                              Set<Node> bottom,
                                              Set<Node> net) {
        negBodyRulePrecon(head, body, cachet);

        List<Node> negPropNodes = Lists.newArrayList();
        for (Atom bodyTerm : body)
            negPropNodes.add(getNodeForProp(bodyTerm.dob, props, bottom, net));

        Node orNode = createAndAddOR(negPropNodes, bottom, net);
        Node notNode = createAndAddNOT(Lists.newArrayList(orNode), bottom, net);

        Node headNode = getNodeForProp(head.dob, props, bottom, net);
        // generate a bigOR that's the sole input to headNode if not done already
        if (headNode.inputs.isEmpty())
            attachInput(headNode, createAndAddOR(Lists.<Node>newArrayList(), bottom, net), bottom);

        Node bigOR = headNode.inputs.get(0);
        attachInput(bigOR, notNode, bottom);
    }

    public static void processPositiveBodyGrounding(List<Dob> posBodyGrounding,
                                                    Rule rule, Pool pool,
                                                    Cachet cachet,
                                                    Map<Dob, Node> props,
                                                    Set<Node> bottom,
                                                    Set<Node> net) {
        // generate head grounding
        Dob headGrounding = Terra.applyBodies(rule, posBodyGrounding, Sets.<Dob>newHashSet(), pool);
        if (debug) {
                System.out.println("[DEBUG] head grounding: " + headGrounding + " body " + posBodyGrounding);
        }
        if (headGrounding == null)
            return;

        // store head grounding in cachet??
        cachet.storeGround(headGrounding);

        // generate node for head grounding
        Node headNode = getNodeForProp(headGrounding, props, bottom, net);

        // generate a bigOR that's the sole input to headNode if not done already
        if (headNode.inputs.isEmpty())
            attachInput(headNode, createAndAddOR(Lists.<Node>newArrayList(), bottom, net), bottom);

        Node bigOR = headNode.inputs.get(0);

        // get the unification of all vars in rule
        // includes those in negative terms by datalog definition
        // valid since applyBodies passed
        Map<Dob,Dob> posUnitedUnify = Unifier.unifyListVars(
                Atom.asDobList(Atom.filterPositives(rule.body)),
                posBodyGrounding,
                rule.vars);

        List<Dob> negBodyGroundings = Lists.newArrayList();
        for (Atom negTerm : Atom.filterNegatives(rule.body)) {
            // this better not be null...
            negBodyGroundings.add(
                    Preconditions.checkNotNull(
                            pool.dobs.submerge(Unifier.replace(negTerm.dob, posUnitedUnify))));
        }

        List<Node> posNodes = Lists.newArrayList();
        List<Node> negNodes = Lists.newArrayList();

        for (Dob pos : posBodyGrounding)
            posNodes.add(getNodeForProp(pos, props, bottom, net));

        if (debug)
            System.out.println("[DEBUG] number of pos nodes " + posNodes.size());

        for (Dob neg : negBodyGroundings)
            negNodes.add(getNodeForProp(neg, props, bottom, net));

        // create AND for this set of body groundings
        Node AND = createAndAddAND(posNodes, bottom, net);

        // squash nots and add as inputs to the and
        if (negNodes.isEmpty() == false) {
            Node negOR = createAndAddOR(negNodes, bottom, net);
            Node negNOT = createAndAddNOT(Lists.<Node>newArrayList(negOR), bottom, net);

            // add to posAND
            attachInput(AND, negNOT, bottom);
        }

        // add to bigOR inputs
        attachInput(bigOR, AND, bottom);
    }

    public static PropNet buildNet(Ruletta ruletta, Cachet cachet) {
        Map<Dob, Node> props = Maps.newHashMap(); // the nodes for the dobs will have to be set each turn
        Set<Node> net = Sets.newHashSet();
        Set<Node> bottom = Sets.newHashSet(); // for constructing the topological order without having to do N^2
        Pool pool = ruletta.fortre.pool;
        List<Rule> rules = Topper.toList(ruletta.ruleOrder);

        for (Rule rule : rules) {
            if (debug)
            System.out.println("[DEBUG] Processing rule: " + rule);
            Atom head = rule.head;
            ImmutableList<Atom> body = rule.body;

            // what should cachet be expected to have??
            ListMultimap<Atom,Dob> bodySpace = Terra.getBodySpace(rule, cachet);
            List<Atom> posBodyTerms = Atom.filterPositives(body);

            // get the positive body space as lists in order they appear in body
            List<List<Dob>> posBodySpace = Lists.newArrayList();
            for (Atom bodyTerm : posBodyTerms)
                posBodySpace.add(bodySpace.get(bodyTerm));


            long tc = 1;
            for (int i=0; i<posBodySpace.size(); i++) {
                List<Dob> space = posBodySpace.get(i);
                tc *= space.size();
            }

            if (debug) {
                System.out.println("[DEBUG] true count " + tc);
                System.out.println("[DEBUG] body space " + bodySpace);
            }

            Iterable<List<Dob>> posBodyGroundings = Cartesian.asIterable(posBodySpace);
            int nPosBodyGroundings = 0;

            for (List<Dob> posBodyGrounding : posBodyGroundings) {
                processPositiveBodyGrounding(posBodyGrounding, rule, pool, cachet, props, bottom, net);
                nPosBodyGroundings++;
            }

            if (nPosBodyGroundings == 0) {
                if (!body.isEmpty())
                    // rule with only negative grounded bodies
                    processAllNegativeBody(head, body, cachet, props, bottom, net);
                else
                    // base case
                    getNodeForProp(head.dob, props, bottom, net);
            }

            if (debug)
                System.out.println();

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
}
