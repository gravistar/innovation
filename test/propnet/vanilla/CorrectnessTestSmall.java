package propnet.vanilla;

import org.junit.Test;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.util.List;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:16 PM
 * Description:
 *      Correctness test for a few small games
 */
public class CorrectnessTestSmall {
    @Test
    public void manyButtonsLights() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules);
    }

    @Test
    public void manyTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules);
    }

    @Test
    public void manyConnect4() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules);
    }
}
