package propnet.vanilla;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import propnet.PropNetInterface;
import propnet.vanilla.core.Node;
import propnet.vanilla.core.NodeFns;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 5/30/13
 * Time: 10:17 AM
 * Description:
 *      A vanilla, in-Java propnet
 */
public class PropNet implements PropNetInterface {
    public List<Node> tnet; // nodes in topological order
    public Map<Dob,Node> props;

    public PropNet(Map<Dob,Node> props, List<Node> net) {
        this.tnet = net;
        this.props = props;
    }

    public Map<Node,Dob> invProps() {
        Map<Node,Dob> ret = Maps.newHashMap();
        for (Dob prop : props.keySet()) {
            Node node = props.get(prop);
            Preconditions.checkArgument(!ret.containsKey(node));
            ret.put(node, prop);
        }
        return ret;
    }

    public Map<Node,Integer> indices() {
        Map<Node,Integer> ret = Maps.newHashMapWithExpectedSize(tnet.size());
        for (int i=0; i<tnet.size(); i++)
            ret.put(tnet.get(i), i);
        return ret;
    }

    @Override
    public void wipe() {
        for (Node node : tnet)
            node.val = false;
    }

    @Override
    public void advance() {
        for (Node node : tnet)
            node.eval();
    }

    @Override
    public Set<Dob> props() {
        return props.keySet();
    }

    @Override
    public boolean val(Dob prop) {
        return props.get(prop).val;
    }

    @Override
    public void set(Dob prop, boolean val) {
        props.get(prop).val = val;
    }

    @Override
    public long size() {
        return tnet.size();
    }



    // not ALWAYS has an or as input
    public StringBuilder notString(Node not, Map<Node, Dob> invProps, int nTab) {
        StringBuilder prefixBuilder = new StringBuilder();
        for (int i=0; i<nTab; i++)
            prefixBuilder.append("\t");
        StringBuilder outBuilder = new StringBuilder();
        // negative case
        outBuilder.append(prefixBuilder).append("[NOT]");
        outBuilder.append(" val: ").append(not.val).append("\n");
        Preconditions.checkArgument(not.inputs.size() == 1 && not.inputs.iterator().next().fn == NodeFns.OR);
        Node notOr = not.inputs.iterator().next();
        Preconditions.checkArgument(notOr.inputs.size() > 0);
        outBuilder.append(prefixBuilder).append("\t[OR]").append(" val: ").append(notOr.val).append("\n");
        for (Node orInput : notOr.inputs) {
            if (invProps.get(orInput) == null)
                outBuilder.append(prefixBuilder).append("\t\t[NOT GROUND]").append(" val: ").append(orInput.val).append("\n");
            else
                outBuilder.append(prefixBuilder).append("\t\t[").append(invProps.get(orInput)).append("] val: ").append(orInput.val).append("\n");
        }
        return outBuilder;
    }


    // warning: big output
    @Override public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("[SUMMARY] Number of props: ").append(props.keySet().size()).append('\n');
        output.append("[SUMMARY] Number of nodes: ").append(tnet.size()).append('\n');
        Map<Node, Dob> invProps = invProps();

        for (Dob prop : props.keySet()) {
            Node node = props.get(prop);
            if (node.inputs.isEmpty()) {
                output.append("[BASE: ").append(prop).append(" val: ").append(node.val).append("]\n");
            } else {
                output.append("[INTERNAL: ").append(prop).append(" val: ").append(node.val).append("]\n");

                // special case when all negative
                if (node.inputs.iterator().next().fn == NodeFns.NOT) {
                    Node not = node.inputs.iterator().next();
                    output.append(notString(not, invProps, 1));
                    continue;
                }

                Preconditions.checkArgument(node.inputs.iterator().next().fn == NodeFns.OR);
                Node or = node.inputs.iterator().next();

                // otherwise, it HAS to be an OR
                output.append("\t[OR]").append(" val: ").append(or.val).append("\n");
                // process each and
                for (Node orIn : or.inputs) {

                    if (orIn.fn == NodeFns.AND) {
                        Node and = orIn;
                        output.append("\t\t[AND]").append(" val: ").append(and.val).append("\n");
                        for (Node andInput : and.inputs) {
                            // this is a proposition
                            if (andInput.fn == NodeFns.OR) {
                                output.append("\t\t\t[").append(invProps.get(andInput)).append(" val: ").append(andInput.val).append("]\n");
                            } else if (andInput.fn == NodeFns.NOT) {
                                // negative case
                                Node not = andInput;
                                output.append(notString(not, invProps, 3));
                            }
                        }
                    }

                    // can happen (negative grounded body as input)
                    if (orIn.fn == NodeFns.NOT) {
                        Node not = orIn;
                        output.append(notString(not, invProps, 2));
                    }
                }
            }
        }
        return output.toString();
    }
}
