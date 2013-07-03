package propnet.vanilla;

import org.junit.Test;
import propnet.Node;

import static org.junit.Assert.assertTrue;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:11 PM
 * Description:
 *      Just tests the components of the propnet
 */
public class ComponentsTest {
    @Test
    public void notTest() {
        Node a,b;
        a = Node.NodeFactory.makeNot();
        b = Node.NodeFactory.makeNot();

        // connect
        b.inputs.add(a);

        a.val = true;
        b.eval();
        assertTrue("b is true but should be false", !b.val);

        a.val = false;
        b.eval();
        assertTrue("b is false but should be true", b.val);
    }

    @Test
    public void orTest() {
        Node a,b,c;
        a = Node.NodeFactory.makeOr();
        b = Node.NodeFactory.makeOr();
        c = Node.NodeFactory.makeOr();

        // connect
        c.inputs.add(a);
        c.inputs.add(b);

        a.val = true;
        b.val = true;
        c.eval();
        assertTrue("c is false when should be true", c.val);

        a.val = false;
        c.eval();
        assertTrue("c is false when should be true", c.val);

        b.val = false;
        c.eval();
        assertTrue("c is true when should be false", !c.val);
    }

    @Test
    public void andTest() {
        Node a,b,c;
        a = Node.NodeFactory.makeAnd();
        b = Node.NodeFactory.makeAnd();
        c = Node.NodeFactory.makeAnd();

        // connect
        c.inputs.add(a);
        c.inputs.add(b);

        a.val = true;
        b.val = true;
        c.eval();
        assertTrue("c is false but should be true", c.val);

        a.val = true;
        c.eval();
        assertTrue("c is true but should be false", !c.val);
    }
}
