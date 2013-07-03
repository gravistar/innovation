package propnet.nativecode;

import com.google.common.base.Preconditions;
import propnet.PropNetInterface;
import rekkura.logic.model.Dob;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 * Date: 6/13/13
 * Time: 11:12 PM
 * Description:
 *      Subclasses of this should be machine generated, final and provide a static
 *      initializer that loads the appropriate jni library.
 */
public abstract class NativePropNet implements PropNetInterface {
    public ByteBuffer net;
    public Map<Dob,Integer> props;
    public int size;

    public NativePropNet(int size, Map<Dob, Integer> props) {
        this.props = props;
        this.size = size;
        net = ByteBuffer.allocateDirect(size / Byte.SIZE + 1);
    }

    @Override
    public final void wipe() {
        net = ByteBuffer.allocateDirect(size / Byte.SIZE + 1);
        for (int i=0; i<size; i++) {
            Preconditions.checkArgument(net.get(i / Byte.SIZE) == 0);
        }
    }

    @Override
    public final Set<Dob> props() {
        return props.keySet();
    }

    @Override
    public final boolean val(Dob prop) {
        int idx = props.get(prop);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        boolean v = (net.get(bi) & (1<<bp)) != 0;
        return v;
    }

    @Override
    public final void set(Dob prop, boolean val) {
        Preconditions.checkArgument(val);
        int idx = props.get(prop);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        byte block = net.get(bi);
        byte target = (byte)(1<<bp);
        block |= target;
        net.put(bi, block);
    }

    @Override
    public final long size() {
        return size;
    }

    public void printBlocks() {
        for (int i=0; i<(size / Byte.SIZE) + 1; i++) {
            byte b = net.get(i);
            System.out.println("Block " + i);
            NativeUtil.printBlock(b);
        }
    }

}
