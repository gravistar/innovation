package player.uct;

import propnet.nativecode.NativeUtil;
import rekkura.ggp.milleu.Game;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * User: david
 * Date: 7/15/13
 * Time: 8:47 PM
 * Description:
 *      The actual competition player!!
 */
public class InnovationPlayer extends UCTPlayer{
    public ExecutorService buildManager;

    // needed for PlayerServer interop
    public InnovationPlayer() {
        buildManager = Executors.newFixedThreadPool(
                UCTStatics.numBuildThreads(Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public ConfigInterface buildConfig(List<Rule> rules) {
        return ConfigFactory.fullKaskadeConfig(rules, buildManager);
    }

    @Override
    protected void reflect() {
        // shutdown builds if they're still going
        buildManager.shutdownNow();

        // print out goals
        Set<Dob> state = getTurn().state;
        System.out.println ("=== END GAME GOALS === ");
        System.out.println(machine.getGoals(state));

        // cleanup!
        NativeUtil.deleteGeneratedFiles();
    }
}
