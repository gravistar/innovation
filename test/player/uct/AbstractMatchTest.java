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

import java.util.List;

/**
 * User: david
 * Date: 7/8/13
 * Time: 11:04 AM
 * Description:
 *      Template for setting up a match test for a game. Comes with some default player type matches.
 */
public abstract class AbstractMatchTest {

    public abstract int startclock();
    public abstract int playclock();
    public abstract int numRuns();
    public abstract String gameName();

    public List<Rule> rules() {
        return SimpleGames.getRulesForFile(Statics.gamesDir + gameName());
    }

    public void generalMatchTest(List<TestUtil.PlayerType> playerTypes) {
        // this needs to be done. otherwise new set of dobs is generated
        List<Rule> rules = rules();
        Game.Config config = new Game.Config(startclock(), playclock(), rules);
        List<Dob> roles = Game.getRoles( rules );
        EventBus bus = new EventBus();
        TestUtil.runMultipleMatches(config, roles, playerTypes, numRuns(), bus);
    }

    @Test
    public void UCTProverVsMonteCarlo() {
        generalMatchTest( Lists.newArrayList(TestUtil.PlayerType.UCTProver, TestUtil.PlayerType.MonteCarlo) );
    }

    @Test
    public void UCTPropNetVanillaVsUCTProver() {
        generalMatchTest( Lists.newArrayList(TestUtil.PlayerType.UCTPropNetVanilla, TestUtil.PlayerType.UCTProver) );
    }

    @Test
    public void UCTPropNetNativeVsUCTPropNetVanilla() {
        generalMatchTest( Lists.newArrayList(TestUtil.PlayerType.UCTPropNetNative,
                TestUtil.PlayerType.UCTPropNetVanilla) );
    }

}
