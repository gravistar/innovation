package player.uct;

import rekkura.logic.model.Dob;

import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: david
* Date: 5/20/13
* Time: 12:15 AM
* To change this template use File | Settings | File Templates.
*/
class StateActionPair {
     private final Dob action;
     private final Set<Dob> state;

    public StateActionPair(Set<Dob> state, Dob action) {
        this.action = action;
        this.state = state;
    }

    public Set<Dob> getState() {
        return state;
    }

    public Dob getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        return 31*action.hashCode() + state.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StateActionPair stateActionPair = (StateActionPair) o;

        if (!action.equals(stateActionPair.action)) return false;
        if (!state.equals(stateActionPair.state)) return false;

        return true;
    }

    @Override
    public String toString() {
        String prefix = "[State-Action Pair] ";
        String stateString = "State: " + state;
        String actionString = "Action: " + action;
        String output = prefix + "\n\t" + stateString + "\n\t" + actionString + "\n";
        return output;
    }
}
