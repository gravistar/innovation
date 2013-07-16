package player.uct;

import com.google.common.base.Preconditions;
import rekkura.logic.model.Dob;

import java.util.List;
import java.util.Random;

/**
 * User: david
 * Date: 7/3/13
 * Time: 2:24 PM
 * Description:
 *      Some static parameters for the uct players
 */
public class UCTStatics {
    public static Random rand = new Random(System.currentTimeMillis());
    public static double discFactor = .999;
    public static double C = 40;

    // fuzz thresholds
    public static int fuzzSuperRisky = 100;
    public static int fuzzRisky = 200;
    public static int fuzzSafe = 1000;
    public static int fuzzSuperSafe = 1200;
    public static long forbiddenTimeout = -1;

    // accum levels
    public static int accumShort = 100;
    public static int accumProduction = 200; // should not take long

    // thread priority
    public static enum ThreadPriority{
        MAIN(Thread.MAX_PRIORITY),
        BUILD(Thread.MAX_PRIORITY-1),
        COPY(Thread.MAX_PRIORITY-2),
        CHARGE(Thread.MAX_PRIORITY-3);

        public int level;

        ThreadPriority(int level) {
            this.level = level;
        }
    }

    public static Dob randomAction(List<Dob> actions) {
        Preconditions.checkArgument(!actions.isEmpty());
        return actions.get(rand.nextInt(actions.size()));
    }

    public static int numBuildThreads (int numThreads) {
        return 2 + 2 * numThreads;
    }

}
