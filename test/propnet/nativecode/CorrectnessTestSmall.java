package propnet.nativecode;

import org.junit.Test;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.util.List;

/**
 * User: david
 * Date: 7/2/13
 * Time: 2:10 PM
 * Description:
 *      Correctness test for a few small games.
 */
public class CorrectnessTestSmall {

    public static int numRuns = 5;

    @Test
    public void manyButtonsLights() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules);
    }

    @Test
    public void manyTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules);
    }

    @Test
    public void manyConnect4() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules);
    }

}
