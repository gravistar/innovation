package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.ggp.machina.ProverStateMachine;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.format.KifFormat;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.prover.StratifiedProver;
import rekkura.logic.structure.Cachet;
import rekkura.logic.structure.Pool;
import rekkura.logic.structure.Ruletta;
import rekkura.test.ggp.SimpleGames;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/30/13
 * Time: 10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetTest {

    @Test
    public void printLightsOnPropnet() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();

        PropNet net = PropNetFactory.createFromRules(rules);

        System.out.println("==== LIGHTS ON NET ====");
        System.out.println(net);
        PropNetFactory.checkDuplicateKeys(net);
    }

    @Test
    public void connect4PropNet() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();

        PropNet net = PropNetFactory.createFromRules(rules);
        System.out.println("==== CONNECT 4 NET ====");
        // too big
        System.out.println(net);
    }

    @Test
    public void tictactoePropNet() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        PropNet net = PropNetFactory.createFromRules(rules);
        System.out.println("==== Tic Tac Toe ====");
        System.out.println(net);
        PropNetFactory.checkDuplicateKeys(net);
    }

    @Test
    public void limitsTest() {
        Set<String> skip = Sets.newHashSet("chinesecheckers.kif", "chinesecheckers4.kif", "chinook.kif", "pentago.kif");
        String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";
        File gameDir = new File(gamesDir);
        File[] files = gameDir.listFiles();
        for (File gameFile : files) {
            String gameName = gameFile.getAbsolutePath().substring(gamesDir.length());
            if (skip.contains(gameName)) {
                System.out.println("Skipping " + gameName);
                System.out.println();
                continue;
            }
            System.out.println("Attempting to propnet " + gameName);
            List<Rule> rules = SimpleGames.getRulesForFile(gameFile.getAbsolutePath());
            PropNet net = PropNetFactory.createFromRules(rules);
            if (net == null) {
                System.out.println("Build failed!");
            } else {
                System.out.println("Build succeeded! Net has " + net.props().size() +
                        " props and " + net.tnet.size() + " total components!");
            }
            System.out.println();
        }
    }

}
