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
        String gameName = "buttonsandlights";
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyTicTacToe() {
        String gameName = "tictactoe.kif";
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyConnect4() {
        String gameName = "connect4.kif";
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyPilgrimage() {
        String gameName = "pilgrimage.kif";
        String abs = Statics.gamesDir + gameName;
        List<Rule> rules = SimpleGames.getRulesForFile(abs);
        for (int i=0; i<20; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }
}
