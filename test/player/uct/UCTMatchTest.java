package player.uct;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.Test;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Match;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;
import rekkura.ggp.player.MonteCarloPlayer;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;
import util.TestUtil;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/21/13
 * Time: 1:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class UCTMatchTest {	
    @Test
    public void test1() {
        int startclock = 30000, playclock = 1000;
        List<Rule> rules = SimpleGames.getConnectFour();
        Game.Config config = new Game.Config(startclock, playclock, rules);
        EventBus bus = new EventBus();
        bus.register(new TestUtil.HistoryShower());
        MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                UCTPlayerFactory.createProverPlayer(), new MonteCarloPlayer()), bus);
        match.run();
    }
    
    @Test
    public void test2() {
    	int startclock=1000, playclock=1000;
    	List<Rule> rules = SimpleGames.getConnectFour();
    	Game.Config config = new Game.Config(startclock, playclock, rules);
    	EventBus bus = new EventBus();
    	int numTest = 20;
    	bus.register(new TestUtil.Tallier(Game.getRoles(rules), numTest));
    	for (int tt=0; tt<numTest; tt++) {
    		MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                    UCTPlayerFactory.createProverPlayer(), new MonteCarloPlayer()), bus);
    		match.run();
    	}
    }
    
    @Test
    public void test3() {
    	int startclock=1000, playclock=1000;
    	List<Rule> rules = SimpleGames.getTicTacToe();
    	Game.Config config = new Game.Config(startclock, playclock, rules);
    	EventBus bus = new EventBus();
    	int numTest = 20;
    	bus.register(new TestUtil.Tallier(Game.getRoles(rules), numTest));
    	for (int tt=0; tt<numTest; tt++) {
    		MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                    UCTPlayerFactory.createProverPlayer(), new MonteCarloPlayer()), bus);
    		match.run();
    	}
    }

    @Test
    public void test4() {
        int startclock = 1000, playclock = 1000;
        List<Rule> rules = SimpleGames.getTicTacToeFromFile();
        Game.Config config = new Game.Config(startclock, playclock, rules);
        EventBus bus = new EventBus();
        int numTest = 20;
        bus.register(new TestUtil.Tallier(Game.getRoles(rules), numTest));
        for (int tt=0; tt<numTest; tt++) {
            MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                    UCTPlayerFactory.createVanillaPropNetPlayer(), UCTPlayerFactory.createProverPlayer()), bus);
            match.run();
        }
    }

    @Test
    public void vanillaVsProverConnect4() {
        int startclock = 1000, playclock = 1000;
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        Game.Config config = new Game.Config(startclock, playclock, rules);
        EventBus bus = new EventBus();
        int numTest = 20;
        bus.register(new TestUtil.Tallier(Game.getRoles(rules), numTest));
        for (int tt=0; tt<numTest; tt++) {
            MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                    UCTPlayerFactory.createVanillaPropNetPlayer(), UCTPlayerFactory.createProverPlayer()), bus);
            match.run();
        }
    }

    @Test
    public void nativeVsVanillaConnect4() {
        int startclock = 1000, playclock = 1000;
        List<Rule> rules = SimpleGames.getConnectFourFromFile();
        Game.Config config = new Game.Config(startclock, playclock, rules);
        EventBus bus = new EventBus();
        int numTest = 20;
        bus.register(new TestUtil.Tallier(Game.getRoles(rules), numTest));
        for (int tt=0; tt<numTest; tt++) {
            MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(
                    UCTPlayerFactory.createNativePropNetPlayer(), UCTPlayerFactory.createVanillaPropNetPlayer()), bus);
            match.run();
        }
    }
}
