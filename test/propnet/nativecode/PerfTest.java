package propnet.nativecode;

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
 * Date: 7/4/13
 * Time: 9:39 PM
 * Description:
 *      PerfTest with native propnet
 */
public class PerfTest extends AbstractPerfTest{

    @Override
    public Callable<Tuple2<Long, Long>> getBuildTask(File gameFile, Set<String> failed) {
        return defaultBuildTask(gameFile, failed);
    }

    @Override
    public PropNetInterface getPropNet(List<Rule> rules) {
        return NativePropNetFactory.createFromRules(rules)._1;
    }

    @Override
    public void cleanup() {
        NativeUtil.deleteGeneratedFiles();
    }
}
