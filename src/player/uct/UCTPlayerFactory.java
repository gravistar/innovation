package player.uct;

import rekkura.logic.model.Rule;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * User: david
 * Date: 6/26/13
 * Time: 12:01 PM
 * Description:
 *      Builds various UCT Players
 */
public class UCTPlayerFactory {
    public static UCTPlayer createProverPlayerSingleThread() {
        final ExecutorService buildManager = Executors.newFixedThreadPool(UCTStatics.numBuildThreads(1));
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.singleProverConfig(rules, buildManager);
            }

            @Override
            protected void reflect() {
                buildManager.shutdownNow();
            }
        };
    }

    public static UCTPlayer createPropNetPlayerVanillaSingleThread() {
        final ExecutorService buildManager = Executors.newFixedThreadPool(UCTStatics.numBuildThreads(1));
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.singlePropNetVanillaConfig(rules, buildManager);
            }

            @Override
            protected void reflect() {
                buildManager.shutdownNow();
            }
        };
    }

    public static UCTPlayer createPropNetPlayerNativeSingleThread() {
        final ExecutorService buildManager = Executors.newFixedThreadPool(UCTStatics.numBuildThreads(1));
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.singlePropNetNativeConfig(rules, buildManager);
            }

            @Override
            protected void reflect() {
                buildManager.shutdownNow();
            }
        };
    }

    public static UCTPlayer createPropNetPlayerNativeFullThread() {
        final ExecutorService buildManager = Executors.newFixedThreadPool(
                UCTStatics.numBuildThreads(Runtime.getRuntime().availableProcessors()));
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.fullKaskadeConfig(rules, buildManager);
            }

            @Override
            public void reflect() {
                buildManager.shutdownNow();
            }
        };
    }
}
