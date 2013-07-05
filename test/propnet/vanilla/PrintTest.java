package propnet.vanilla;

import org.junit.Test;
import propnet.PropNetInterface;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;

import java.util.List;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:13 PM
 * Description:
 *      Prints out the propnets for some of the standard games
 */
public class PrintTest {

    public static String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";

    @Test
    public void lightsOn() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
        System.out.println("==== LIGHTS ON NET ====");
        System.out.println(net);
    }

    @Test
    public void connect4() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
        System.out.println("==== CONNECT 4 NET ====");
        System.out.println(net);
    }

    @Test
    public void breakthrough() {
        String breakthrough = gamesDir + "breakthrough.kif";
        List<Rule> rules = SimpleGames.getRulesForFile(breakthrough);
        PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
        System.out.println("==== BREAKTHROUGH NET ====");
        System.out.println(net);
    }

    @Test
    public void tictactoe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
        System.out.println("==== Tic Tac Toe ====");
        System.out.println(net);
    }
}
