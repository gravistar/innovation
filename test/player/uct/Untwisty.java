package player.uct;

import com.google.common.collect.Lists;
import org.junit.Test;
import util.TestUtil;

/**
 * User: david
 * Date: 7/16/13
 * Time: 4:38 PM
 * Description:
 */
public class Untwisty extends AbstractMatchTest {
    @Override
    public int startclock() {
        return 60000;
    }

    @Override
    public int playclock() {
        return 10000;
    }

    @Override
    public int numRuns() {
        return 5;
    }

    @Override
    public String gameName() {
        return "untwistycorridor.kif";
    }

    @Override
    @Test
    public void UCTPropNetNativeFullPerf() {
        generalMatchTest( Lists.newArrayList(TestUtil.PlayerType.UCTPropNetNativeFullThreaded) );
    }
}
