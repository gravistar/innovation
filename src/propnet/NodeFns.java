package propnet;

import java.util.Collection;
import java.util.Iterator;

/**
* Created with IntelliJ IDEA.
* User: david
* Date: 5/25/13
* Time: 12:42 PM
* To change this template use File | Settings | File Templates.
*/
public class NodeFns {
    public static class andFn implements ValFn<Node> {
        public boolean eval(Collection<Node> inputs) {
            for (Node input : inputs)
                if (!input.val)
                    return false;
            return true;
        }
    }

    public static class orFn implements ValFn<Node> {
        public boolean eval(Collection<Node> inputs) {
            for (Node input : inputs)
                if (input.val)
                    return true;
            return false;
        }
    }

    // pretty unsafe. do the checking outside
    public static class notFn implements ValFn<Node> {
        public boolean eval(Collection<Node> inputs) {
            return !(inputs.iterator().next().val);
        }
    }

    // why not
    public static class xorFn implements ValFn<Node> {
        public boolean eval(Collection<Node> inputs) {
            Iterator<Node> it = inputs.iterator();
            Node first = it.next();
            Node second = it.next();
            return first.val ^ second.val;
        }
    }
    public static ValFn<Node> AND = new andFn();
    public static ValFn<Node> OR = new orFn();
    public static ValFn<Node> NOT = new notFn();
    public static ValFn<Node> XOR = new xorFn();
}
