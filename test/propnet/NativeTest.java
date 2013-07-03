package propnet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import machina.PropNetStateMachine;
import org.junit.Test;
import propnet.nativecode.NativePropNet;
import propnet.nativecode.NativePropNetFactory;
import propnet.util.Tuple3;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;
import util.TestUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/13/13
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class NativeTest {

    public static int nTrial = 0;




    @Test
    public void manyNativeConnect4() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (Rule r : rules)
            System.out.println(r);
        for (int i=0; i<nTrial; i++)
            MachineTestUtil.stepThroughNative(rules);
    }

    @Test
    public void manyNativeLightsOn() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<nTrial; i++)
            MachineTestUtil.stepThroughNative(rules);
    }

    @Test
    public void nativeTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        Tuple3<String, Integer, Map<Dob, Integer>> needed = NativePropNetFactory.compileFromRules(rules);
        String fullName = needed._1;
        int size = needed._2;
        Map<Dob,Integer> propIndices = needed._3;

    }

    @Test
    public void manyNativeTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<nTrial; i++)
            MachineTestUtil.stepThroughNative(rules);
    }
}
