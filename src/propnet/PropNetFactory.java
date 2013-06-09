package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import propnet.util.FilteringCartesianIterator;
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

    /**
     * @param rules
     * @return
     */
    public static PropNet createFromRules(List<Rule> rules) {
        BackwardStateMachine machine = BackwardStateMachine.createForRules(rules);
        Set<Dob> initGrounds = PropNetFactory.prepareMachine(machine);
        Cachet cachet = new Cachet(machine.rta);
        cachet.storeAllGround(initGrounds);
        List<Rule> topRuleOrder = Topper.toList(machine.rta.ruleOrder);
        PropNet net = PropNetFactory.buildNet(initGrounds, topRuleOrder, machine.rta.fortre.pool, cachet);
        return net;
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

    public static boolean badRuleHead(Rule rule) {
        String name = rule.head.dob.toString();
        return name.startsWith("((base)") || name.startsWith("((input)");
    }

    public static List<Dob> posBodies(Rule rule) {
        List<Dob> ret = Lists.newArrayList();
        for (Atom body : Atom.filterPositives(rule.body))
            ret.add(body.dob);
        return ret;
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

    public static Rule cleanRule (Rule rule, GameLogicContext context) {
        return Unifier.replace(rule, context.INPUT_UNIFY, rule.vars);
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
        SetMultimap<Dob, Set<Atom>> groundings = HashMultimap.create(); // head -> all atoms
                                                                   // combines bodies of rules together
        if (debug) {
            System.out.println("[DEBUG] Init props: " + init);
        }

        // create nodes for init
        for (Dob ground : init) {
            Preconditions.checkArgument(pool.dobs.cache.containsKey(ground.toString()));
            getNodeForProp(ground, props, bottom, net);
        }

        // now process the rules
        for (Rule rule : rules) {
            if (debug)
                System.out.println("[DEBUG] Processing rule: " + rule);

            // already grounded?
            if (rule.vars.isEmpty()) {
                Dob head = rule.head.dob;
                Preconditions.checkArgument(pool.dobs.cache.containsKey(head.toString()));
                for (Atom body : rule.body) {
                    Preconditions.checkArgument(pool.atoms.cache.containsKey(body.toString()));
                    Preconditions.checkArgument(pool.dobs.cache.containsKey(body.dob.toString()));
                }
                groundings.put(head, Sets.newHashSet(rule.body));
                continue;
            }

            // now ground the rule
            List<Atom> body = rule.body;

            // what should cachet be expected to have??
            ListMultimap<Atom,Dob> bodySpace = Terra.getBodySpace(rule, cachet);
            List<Atom> posBodyTerms = Atom.filterPositives(body);

            // get the positive body space as lists in order they appear in body
            List<List<Dob>> posBodySpace = Lists.newArrayList();
            for (Atom bodyTerm : posBodyTerms)
                posBodySpace.add(bodySpace.get(bodyTerm));

            // unification of positive body
            SetMultimap<Dob,Dob> unitedUnification = unitedUnification(rule, posBodySpace);
            final List<Dob> vars = Lists.newArrayList();
            for (Dob v : rule.vars)
                if (unitedUnification.containsKey(v))
                    vars.add(v);

            List<List<Dob>> unitedAsList = Lists.newArrayList();
            for (Dob var : vars)
                unitedAsList.add(Lists.newArrayList(unitedUnification.get(var)));

            final SetMultimap<Dob,Dob> groundedBy = getGroundedBy(rule, vars);

            final Rule fr = rule;
            FilteringCartesianIterator.FilterFn<Dob> unificationFilter = new FilteringCartesianIterator.FilterFn<Dob>() {
                // IMPORTANT! current must be in the same order as vars.
                @Override
                public boolean pred(List<Dob> current, Dob x) {

                    // no assignment to filter by
                    if (current.isEmpty())
                        return true;
                    // get old variable assignments
                    Map<Dob,Dob> unify = Maps.newHashMap();
                    for (int i=0; i<current.size(); i++)
                        unify.put(vars.get(i), current.get(i));
                    // new assignment
                    Dob lastVar = vars.get(current.size());
                    unify.put(lastVar, x);

                    // verify unification
                    if (!fr.evaluateDistinct(unify))
                        return false;

                    Set<Dob> bodiesToGround = groundedBy.get(lastVar);
                    for (Dob bodyToGround : bodiesToGround) {
                        Dob grounded = Unifier.replace(bodyToGround, unify);
                        if (!(pool.dobs.cache.containsKey(grounded.toString())))
                            return false;
                    }

                    return true;
                }
            };

            Iterable<List<Dob>> filteredUnifications = new FilteringCartesianIterator.FilteringCartesianIterable<Dob>(
                                                        unitedAsList, unificationFilter);

            int count = 0;
            for (List<Dob> unifyAsList : filteredUnifications) {
                count++;
                if (debug) {
                    if (count % 10000 == 0)
                        System.out.println("[DEBUG] Done processing " + count + " unifications");
                }
                Preconditions.checkArgument(unifyAsList.size() == vars.size());
                // create unification
                Map<Dob,Dob> unify = Maps.newHashMap();
                for (int i=0; i<vars.size(); i++)
                    unify.put(vars.get(i), unifyAsList.get(i));

                Rule grounded = Unifier.replace(rule, unify, Sets.<Dob>newHashSet());
                Preconditions.checkArgument(grounded.vars.isEmpty());
                Dob head = getSubmergedGround(grounded.head.dob, pool, cachet);
                List<Atom> submergedBodies = Lists.newArrayList();
                // submerge the atom dobs and make new bodies
                for (Atom b : grounded.body) {
                    Dob bd = getSubmergedGround(b.dob, pool, cachet);
                    Atom newbody = getSubmergedAtom(new Atom(bd, b.truth), pool);
                    Preconditions.checkNotNull(newbody);
                    submergedBodies.add(newbody);
                }


                groundings.put(head, Sets.newHashSet(submergedBodies));
            }

            if (debug)
                System.out.println();
        }

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

    /**
     * Returns the submerged version if there is one. Otherwise, submerges the dob
     * and returns it.
     * @param dob
     * @param pool
     * @param cachet
     * @return
     */
    public static Dob getSubmergedGround(Dob dob, Pool pool, Cachet cachet) {
        String key = dob.toString();
        if (pool.dobs.cache.containsKey(key))
            return pool.dobs.cache.get(key);
        cachet.storeGround(dob);
        return pool.dobs.submerge(dob);
    }

    public static Atom getSubmergedAtom(Atom atom, Pool pool) {
        Preconditions.checkArgument(pool.dobs.cache.containsKey(atom.dob.toString()));
        String key = atom.toString();
        if (pool.atoms.cache.containsKey(key))
            return pool.atoms.cache.get(key);
        return pool.atoms.submerge(atom);
    }


    /**
     * Returns a multimap where the key is a body term in rule.  The corresponding values
     * is are the variables v_j such that if variables v_1...v_j are assigned, the key
     * is grounded. Note that if v_j is a value, v_k for k>j is also in there.
     * @param rule
     * @param vars
     *      variables of rule in order v_1...v_n
     * @return
     */
    public static SetMultimap<Dob,Dob> getGroundedBy(Rule rule, List<Dob> vars) {
        SetMultimap<Dob,Dob> ret = HashMultimap.create();
        Set<Dob> processedVars = Sets.newHashSet();
        Set<Dob> allVars = Sets.newHashSet(vars);
        for (Dob var : vars) {
            processedVars.add(var);
            for (Atom bodyTerm : rule.body) {
                boolean grounded = true;
                for (Dob bodyTermDob : bodyTerm.dob.fullIterable())
                    if (!processedVars.contains(bodyTermDob) && allVars.contains(bodyTermDob)) {
                        grounded = false;
                        break;
                    }
                if (grounded)
                    ret.put(var, bodyTerm.dob);
            }
        }
        return ret;
    }

    // united unifications
    // key: variable, value: grounding
    public static SetMultimap<Dob,Dob> unitedUnification(Rule rule, List<List<Dob>> posBodySpace) {
        SetMultimap<Dob,Dob> ret = HashMultimap.create();
        List<Dob> posBodies = posBodies(rule);
        Preconditions.checkArgument(posBodies.size() == posBodySpace.size());
        for (int i=0; i<posBodies.size(); i++) {
            Dob ungrounded = posBodies.get(i);
            for (Dob ground : posBodySpace.get(i)) {
                Map<Dob,Dob> unification = Unifier.unify(ungrounded, ground);
                if (unification == null)
                    continue;
                for (Dob var : unification.keySet())
                    if (var.name.startsWith("?"))
                        ret.put(var, unification.get(var));
            }
        }
        return ret;
    }

    public static ListMultimap<Node,Node> toMultimap(Set<Node> net) {
        ListMultimap<Node,Node> ret = ArrayListMultimap.create();
        for (Node node : net)
            ret.putAll(node, node.inputs);
        return ret;
    }

    public static void checkDuplicateKeys(PropNet net) {
        Multiset<String> keys = HashMultiset.create();
        for (Dob prop : net.props.keySet())
            keys.add(prop.toString());
        for (String k : keys.elementSet()) {
            Preconditions.checkArgument(keys.count(k) == 1);
        }

    }

}
