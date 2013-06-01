package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.algorithm.Unifier;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.state.algorithm.Topper;
import rekkura.util.Cartesian;
import rekkura.util.Colut;

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
    public static Node getNodeForProp(Dob ground, Map<Dob,Node> props, Set<Node> net) {

        // props are always ands
        Node node = props.containsKey(ground) ? props.get(ground) :
                Node.NodeFactory.makeOr();

        props.put(ground, node);
        net.add(node);
        return node;
    }

    public static Node createAndAddAND(List<Node> inputs, Set<Node> net) {
        Node ret = Node.NodeFactory.makeAnd(inputs, Lists.<Node>newArrayList());
        net.add(ret);
        return ret;
    }

    public static Node createAndAddNOT(List<Node> inputs, Set<Node> net) {
        Node ret = Node.NodeFactory.makeNot(inputs, Lists.<Node>newArrayList());
        net.add(ret);
        return ret;
    }

    public static Node createAndAddOR(List<Node> inputs, Set<Node> net) {
        Node ret = Node.NodeFactory.makeOr(inputs, Lists.<Node>newArrayList());
        net.add(ret);
        return ret;
    }

    public static void processAllNegativeBody(Rule rule, Cachet cachet, Map<Dob, Node> props, Set<Node> net) {
        processAllNegativeBody(rule.head, rule.body, cachet, props, net);
    }

    public static void negBodyRulePrecon(Atom head, ImmutableList<Atom> body, Cachet cachet) {
        // head is grounded
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

    public static void processAllNegativeBody(Atom head, ImmutableList<Atom> body,
                                              Cachet cachet, Map<Dob, Node> props, Set<Node> net) {
        negBodyRulePrecon(head, body, cachet);

        List<Node> negPropNodes = Lists.newArrayList();
        for (Atom bodyTerm : body)
            negPropNodes.add(getNodeForProp(bodyTerm.dob, props, net));

        Node orNode = createAndAddOR(negPropNodes, net);
        Node notNode = createAndAddNOT(Lists.newArrayList(orNode), net);
        Node headNode = getNodeForProp(head.dob, props, net);
        headNode.inputs.add(notNode);
    }

    public static void processPositiveBodyGrounding(List<Dob> posBodyGrounding, Rule rule, Pool pool,
                                                    Map<Dob, Node> props, Set<Node> net) {
        // generate head grounding
        Dob headGrounding = Terra.applyBodies(rule, posBodyGrounding, Sets.<Dob>newHashSet(), pool);
        if (headGrounding == null)
            return;

        // generate node for head grounding
        Node headNode = getNodeForProp(headGrounding, props, net);

        // generate a bigOR that's the sole input to headNode if not done already
        if (headNode.inputs.isEmpty())
            headNode.inputs.add(Node.NodeFactory.makeOr());

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
            posNodes.add(getNodeForProp(pos, props, net));

        for (Dob neg : negBodyGroundings)
            negNodes.add(getNodeForProp(neg, props, net));

        // create AND for this set of body groundings
        Node AND = createAndAddAND(posNodes, net);

        // squash nots and add as inputs to the and
        if (negNodes.isEmpty() == false) {
            Node negOR = createAndAddOR(negNodes, net);
            Node negNOT = createAndAddNOT(Lists.<Node>newArrayList(negOR), net);

            // add to posAND
            AND.inputs.add(negNOT);
        }

        // add to bigOR inputs
        bigOR.inputs.add(AND);
    }

    public static PropNet buildNet(Ruletta ruletta, Cachet cachet) {
        Map<Dob, Node> props = Maps.newHashMap(); // the nodes for the dobs will have to be set each turn
        Set<Node> net = Sets.newHashSet();
        Pool pool = ruletta.fortre.pool;
        List<Rule> rules = Topper.toList(ruletta.ruleOrder);

        for (Rule rule : rules) {
            Atom head = rule.head;
            ImmutableList<Atom> body = rule.body;

            // what should cachet be expected to have??
            ListMultimap<Atom,Dob> bodySpace = Terra.getBodySpace(rule, cachet);
            List<Atom> posBodyTerms = Atom.filterPositives(body);

            // get the positive body space as lists in order they appear in body
            List<List<Dob>> posBodySpace = Lists.newArrayList();
            for (Atom bodyTerm : posBodyTerms)
                posBodySpace.add(bodySpace.get(bodyTerm));

            Iterable<List<Dob>> posBodyGroundings = Cartesian.asIterable(posBodySpace);
            int nPosBodyGroundings = 0;
            for (List<Dob> posBodyGrounding : posBodyGroundings) {
                processPositiveBodyGrounding(posBodyGrounding, rule, pool, props, net);
                nPosBodyGroundings++;
            }

            // rule with only negative grounded bodies
            if (nPosBodyGroundings == 0 && !body.isEmpty())
                processAllNegativeBody(head, body, cachet, props, net);
        }

        return new PropNet(props, net);
    }
}
