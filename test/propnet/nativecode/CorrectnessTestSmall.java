package propnet.nativecode;

import org.junit.Test;
import propnet.vanilla.Statics;
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
        String gameName = "buttonsandlights";
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules, gameName);
    }

    @Test
    public void manyTicTacToe() {
        String gameName = "tictactoe.kif";
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules, gameName);
    }

    @Test
    public void manyConnect4() {
        String gameName = "connect4.kif";
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules, gameName);
    }

    @Test
    public void manyPilgrimage() {
        String gameName = "pilgrimage.kif";
        String abs = Statics.gamesDir + gameName;
        List<Rule> rules = SimpleGames.getRulesForFile(abs);
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughNative(rules, gameName);
    }

}
