package propnet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Ruletta;
import rekkura.state.algorithm.Topper;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/25/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetFactory {

    public Map<Dob, Node> props; // the nodes for the dobs will have to be set each turn

    public static void buildNet(Ruletta ruletta, Cachet cachet) {
        List<Rule> rules = Topper.toList(ruletta.ruleOrder);

        for (Rule rule : rules) {
            Atom head = rule.head;
            ImmutableList<Atom> body = rule.body;

            Iterable<Dob> headSpace = Terra.getGroundCandidates(head.dob, cachet);
            ListMultimap<Atom,Dob> bodySpace = Terra.getBodySpace(rule, cachet);

            // and together
            for (Dob headGround : headSpace) {
                List<Node> toAnd = Lists.newArrayList();
                for (Atom bodyTerm : bodySpace.keySet()) {

                }
            }

        }


    }



}
