package propnet;

import rekkura.logic.model.Dob;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/11/13
 * Time: 10:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropNetInterface {
    public void wipe();
    public void advance();
    public Set<Dob> props();
    public boolean val(Dob prop);
    public void set(Dob prop, boolean val);
}
