package util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import propnet.Node;
import propnet.PropNet;
import propnet.ValFn;
import propnet.util.Tuple2;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.MatchRunnable;
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

    public static class Tallier {
        public Map<Dob, Integer> wins = Maps.newHashMap();
        public int total = 0;
        public int limit;
        public Tallier(List<Dob> roles, int l) {
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
            output();
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
