package propnet.vanilla;

import propnet.AbstractPerfTest;
import propnet.PropNetInterface;
import propnet.util.Tuple2;
import rekkura.logic.model.Rule;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * User: david
 * Date: 6/28/13
 * Time: 1:15 PM
 * Description:
 *      Perf test with vanilla propnet
 *
 *      Make sure the heap space is 4G
 */
public class PerfTest extends AbstractPerfTest{
    @Override
    public Callable<Tuple2<Long, Long>> getBuildTask(File gameFile, Set<String> failed) {
        return defaultBuildTask(gameFile, failed);
    }

    @Override
    public PropNetInterface getPropNet(List<Rule> rules) {
        return PropNetFactory.createFromRulesOnlyNet(rules);
    }

    @Override
    public void cleanup(){}
}
