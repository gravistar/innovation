package player.uct;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.structure.Pool;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * User: david
 * Date: 7/13/13
 * Time: 12:11 PM
 * Description:
 *      Meant to be called withing <code>constructMachine()</code>
 *      This sets up all the fields that the UCT player needs
 *      to run its depth charges, package them together, and then
 *      pick its final move.
 *
 *      Remember to submerge this.role in the accumulator pool!
 */
public interface ConfigInterface {

    // Config params
    public String getTag();                                 // player name
    public int getFuzz();                                   // fuzz threshold (ms)
    public int numThreads();
    public Level getLoggingLevel();
    public int getAccumDuration();

    // Depth charger components
    public List<Future<GgpStateMachine>> createChargeMachines(); // async
    public List<Charger> createChargers();                       // never async
    public List<Pool> createChargePools();                       // never async.
                                                                 // pools associated with each charge machine

    // Master components
    public GgpStateMachine createMainMachine();             // used for this.machine
    public Pool getAccumulatorPool();                       // pool for this.machine and accumulator pool
    public Charger getAccumulator();

}
