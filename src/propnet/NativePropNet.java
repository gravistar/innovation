package propnet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import rekkura.logic.model.Dob;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/11/13
 * Time: 10:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class NativePropNet implements PropNetInterface {
    public ByteBuffer net;
    public Map<Dob,Integer> props;
    public Map<Node,Integer> indices;
    native void advanceNative(ByteBuffer net);

    public static Random rand = new Random(System.currentTimeMillis());

    public NativePropNet (Map<Node,Dob> invProps, List<Node> top) {
        props = Maps.newHashMapWithExpectedSize(invProps.keySet().size());
        indices = Maps.newHashMapWithExpectedSize(top.size());
        for (int i=0; i<top.size(); i++) {
            Node n = top.get(i);
            if (invProps.containsKey(n)) {
                Preconditions.checkArgument(!props.containsKey(n));
                props.put(invProps.get(n), i);
            }
            indices.put(n, i);
        }
        net = ByteBuffer.allocateDirect(top.size() / Byte.SIZE + 1);
        compileAndLoad(top);
    }

    /**
     * @param top
     *      node in topological order
     */
    public void compileAndLoad(List<Node> top) {
        long id = rand.nextLong() >>> 1;
        // jni class implements advanceNative
        String nativeName = "nativePropNet" + id;
        String macroName = "__" + nativeName + "__";
        StringBuilder source = new StringBuilder();
        String header = "#ifndef " + macroName + "\n#define " + macroName + "\n";
        String footer = "#endif";
        String include = "#include <jni.h>\n";
        String advanceDecl = "JNIEXPORT void JNICALL Java_propnet_advance_Ljava_nio_ByteBuffer_1(JNIEnv *env, jobject obj, jobject buf)";
        String directAccess = "unsigned char* net = (unsigned char *) (*env)->GetDirectBufferAddress(env, buf);";

        // create source file
        source.append(header);
        source.append(include);
        source.append(advanceDecl);
        source.append("{\n");
        source.append("\t").append(directAccess).append("\n");
        for (Node n : top) {
            StringBuilder nextLine = getAdvanceLine(n, indices);
            if (nextLine.length() > 0)
                source.append("\t").append(nextLine);
        }
        source.append("}\n");
        source.append(footer);

        // compile fields
        String cwd = System.getProperty("user.dir") + "/";
        //String compileDir = cwd + "compiled/";
        String compileDir = "compiled/";
        String sourceDir = compileDir + "src/";
        String libDir = compileDir + "lib/";
        String nativeLibName = System.mapLibraryName(nativeName);
        String sourceFileName = nativeName + ".c";
        String fullSourceFileName = sourceDir + sourceFileName;
        String fullLibFileName = libDir + nativeLibName;
        String jniDir = "/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers/";
        String libFlag = "-I" + jniDir;
        String gccFlags = libFlag + " -shared -O2 -lc ";

        try {
            // write source
            BufferedWriter out = new BufferedWriter(new FileWriter(fullSourceFileName));
            out.write(source.toString());
            out.close();

            // compile
            String cmd = "gcc " + gccFlags + fullSourceFileName + " -o " + fullLibFileName;
            System.out.println("Compiling... [command: " + cmd + "]");
            Runtime.getRuntime().exec(cmd);

            // load
            System.out.println("Loading... [Native lib: " + nativeLibName + "]");
            Thread.sleep(1000);
            System.loadLibrary(nativeName);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

         /**
         *
         * // advance
         * // fully qualified bytebuffer class is java.nio.ByteBuffer
         * void Java_propnet_advance_Ljava_nio_ByteBuffer_1 (JNIEnv *env, jobject obj, jobject buf) {
         *    unsigned char* net = (char *) (*env)->GetDirectBufferAddress(env, buf);
         *    // for i = 1...numComponents
         *      //TARGET = char with 1 in target bit
         *      //NEG_TARGET = all 1s except in targets bit
         *      net[i] = (net[i_1]<<j_1)&(net[i_2]>>j_2)&(net[i_3]<<j_3)...(NEG_TARGET&((net[k_1]<<l_1)|(net[k_2]<<l_2)|...))&TARGET;
         * }
         *
         *
         *
         */
    }

    /**
     * Gives code that tells how to shift bit of node n to position dst
     * @param idx
     *      absolute index of n in topo order
     * @param p
     *      target bit pos (since byte, p is in range [0,7])
     * @param n
     * @return
     */

    /**
     * Gives code that tells how to shift bit of node n to position p
     * @param n
     * @param p
     *      target bit pos (since byte, range[0,7])
     * @param indices
     *      mapping from node to index in topo order
     * @return
     */
    public static StringBuilder shiftTo(Node n, int p, Map<Node,Integer> indices) {
        Preconditions.checkArgument(indices.containsKey(n));
        StringBuilder ret = new StringBuilder();
        ret.append("(");
        int idx = indices.get(n);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        int d = bp - p;
        ret.append(nodeBlock(n, idx));
        if (d > 0) {
            ret.append("<<");
            ret.append(d);
        }
        if (d < 0) {
            ret.append(">>");
            ret.append(-d);
        }
        ret.append(")");
        return ret;
    }

    public static StringBuilder nodeBlock(Node n, int idx) {
        StringBuilder ret = new StringBuilder();
        ret.append("net[").append(idx / Byte.SIZE).append("]");
        return ret;
    }

    public StringBuilder getAdvanceLine(Node n, Map<Node,Integer> indices) {
        StringBuilder ret = new StringBuilder();
        if (n.inputs.isEmpty())
            return ret;
        Preconditions.checkArgument(indices.containsKey(n));
        int idx = indices.get(n);
        ret.append(nodeBlock(n,idx));
        ret.append("&=");
        StringBuilder rhs = getRHS(n, indices);
        ret.append(rhs);
        ret.append(";\n");
        return ret;
    }

    public StringBuilder getRHS(Node n, Map<Node,Integer> indices) {
        Preconditions.checkArgument(indices.containsKey(n));
        int idx = indices.get(n);
        StringBuilder rhs = new StringBuilder();
        char op = '|';
        int pos = idx % Byte.SIZE;
        StringBuilder target = new StringBuilder("0x");

        if (n.fn == NodeFns.NOT)
            target.append(Integer.toHexString(Byte.MAX_VALUE - (1 << pos)));
        else
            target.append(Integer.toHexString(1 << pos));
        if (n.fn == NodeFns.AND)
            op = '&';

        rhs.append("(");
        for (Node in : n.inputs)
            rhs.append(shiftTo(n, pos, indices)).append(op);
        rhs.deleteCharAt(rhs.length()-1);
        rhs.append(")");
        rhs.append("&");
        rhs.append(target);
        return rhs;
    }

    @Override
    public void wipe() {
        byte[] asBytes = net.array();
        Arrays.fill(asBytes, (byte)0);
    }

    // NEW DIRECT BYTE BUFFER!!
    // THIS SHOULD BE ONLY JNI METHOD
    @Override
    public void advance() {
        advanceNative(net);
    }

    @Override
    public Set<Dob> props() {
        return props.keySet();
    }

    @Override
    public boolean val(Dob prop) {
        int idx = props.get(prop);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        return (net.get(bi) & (1<<bp)) > 0;
    }

    @Override
    public void set(Dob prop, boolean val) {
        int idx = props.get(prop);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        byte block = net.get(bi);
        byte target;
        if (val)
            target = (byte)(1<<bp);
        else
            target = (byte)(Byte.MAX_VALUE - (1<<bp));
        net.putChar(bi, (char)(block &= target));
    }
}
