package propnet.util;

import com.google.common.base.Preconditions;
import propnet.vanilla.core.Node;
import propnet.vanilla.core.NodeFns;
import propnet.nativecode.NativePropNet;
import rekkura.logic.model.Dob;

import java.util.Map;

/**
 * User: david
 * Date: 6/18/13
 * Time: 8:31 AM
 * Description:
 *      A utility for printing out a native propnet in a readable format.
 */
public class PrintUtil {

    public static String getValNative(Node n, Map<Node, Integer> indices, NativePropNet net) {
        int idx = indices.get(n);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        byte a = net.net.get(bi);
        byte b = net.net.get(bi);
        Preconditions.checkArgument(a == b);
        return ((a & (1<<bp)) != 0) + " actual: " + a + " bp: " + (1<<bp) + " ";
    }

    // not ALWAYS has an or as input
    public static StringBuilder notStringNative(Node not, Map<Node, Dob> invProps, int nTab,
                                          Map<Node,Integer> indices, NativePropNet net) {
        StringBuilder prefixBuilder = new StringBuilder();
        for (int i=0; i<nTab; i++)
            prefixBuilder.append("\t");
        StringBuilder outBuilder = new StringBuilder();
        // negative case
        outBuilder.append(prefixBuilder).append("[NOT]");
        Preconditions.checkArgument(not.inputs.size() == 1 && not.inputs.iterator().next().fn == NodeFns.OR);
        Node notOr = not.inputs.iterator().next();
        outBuilder.append(" val: ").append(getValNative(notOr, indices, net)).append(buildBlock(indices.get(not))).append("\n");
        Preconditions.checkArgument(notOr.inputs.size() > 0);
        outBuilder.append(prefixBuilder).append("\t[OR]").append(" val: ").append(getValNative(notOr, indices, net))
                .append(buildBlock(indices.get(notOr))).append("\n");
        for (Node orInput : notOr.inputs) {
            if (invProps.get(orInput) == null)
                outBuilder.append(prefixBuilder).append("\t\t[NOT GROUND]").append(" val: ").append(getValNative(orInput, indices, net))
                        .append(buildBlock(indices.get(orInput))).append("\n");
            else
                outBuilder.append(prefixBuilder).append("\t\t[").append(invProps.get(orInput)).append("] val: ").append(getValNative(orInput, indices, net))
                        .append(buildBlock(indices.get(orInput))).append("\n");
        }
        return outBuilder;
    }

    public static StringBuilder buildBlock(int idx) {
        StringBuilder b = new StringBuilder();
        b.append(" block: ").append(idx).append(" bid: ").append(idx / Byte.SIZE).append(" pos: ").append(idx % Byte.SIZE);
        return b;
    }


    // warning: big output
    public static String printNativeNet(Map<Dob,Node> props, Map<Node,Dob> invProps, Map<Node,Integer> indices, NativePropNet net) {
        StringBuilder output = new StringBuilder();
        output.append("[SUMMARY] Number of props: ").append(props.keySet().size()).append('\n');

        for (Dob prop : props.keySet()) {
            Node node = props.get(prop);
            if (node.inputs.isEmpty()) {
                output.append("[BASE: ").append(prop).append(" val: ").append(getValNative(node, indices, net)).append(buildBlock(indices.get(node))).append("]\n");
            } else {
                output.append("[INTERNAL: ").append(prop).append(" val: ").append(getValNative(node, indices, net)).append(buildBlock(indices.get(node))).append("]\n");

                // special case when all negative
                if (node.inputs.iterator().next().fn == NodeFns.NOT) {
                    Node not = node.inputs.iterator().next();
                    output.append(notStringNative(not, invProps, 1, indices, net));
                    continue;
                }

                Preconditions.checkArgument(node.inputs.iterator().next().fn == NodeFns.OR);
                Node or = node.inputs.iterator().next();

                // otherwise, it HAS to be an OR
                output.append("\t[OR]").append(" val: ").append(getValNative(or, indices, net)).append(buildBlock(indices.get(or))).append("\n");
                // process each and
                for (Node orIn : or.inputs) {

                    if (orIn.fn == NodeFns.AND) {
                        Node and = orIn;
                        output.append("\t\t[AND]").append(" val: ").append(getValNative(and, indices, net)).append(buildBlock(indices.get(and))).append("\n");
                        for (Node andInput : and.inputs) {
                            // this is a proposition
                            if (andInput.fn == NodeFns.OR) {
                                output.append("\t\t\t[").append(invProps.get(andInput)).append(" val: ").append(getValNative(andInput, indices, net))
                                        .append(buildBlock(indices.get(andInput))).append("]\n");
                            } else if (andInput.fn == NodeFns.NOT) {
                                // negative case
                                Node not = andInput;
                                output.append(notStringNative(not, invProps, 3, indices, net));
                            }
                        }
                    }

                    // can happen (negative grounded body as input)
                    if (orIn.fn == NodeFns.NOT) {
                        Node not = orIn;
                        output.append(notStringNative(not, invProps, 2, indices, net));
                    }
                }
            }
        }
        return output.toString();
    }


}
