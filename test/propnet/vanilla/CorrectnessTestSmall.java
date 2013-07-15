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

    public static int numRuns = 5;

    @Test
    public void manyButtonsLights() {
        String gameName = "bestbuttonsandlights.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyTicTacToe() {
        String gameName = "tictactoe.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyConnect4() {
        String gameName = "connect4.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++)
            MachineTestUtil.stepThroughVanilla(rules, gameName);
    }

    @Test
    public void manyPilgrimage() {
        String gameName = "pilgrimage.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }

    @Test
    public void manyDualConnect4() {
        String gameName = "dualconnect4.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }

    @Test
    public void manyCheckersBarrel() {
        String gameName = "checkersbarrelnokings.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }

    @Test
    public void manyChinook6x6() {
        String gameName = "chinook6x6.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }

/*  BAD KIF: No bases
    @Test
    public void manyTicTacHeavenFc() {
        String gameName = "tictacheavenfc.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }
  */

/*
    @Test
    public void manyTTCC4() {
        String gameName = "ttcc4.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }
*/

    @Test
    public void jointButtonsAndLights() {
        String gameName = "jointbuttonsandlights.kif";
        List<Rule> rules = Statics.rulesForGame(gameName);
        for (int i=0; i<numRuns; i++) {
            MachineTestUtil.stepThroughVanilla(rules, gameName);
            System.out.println("Done with run " + i);
        }
    }

}
