package propnet.vanilla;

import com.google.common.collect.Sets;

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
        "chinook.kif",
        "chinesecheckers4.kif",
        "slaughter.kif",
        "skirmish.kif",
        "pentago.kif",
        "firesheep.kif",
        "chinesecheckers.kif"
    );

    public static String gamesDir = "/Users/david/Documents/ggp/rekkura2/test/rekkura/test/ggp/games/";

}
