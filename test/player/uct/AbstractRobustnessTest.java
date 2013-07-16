package player.uct;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.Test;
import propnet.vanilla.Statics;
import rekkura.ggp.milleu.Game;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.TestUtil;

import java.io.File;
import java.util.List;

/**
 * User: david
 * Date: 7/15/13
 * Time: 9:28 PM
 * Description:
 *      Can the test player play vs legal on all the games?
 *
 */
public abstract class AbstractRobustnessTest {

    public abstract TestUtil.PlayerType getTestPlayerType();
    public abstract int startclock();
    public abstract int playclock();
    public abstract int numRuns();

    @Test
    public void testAllVsLegal() {
        int startclock = startclock();
        int playclock = playclock();
        File gameDir = new File(Statics.gamesDir);
        File[] files = gameDir.listFiles();
        for (final File gameFile : files) {
            String gameName = gameFile.getAbsolutePath().substring(Statics.gamesDir.length());
            System.out.println(">>>> STARTING GAME " + gameName);

            List<Rule> rules = SimpleGames.getRulesForFile(gameFile.getAbsolutePath());
            Game.Config config = new Game.Config(startclock(), playclock(), rules);
            List<Dob> roles = Game.getRoles( rules );
            EventBus bus = new EventBus();
            List<TestUtil.PlayerType> playerTypes = Lists.newArrayList(getTestPlayerType(), TestUtil.PlayerType.Legal);
            TestUtil.runMultipleMatches(config, roles, playerTypes, numRuns(), bus);
            System.out.println();
        }
    }

    @Test
    public void testLargeVsLegal() {
        for (String gameName : Statics.large) {
            System.out.println(">>>>> Running game: " + gameName);
            String absGameName = Statics.gamesDir + gameName;
            List<Rule> rules = SimpleGames.getRulesForFile(absGameName);
            Game.Config config = new Game.Config(startclock(), playclock(), rules);
            List<Dob> roles = Game.getRoles( rules );
            EventBus bus = new EventBus();
            List<TestUtil.PlayerType> playerTypes = Lists.newArrayList(getTestPlayerType(), TestUtil.PlayerType.Legal);
            TestUtil.runMultipleMatches(config, roles, playerTypes, numRuns(), bus);
            System.out.println();
        }
    }
}