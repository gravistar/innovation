package propnet.vanilla;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import rekkura.logic.model.Rule;
import rekkura.test.ggp.SimpleGames;

import java.util.List;
import java.util.Set;

/**
 * User: david
 * Date: 7/4/13
 * Time: 9:27 PM
 * Description:
 *     Contains some shared items across tests
 */
public class Statics {

    public static final Set<String> skip = Sets.newHashSet(
        "chess.kif",
        "chinook.kif",
        "chinesecheckers4.kif",
        "slaughter.kif",
        "skirmish.kif",
        "pentago.kif",
        "firesheep.kif",
        "chinesecheckers.kif",
        "checkersbarrelnokings.kif", // propnettable
        "chinook6x6.kif",            // propnettable
        "freeforall.kif",            // propnettable but hangs
        "knightstour.kif",           // propnettable but hangs
        "tictacheavenfc.kif",        // BAD KIF (no bases)
        "ttcc4.kif"                  // propnettable
    );

    public static final List<String> large = Lists.newArrayList(
            "pilgrimage.kif",
            "firesheep.kif",
            "platformjumpers.kif"
            // "chinook.kif" // too beastly
    );

    // following just hang randomly
    // freeforall.kif
    // knightstour.kif

    public static String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";

    public static List<Rule> rulesForGame(String gameName) {
        return SimpleGames.getRulesForFile(gamesDir + gameName);
    }
}
