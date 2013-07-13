package util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import player.uct.UCTPlayerFactory;
import propnet.vanilla.core.Node;
import propnet.vanilla.PropNet;
import propnet.vanilla.core.ValFn;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Match;
import rekkura.ggp.milleu.MatchRunnable;
import rekkura.ggp.milleu.Player;
import rekkura.ggp.player.MonteCarloPlayer;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/2/13
 * Time: 10:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestUtil {

    /**
     * Gives a disconnected propnet with same val fn
     * @param fn
     * @return
     */
    public static PropNet uniformDiscVanilla(int sz, ValFn<Node> fn) {
        List<Node> topo = Lists.newArrayList();
        Map<Dob,Node> props = Maps.newHashMap();
        for (int i=0; i<sz; i++) {
            Node toAdd = new Node(fn);
            Dob toAddProp = new Dob();
            topo.add(toAdd);
            props.put(toAddProp, toAdd);
        }
        return new PropNet(props, topo);
    }

    /**
     * Pretty core method to the test suite. Runs multiple matches between players to collect stats
     * @param config
     * @param roles
     * @param types
     * @param numRuns
     * @param bus
     */
    public static void runMultipleMatches(Game.Config config, List<Dob> roles, List<TestUtil.PlayerType> types,
                                          int numRuns, EventBus bus) {
        TestUtil.MatchRunner mr = new TestUtil.MatchRunner(config, types, bus);
        bus.register(mr);
        bus.register(new TestUtil.Tallier(mr, roles, numRuns, bus));
        bus.post(new TestUtil.NewMatchEvent(mr));
    }

    public static class NewMatchEvent {
        public MatchRunner mr;
        public NewMatchEvent(MatchRunner mr) {
            this.mr = mr;
        }
    }

    public static enum PlayerType {
        Legal,
        MonteCarlo,
        UCTProver,
        UCTPropNetVanilla,
        UCTPropNetNative,
        UCTPropNetNativeThreaded
    }

    public static Player createNewPlayer(PlayerType type) {
        switch (type) {
            case Legal:
                return new Player.Legal();
            case MonteCarlo:
                return new MonteCarloPlayer();
            case UCTProver:
                return UCTPlayerFactory.createProverPlayer();
            case UCTPropNetVanilla:
                return UCTPlayerFactory.createVanillaPropNetPlayer();
            case UCTPropNetNative:
                return UCTPlayerFactory.createNativePropNetPlayer();
            case UCTPropNetNativeThreaded:
                return UCTPlayerFactory.createNativePropNetPlayerThreads();
        }
        return null;
    }

    public static List<Player> createNewPlayers(List<PlayerType> types) {
        List<Player> ret = Lists.newArrayList();

        for (PlayerType type : types)
            ret.add( createNewPlayer(type) );

        return ret;
    }

    /**
     * Works in conjunction with the tallier to see which player is better.
     * Starts the actual matches between players.
     */
    public static class MatchRunner {
        public List<PlayerType> types;
        public Match.Builder builder;
        public EventBus bus;

        public MatchRunner(Match.Builder builder, List<PlayerType> types, EventBus bus) {
            this.builder = builder;
            this.types = types;
            this.bus = bus;
        }

        public MatchRunner(Game.Config config, List<PlayerType> types, EventBus bus) {
            this.builder = Match.newBuilder(config);
            this.types = types;
            this.bus = bus;
        }

        @Subscribe
        public void startNewMatch(NewMatchEvent e) {
            if (this != e.mr)
                return;
            MatchRunnable match = builder.build().newRunnable(
                    createNewPlayers(types),
                    bus);
            match.run();
        }
    }

    /**
     * Works in conjunction with MatchRunner to see which player is better.
     * Tallies a running total of games won between the players and prints
     * statistics.
     */
    public static class Tallier {
        public Map<Dob, Integer> wins = Maps.newHashMap();
        public int total = 0;
        public int limit;
        public MatchRunner mr;
        public EventBus bus;
        public Tallier(MatchRunner mr, List<Dob> roles, int l, EventBus bus) {
            this.mr = mr;
            this.bus = bus;
            for (Dob role : roles)
                wins.put(role, 0);
            limit = l;
        }

        @Subscribe
        public void countMatchResult(MatchRunnable.GoalEvent e) {
            tally(e.match);
        }

        public void tally(MatchRunnable m) {
            for (Dob role : m.goals.keySet()) {
                if (m.goals.get(role) == 100)
                    wins.put(role, 1 + wins.get(role));
            }
            total++;
            if (total < limit)
                callForNewMatch();
            if (total == limit)
                printFinalHeader();
            output();
        }

        public void callForNewMatch() {
            bus.post(new NewMatchEvent(mr));
        }

        public void printFinalHeader() {
            System.out.println("===== MATCH TEST SUMMARY =====");
        }

        public void output() {
            System.out.println("==== SUMMARY (" + total + "/" + limit +") GAMES PLAYED ===");
            for (Dob r : wins.keySet()) {
                System.out.println("Player " + r + " has " + wins.get(r) + " / " + total);
            }
            System.out.println();
        }
    }

    public static class HistoryShower {
        @Subscribe public void printMatchSummary(MatchRunnable.GoalEvent e) {
            printHistory(e.match);
        }
    }

    public static void printHistory(MatchRunnable m) {
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
