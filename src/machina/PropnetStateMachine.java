package machina;

import com.google.common.collect.ListMultimap;
import propnet.PropNet;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.model.Dob;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 5/31/13
 * Time: 8:12 PM
 *
 * A simple in-memory propnet state machine.
 *
 */
public class PropNetStateMachine implements GgpStateMachine{

    public PropNet net;

    // store the following data structures

    // Set<Dob> initial
    // Dob terminal
    // Map<Dob, Dob> next to base
    // Set<Dob> goal
    // Set<Dob> input
    // Set<Dob> legal

    @Override
    public Set<Dob> getInitial() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ListMultimap<Dob, Dob> getActions(Set<Dob> state) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Dob> nextState(Set<Dob> state, Map<Dob, Dob> actions) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isTerminal(Set<Dob> state) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<Dob, Integer> getGoals(Set<Dob> state) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
