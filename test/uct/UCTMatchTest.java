package uct;

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
import uct.UCTPlayer;

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
        bus.register(new HistoryShower());
        MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(new UCTPlayer(), new MonteCarloPlayer()), bus);
        match.run();
    }
    
    @Test
    public void test2() {
    	int startclock=1000, playclock=2000;
    	List<Rule> rules = SimpleGames.getConnectFour();
    	Game.Config config = new Game.Config(startclock, playclock, rules);
    	EventBus bus = new EventBus();
    	int numTest = 20;
    	bus.register(new Tallier(Game.getRoles(rules), numTest));
    	for (int tt=0; tt<numTest; tt++) {
    		MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(new UCTPlayer(), new MonteCarloPlayer()), bus);
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
    	bus.register(new Tallier(Game.getRoles(rules), numTest));
    	for (int tt=0; tt<numTest; tt++) {
    		MatchRunnable match = Match.newBuilder(config).build().newRunnable(Lists.<Player>newArrayList(new UCTPlayer(), new MonteCarloPlayer()), bus);
    		match.run();
    	}
    }
    
    public static class Tallier {
    	public Map<Dob, Integer> wins = Maps.newHashMap();
    	public int total = 0;
    	public int limit;
    	public Tallier(List<Dob> roles, int l) {
    		for (Dob role : roles)
    			wins.put(role, 0);
    		limit = l;
    	}
    	
    	@Subscribe public void countMatchResult(MatchRunnable.GoalEvent e) {
    		tally(e.match);
    	}
    	
    	public void tally(MatchRunnable m) {
    		for (Dob role : m.goals.keySet()) {
    			if (m.goals.get(role) == 100)
    				wins.put(role, 1 + wins.get(role));
    		}
    		total++;
    		if (total == limit)
    			output();
    	}
    	
    	public void output() {
    		System.out.println("==== TEST RUN INFORMATION ===");
    		for (Dob r : wins.keySet()) {
    			System.out.println("Player " + r + " has " + wins.get(r) + " / " + total);
    		}
    	}
    }
    
    class HistoryShower {
    	@Subscribe public void printMatchSummary(MatchRunnable.GoalEvent e) {
    		printHistory(e.match);
    	}
    }

    public void printHistory(MatchRunnable m) {
       	System.out.println("[MATCH SUMMARY]");
       	System.out.println("goal size: " + m.goals.size());
    	for (Dob role : m.goals.keySet()) {
    		System.out.println("Player " + role + " has score " + m.goals.get(role));
    	}
    	
    	int n = 1;
    	System.out.println("[MATCH HISTORY]");
    	for (Game.Record r : m.history) {
    		System.out.print("Move #" + n + ": ");
    		System.out.println(r);
    		n++;
    	}
 
    }
    
}
