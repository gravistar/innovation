package player.uct;

import util.TestUtil;

/**
 * User: david
 * Date: 7/15/13
 * Time: 9:43 PM
 * Description:
 *      Does innovation play all the games without breaking?
 */
public class InnovationRobustnessTest extends AbstractRobustnessTest{
    @Override
    public TestUtil.PlayerType getTestPlayerType() {
        return TestUtil.PlayerType.UCTPropNetNativeFullThreaded;
    }

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
        return 1;
    }
}
