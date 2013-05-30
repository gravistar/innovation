package propnet;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/25/13
 * Time: 11:06 AM
 *
 * These are compared by reference equality.
 *
 */
public class Node {
    public Collection<Node> inputs, outputs;
    public boolean val = false;
    public ValFn<Node> fn;

    public Node (Collection<Node> inputs, Collection<Node> outputs, ValFn<Node> fn) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.fn = fn;
    }

    public boolean source() {
        return inputs.isEmpty();
    }

    public boolean sink() {
        return outputs.isEmpty();
    }

    public boolean eval() {
        return val = fn.eval(inputs);
    }


    public static class NodeFactory {
        public static Node makeAnd(Collection<Node> inputs, Collection<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.AND);
        }

        public static Node makeOr(Collection<Node> inputs, Collection<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.OR);
        }

        public static Node makeNot(Collection<Node> inputs, Collection<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.NOT);
        }

        public static Node makeXor(Collection<Node> inputs, Collection<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.XOR);
        }

    }
}
