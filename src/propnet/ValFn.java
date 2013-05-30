package propnet;

import java.util.Collection;

/**
* Created with IntelliJ IDEA.
* User: david
* Date: 5/25/13
* Time: 11:14 AM
* To change this template use File | Settings | File Templates.
*/
public interface ValFn<T> {
    public boolean eval(Collection<T> inputs);
}
