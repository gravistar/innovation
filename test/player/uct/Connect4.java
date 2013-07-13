package player.uct;

/**
 * User: david
 * Date: 7/8/13
 * Time: 10:58 AM
 * Description:
 *      Match tests for connect4
 */
public class Connect4 extends AbstractMatchTest{
    public static int startclock = 10000, playclock = 10000;
    public static String gameName = "connect4.kif";

    @Override
    public int startclock() {
        return startclock;
    }

    @Override
    public int playclock() {
        return playclock;
    }

    @Override
    public int numRuns() {
        return 5;
    }

    @Override
    public String gameName() {
        return gameName;
    }
}
