package propnet.vanilla;

import org.junit.Test;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.MachineTestUtil;

import java.io.File;
import java.util.List;

/**
 * User: david
 * Date: 6/28/13
 * Time: 2:05 PM
 * Description:
 *      Runs correctness test on all the games in the repository
 *      Doesn't give a time limit because that's up to perf test
 */
public class CorrectnessTestLarge {

    public static int numRuns = 5;

    @Test
    public void manyTestHuge() {
        String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";
        File gameDir = new File(gamesDir);
        File[] files = gameDir.listFiles();
        for (File gameFile : files) {
            String gameName = gameFile.getAbsolutePath().substring(gamesDir.length());
            if (Statics.skip.contains(gameName))
                continue;
            System.out.println("Processing game: " + gameName);
            List<Rule> rules = SimpleGames.getRulesForFile(gameFile.getAbsolutePath());
            for (int i=0; i<numRuns; i++) {
                MachineTestUtil.stepThroughVanilla(rules, gameName);
                System.out.println("Done with run " + i);
            }
            System.out.println();
        }
    }

}
