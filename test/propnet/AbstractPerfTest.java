package propnet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;
import propnet.util.Tuple2;
import propnet.vanilla.Statics;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * User: david
 * Date: 7/4/13
 * Time: 9:41 PM
 * Description:
 *      Template test for seeing which games are propnettable given a certain start clock.
 *
 *      Right now the arguments are
 *          TIMEOUT - 1 minute
 *          Heap Space - 4G
 */
public abstract class AbstractPerfTest {

    public static long TIMEOUT = 60000; // timeout in ms

    public abstract Callable<Tuple2<Long,Long>> getBuildTask(File gameFile, Set<String> failed);
    public abstract PropNetInterface getPropNet(List<Rule> rules);
    public abstract void cleanup();

    public Callable<Tuple2<Long,Long>> defaultBuildTask(final File gameFile, final Set<String> failed) {
        return new Callable<Tuple2<Long,Long>>() {
            @Override
            public Tuple2<Long, Long> call() throws Exception {
                List<Rule> rules = SimpleGames.getRulesForFile(gameFile.getAbsolutePath());
                PropNetInterface net = getPropNet(rules);
                if (net == null) {
                    failed.add(gameFile.getName());
                    return new Tuple2<Long, Long>(0L, 0L);
                }
                return new Tuple2<Long, Long>(net.size(), (long)net.props().size());
            }
        };
    }

    @Test
    public void limitsTest() {
        final Set<String> failed = Sets.newHashSet();
        // _1: net size
        // _2: num props
        final Map<String, Tuple2<Long,Long>> pass = Maps.newHashMap();

        File gameDir = new File(Statics.gamesDir);
        File[] files = gameDir.listFiles();
        for (final File gameFile : files) {
            String gameName = gameFile.getAbsolutePath().substring(Statics.gamesDir.length());

            if (Statics.skip.contains(gameName))
                continue;

            System.out.println("Attempting to propnet " + gameName);
            ExecutorService builder = Executors.newSingleThreadExecutor();

            Callable<Tuple2<Long,Long>> buildTask = getBuildTask(gameFile, failed);

            try {
                List<Future<Tuple2<Long,Long>>> resList = builder.invokeAll(Lists.newArrayList(buildTask), TIMEOUT, TimeUnit.MILLISECONDS);
                Future<Tuple2<Long,Long>> res = resList.get(0);
                if (res.isDone() && !res.isCancelled()) {
                    pass.put(gameFile.getName(), res.get());
                } else {
                    failed.add(gameFile.getName());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            System.out.println("Done trying to propnet " + gameName);
            System.out.println();
        }
        printStatistics(pass, failed, TIMEOUT);
        cleanup();
    }

    public static void printStatistics(Map<String, Tuple2<Long,Long>> passed, Set<String> failed, long timeout) {
        System.out.println("===== AGGREGATE STATISTICS FOR LIMITS TEST =====");
        System.out.println("Timeout: " + timeout + " ms");
        System.out.println();

        System.out.println("==== Successful PropNet ====");
        for (String gameName : passed.keySet()) {
            System.out.println("\t" + gameName);
            System.out.println("\t\tSize: " + passed.get(gameName)._1);
            System.out.println("\t\tNum Props: " + passed.get(gameName)._2);
        }
        System.out.println();

        System.out.println("==== Failed To PropNet ====");
        for (String gameName : failed)
            System.out.println("\t" + gameName);
        System.out.println();
    }
}
