package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import machina.PropNetStateMachine;
import org.junit.Test;
import rekkura.ggp.machina.BackwardStateMachine;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/1/13
 * Time: 4:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropNetMachineTest {

    @Test
    public void manyButtonsLights() {
        List<Rule> rules = SimpleGames.getButtonsAndLights();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThrough(rules);
    }

    @Test
    public void manyTicTacToe() {
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThrough(rules);
    }

    @Test
    public void manyConnect4() {
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        for (int i=0; i<20; i++)
            MachineTestUtil.stepThrough(rules);
    }

    @Test
    public void manyTestHuge() {
        Set<String> skip = Sets.newHashSet("chess.kif", "chinesecheckers.kif", "chinesecheckers4.kif", "chinook.kif", "pentago.kif");
        Set<String> mismatch = Sets.newHashSet("breakthrough.kif", "checkersbarrelnokings.kif", "chinook6x6.kif", "dualconnect4.kif",
                                                "firesheep.kif", "hunter.kif");
        String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";
        File gameDir = new File(gamesDir);
        File[] files = gameDir.listFiles();
        for (File gameFile : files) {
            String gameName = gameFile.getAbsolutePath().substring(gamesDir.length());
            if (skip.contains(gameName) || mismatch.contains(gameName)) {
                System.out.println("Skipping " + gameName);
                System.out.println();
                continue;
            }
            System.out.println("Attempting to propnet " + gameName);
            List<Rule> rules = SimpleGames.getRulesForFile(gameFile.getAbsolutePath());
            PropNet net = PropNetFactory.createFromRules(rules);
            if (net == null) {
                System.out.println("Build failed!");
                continue;
            } else {
                System.out.println("Build succeeded! Net has " + net.props().size() +
                        " props and " + net.tnet.size() + " total components!");
            }
            for (int i=0; i<10; i++)
                MachineTestUtil.stepThrough(rules);
            System.out.println();
        }
    }





}
