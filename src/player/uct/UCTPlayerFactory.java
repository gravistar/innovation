package player.uct;

import rekkura.logic.model.Rule;

import java.util.List;
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
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.singleProverConfig(rules);
            }

            @Override
            protected void reflect() {
                if (logLevel == Level.INFO)
                    printStats();
            }
        };
    }

    public static UCTPlayer createPropNetPlayerSingleThread() {
        return new UCTPlayer() {
            @Override
            public ConfigInterface buildConfig(List<Rule> rules) {
                return ConfigFactory.singlePropNetVanillaConfig(rules);
            }

            @Override
            protected void reflect() {
                if (logLevel == Level.INFO)
                    printStats();
            }
        };
    }
}
