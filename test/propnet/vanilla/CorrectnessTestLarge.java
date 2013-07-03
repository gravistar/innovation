package propnet.vanilla;

import com.google.common.collect.Sets;
import org.junit.Test;
import propnet.PropNetFactory;
import propnet.PropNetInterface;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * User: david
 * Date: 6/28/13
 * Time: 2:05 PM
 * Description:
 *      Runs correctness test on all the games in the repository
 */
public class CorrectnessTestLarge {
    @Test
    public void manyTestHuge() {
        Set<String> skip = Sets.newHashSet("chess.kif", "chinesecheckers.kif", "chinesecheckers4.kif", "chinook.kif",
                "firesheep.kif", "pentago.kif");
        Set<String> mismatch = //Sets.newHashSet();
                Sets.newHashSet("checkersbarrelnokings.kif", "chinook6x6.kif", "dualconnect4.kif",
                        "jointbuttonsandlights.kif");
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
            PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
            if (net == null) {
                System.out.println("Build failed!");
                continue;
            } else {
                System.out.println("Build succeeded! Net has " + net.props().size() +
                        " props and " + net.size() + " total components!");
            }
            for (int i=0; i<10; i++)
                MachineTestUtil.stepThroughVanilla(rules);
            System.out.println();
        }
    }

}
