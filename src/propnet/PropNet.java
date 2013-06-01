package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/30/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropNet {
    public List<Node> tnet; // nodes in topological order
    public Map<Dob,Node> props;
    public Set<Dob> onBases; // bases cache

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

    public void wipe() {
        for (Node node : tnet)
            node.val = false;
    }

    public void advance() {
        for (Node node : tnet)
            node.eval();
    }

    // warning: big output
    @Override public String toString() {
        System.out.println("[SUMMARY] Number of props: " + props.keySet().size());
        System.out.println("[SUMMARY] Number of nodes: " + tnet.size());
        String output = "";
        Map<Node, Dob> invProps = invProps();

        for (Dob prop : props.keySet()) {
            Node node = props.get(prop);
            if (node.inputs.isEmpty()) {
                output += "[BASE: " + prop + "]\n";
            } else {
                output += "[INTERNAL: " + prop + "]\n";

                // special case when all negative
                if (node.inputs.get(0).fn == NodeFns.NOT) {
                    output += "\t[NOT]\n";
                    Node not = node.inputs.get(0);
                    Preconditions.checkArgument(not.inputs.get(0).fn == NodeFns.OR);
                    output += "\t\t[OR]\n";
                    continue;
                }

                Preconditions.checkArgument(node.inputs.get(0).fn == NodeFns.OR);
                Node or = node.inputs.get(0);

                output += "\t[OR]\n";
                // process each and
                for (Node and : or.inputs) {
                    output += "\t\t[AND]\n";
                    for (Node andInput : and.inputs) {
                        // this is a proposition
                        if (andInput.fn == NodeFns.OR) {
                            output += "\t\t\t[" + invProps.get(andInput) + "]\n";
                        } else if (andInput.fn == NodeFns.NOT) {
                            // negative case
                            output += "\t\t\t[NOT]\n";
                            Node not = andInput;
                            Preconditions.checkArgument(not.inputs.size() == 1 && not.inputs.get(0).fn == NodeFns.OR);
                            Node notOr = andInput.inputs.get(0);
                            Preconditions.checkArgument(notOr.inputs.size() > 0);
                            output += "\t\t\t\t[OR]\n";
                            for (Node orInput : notOr.inputs) {
                                String prefix = "\t\t\t\t\t";
                                output += prefix;
                                if (invProps.get(orInput) == null)
                                    output += "[NOT GROUND]\n";
                                else
                                    output += "[" + invProps.get(orInput) + "]\n";
                            }
                        }
                    }
                }
            }
        }
        return output;
    }
}
