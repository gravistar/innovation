package propnet.vanilla;

import com.google.common.collect.Sets;
import org.junit.Test;
import propnet.PropNetFactory;
import propnet.PropNetInterface;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:15 PM
 * Description:
 *      Detects which games are propnetable and how long it takes to propnet them
 */
public class PerfTest {
    @Test
    public void limitsTest() {
        Set<String> skip = Sets.newHashSet("chinesecheckers.kif", "chinesecheckers4.kif", "chinook.kif",
                "pentago.kif");
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
            PropNetInterface net = PropNetFactory.createFromRulesOnlyNet(rules);
            if (net == null) {
                System.out.println("Build failed!");
            } else {
                System.out.println("Build succeeded! Net has " + net.props().size() +
                        " props and " + net.size() + " total components!");
            }
            System.out.println();
        }
    }
}
