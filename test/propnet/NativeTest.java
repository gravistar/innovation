package propnet;

import org.junit.Test;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/13/13
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class NativeTest {

    @Test
    public void nativeTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        PropNet net = PropNetFactory.createFromRules(rules);
        NativePropNet nativeNet = new NativePropNet(net.invProps(), net.tnet);
    }

    @Test
    public void manyTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThrough(rules);
    }
}
