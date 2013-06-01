package propnet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/25/13
 * Time: 11:06 AM
 *
 * These are compared by reference equality.
 */
public class Node {
    public List<Node> inputs;
    public boolean val = false;
    public ValFn<Node> fn;

    public Node (ValFn<Node> fn) {
        this(Lists.<Node>newArrayList(), Lists.<Node>newArrayList(), fn);
    }

    public Node (List<Node> inputs, Collection<Node> outputs, ValFn<Node> fn) {
        this.inputs = inputs;
        this.fn = fn;
    }

    public boolean eval() {
        return val = fn.eval(inputs);
    }

    public static class NodeFactory {
        public static Node makeAnd(List<Node> inputs, List<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.AND);
        }

        public static Node makeOr(List<Node> inputs, List<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.OR);
        }

        public static Node makeNot(List<Node> inputs, List<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.NOT);
        }

        public static Node makeXor(List<Node> inputs, List<Node> outputs) {
            return new Node(inputs, outputs, NodeFns.XOR);
        }

        public static Node makeAnd() {
            return new Node(NodeFns.AND);
        }

        public static Node makeOr() {
            return new Node(NodeFns.OR);
        }

        public static Node makeNot() {
            return new Node(NodeFns.NOT);
        }

        public static Node makeXor() {
            return new Node(NodeFns.XOR);
        }

    }
}