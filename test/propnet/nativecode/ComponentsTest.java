package propnet.nativecode;

import org.junit.*;
import propnet.vanilla.core.Node;
import propnet.vanilla.core.NodeFns;
import propnet.vanilla.PropNet;
import rekkura.logic.model.Dob;
import util.TestUtil;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:19 PM
 * Description:
 *      Tests that the components of the native net work as expected
 *      Need to set the java.library.path
 */
public class ComponentsTest {

    @BeforeClass
    public static void before() {
        NativeUtil.setupLibraryPath();
    }

    @AfterClass
    public static void after() {
        NativeUtil.deleteGeneratedFiles();
    }

    @Test
    public void testOr() {
        PropNet vanilla = TestUtil.uniformDiscVanilla(Byte.SIZE + 1, NodeFns.OR);
        Map<Node,Dob> inv = vanilla.invProps();

        Node toCheck = vanilla.tnet.get(vanilla.tnet.size()-1);
        List<Node> inputs = vanilla.tnet.subList(0,Byte.SIZE+1);
        toCheck.inputs.addAll(inputs);
        Dob toCheckProp = inv.get(toCheck);

        // compile native net
        NativePropNet net = NativePropNetFactory.getCompiledNet(NativePropNetFactory.compile(vanilla));

        // functionality tests
        net.wipe();
        net.advance();
        assertTrue("or's value is true instead of false", !net.val(toCheckProp));

        net.wipe();
        net.set(inv.get(inputs.get(0)), true);
        net.advance();
        assertTrue("or's value is false instead of true", net.val(toCheckProp));
    }

    @Test
    public void testAnd() {
        PropNet vanilla = TestUtil.uniformDiscVanilla(Byte.SIZE + 1, NodeFns.AND);
        Map<Node,Dob> inv = vanilla.invProps();

        Node toCheck = vanilla.tnet.get(vanilla.tnet.size()-1);
        List<Node> inputs = vanilla.tnet.subList(0,Byte.SIZE+1);
        toCheck.inputs.addAll(inputs);
        Dob toCheckProp = inv.get(toCheck);

        // compile native net
        NativePropNet net = NativePropNetFactory.getCompiledNet(NativePropNetFactory.compile(vanilla));

        // functionality tests
        net.wipe();
        net.set(inv.get(inputs.get(0)), true);
        net.advance();
        assertTrue("and's value is true instead of false", !net.val(toCheckProp));

        net.wipe();
        for (Node input : inputs)
            net.set(inv.get(input), true);
        net.advance();
        assertTrue("and's value is false instead of true", net.val(toCheckProp));
    }

    @Test
    public void testNot() {
        PropNet vanilla = TestUtil.uniformDiscVanilla(Byte.SIZE, NodeFns.NOT);
        Map<Node,Dob> inv = vanilla.invProps();

        Node toCheck = vanilla.tnet.get(vanilla.tnet.size()-1);
        Node input = vanilla.tnet.get(0);
        toCheck.inputs.add(input);
        Dob toCheckProp = inv.get(toCheck);
        Dob inputProp = inv.get(input);

        // compile native net
        NativePropNet net = NativePropNetFactory.getCompiledNet(NativePropNetFactory.compile(vanilla));

        // functionality tests
        net.wipe();
        net.set(inputProp, true);
        net.advance();
        assertTrue("not's value is true instead of false", !net.val(toCheckProp));

        net.wipe();
        net.advance();
        assertTrue("not's value is false instead of true", net.val(toCheckProp));

        // cleanup
    }
}
