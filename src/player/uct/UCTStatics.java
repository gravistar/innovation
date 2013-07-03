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

    public static Dob randomAction(List<Dob> actions) {
        Preconditions.checkArgument(!actions.isEmpty());
        return actions.get(rand.nextInt(actions.size()));
    }
}
