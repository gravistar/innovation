package propnet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/25/13
 * Time: 11:06 AM
 *
 * These are compared by reference equality.
 */
public class Node {
    public Set<Node> inputs;
    public boolean val = false;
    public ValFn<Node> fn;

    public Node (ValFn<Node> fn) {
        this(Sets.<Node>newHashSet(), fn);
    }

    public Node (Set<Node> inputs, ValFn<Node> fn) {
        this.inputs = inputs;
        this.fn = fn;
    }

    // | is because inputs may be empty and val already assigned true
    public boolean eval() {
        if (inputs.size() > 0)
            val = fn.eval(inputs);
        return val;
    }

    public static class NodeFactory {
        public static Node makeAnd(Set<Node> inputs) {
            return new Node(inputs, NodeFns.AND);
        }

        public static Node makeOr(Set<Node> inputs) {
            return new Node(inputs, NodeFns.OR);
        }

        public static Node makeNot(Set<Node> inputs) {
            return new Node(inputs, NodeFns.NOT);
        }

        public static Node makeXor(Set<Node> inputs) {
            return new Node(inputs, NodeFns.XOR);
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
