package propnet.nativecode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import propnet.*;
import propnet.util.Tuple2;
import propnet.util.Tuple3;
import rekkura.ggp.milleu.GameLogicContext;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: david
 * Date: 6/13/13
 * Time: 9:31 PM
 * Description:
 *      Generates a native propnet. Output files include a .java, .c, .jnilib and .class file.
 */
public class NativePropNetFactory {

    // Setup java.library.path
    static {
        NativeUtil.setupLibraryPath();
    }

    public static Random rand = new Random(System.currentTimeMillis());
    public static boolean debug = false;

    // Shared dirs
    public static String cwd = System.getProperty("user.dir") + "/";
    public static String compileDir = "compiled/";
    public static String sourceDir = compileDir + "src/";

    // JNI compilation constants
    public static String cSourceDir = sourceDir + "c/";
    public static String libDir = compileDir + "lib/";
    public static String jniDir = "/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers/";
    public static String libFlag = "-I" + jniDir;
    public static String gccFlags = libFlag + " -shared -O2 -lc ";

    // Java class compilation constants
    public static String javaSourceDir = sourceDir + "Java/";
    public static String innovationClassPath = "out/production/innovation/";
    public static String rekkuraClassPath = "../rekkura2/bin/";
    public static String classPath = ".:" + rekkuraClassPath + ".:" + innovationClassPath;
    public static String javacFlags = "-classpath " + classPath + " -d " + innovationClassPath;
    public static String packageName = "compiled.clazz";

    public static Tuple2<PropNetInterface, GameLogicContext> createForStateMachine(List<Rule> rules) {
        Tuple2<NativePropNet, GameLogicContext> param = createFromRules(rules);
        return new Tuple2<PropNetInterface, GameLogicContext>(param._1, param._2);
    }

    public static Tuple2<NativePropNet, GameLogicContext> createFromRules(List<Rule> rules) {
        Tuple2<PropNet, GameLogicContext> needed = PropNetFactory.createFromRules(rules);
        PropNet vanilla = needed._1;
        NativePropNet net = getCompiledNet(compile(vanilla));
        return new Tuple2<NativePropNet, GameLogicContext>(net, needed._2);
    }

    // just for debugging
    public static Tuple2<Tuple2<PropNetInterface, GameLogicContext>, PropNet> fromRulesWithVanilla (List<Rule> rules) {
        Tuple2<PropNet, GameLogicContext> needed = PropNetFactory.createFromRules(rules);
        PropNet vanilla = needed._1;
        NativePropNet net = getCompiledNet(compile(vanilla));
        Tuple2<PropNetInterface, GameLogicContext> forMachine = new Tuple2<PropNetInterface, GameLogicContext>
                (net, needed._2);
        Tuple2<Tuple2<PropNetInterface, GameLogicContext>, PropNet> ret =
                new Tuple2<Tuple2<PropNetInterface, GameLogicContext>, PropNet>(forMachine, vanilla);
        return ret;
    }

    public static Map<Dob,Integer> getPropIndices(Map<Dob,Node> props, Map<Node,Integer> indices) {
        Map<Dob,Integer> ret = Maps.newHashMapWithExpectedSize(props.size());
        for (Dob p : props.keySet()) {
            Preconditions.checkArgument(!ret.containsKey(p));
            ret.put(p, indices.get(props.get(p)));
        }
        return ret;
    }

    public static Map<Node,Integer> getIndices(PropNet vanilla) {
        List<Node> top = vanilla.tnet;
        Map<Node,Integer> ret = Maps.newHashMapWithExpectedSize(top.size());
        for (int i=0; i<top.size(); i++)
            ret.put(top.get(i), i);
        return ret;
    }

    public static Tuple3<String, Integer, Map<Dob, Integer>> compileFromRules(List<Rule> rules) {
        PropNet net = PropNetFactory.createFromRulesOnlyNet(rules);
        return compile(net);
    }

    public static NativePropNet getCompiledNet(Tuple3<String, Integer, Map<Dob,Integer>> needed) {
        String fullName = needed._1;
        int size = needed._2;
        Map<Dob,Integer> propIndices = needed._3;
        try {
            System.out.println("[Native] Loading class " + fullName);
            Class<?> clazz = Class.forName(fullName);
            Constructor<?> ctor = clazz.getConstructor(Integer.TYPE, Map.class);
            NativePropNet nativeNet =
                    (NativePropNet) ctor.newInstance(new Object []{size, propIndices});
            return nativeNet;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Tuple3<String, Integer, Map<Dob,Integer>> compile(PropNet vanilla) {
        // setup stuff we need
        Map<Node,Integer> indices = getIndices(vanilla);
        Map<Dob,Integer> propIndices = getPropIndices(vanilla.props, indices);
        List<Node> top = vanilla.tnet;

        // create class/lib name
        String className = makeClassName();
        System.out.println("[Native] Class name: " + className);
        // compile
        compileJavaClass(className);
        compileJNI(className, top, indices);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // try making the class
        String fullName = fullClassName(packageName, className);

        return new Tuple3<String, Integer, Map<Dob, Integer>>(fullName, top.size(), propIndices);
    }

    public static String makeClassName() {
        return "NativePropNet" + (rand.nextLong() >>> 1);
    }

    public static String fullClassName(String packageName, String className) {
        return packageName + "." + className;
    }

    public static String mangledClassName(String fullClassName) {
        return fullClassName.replace(".","_");
    }

    public static void writeline(BufferedWriter out, String line) throws IOException {
        out.write(line + "\n");
        out.flush();
    }

    public static String javaName(String className) {
        return className + ".java";
    }

    public static String srcName(String className) {
        return className + ".c";
    }

    public static String libName(String className) {
        return System.mapLibraryName(className);
    }

    public static String macroName(String className) {
        return "__" + className + "__";
    }

    public static void compileJavaClass(String className) {
        try {
            // write source file
            BufferedWriter out = new BufferedWriter(new FileWriter(javaSourceDir + javaName(className)));
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            writeline(out, "package " + packageName + ";");
            writeline(out, "import propnet.nativecode.NativePropNet;");
            writeline(out, "import rekkura.logic.model.Dob;");
            writeline(out, "import java.util.Map;");
            writeline(out, "import java.nio.ByteBuffer;");
            writeline(out, "");
            writeline(out, "// Creation time: " + timeStamp);
            writeline(out, "public final class " + className + " extends NativePropNet {");
            writeline(out, "\tpublic " + className + "(int size, Map<Dob,Integer> props) {");
            writeline(out, "\t\tsuper(size, props);");
            writeline(out, "\t}\n");
            writeline(out, "\tstatic {");
            writeline(out, "\t\tSystem.loadLibrary(\"" + className + "\");");
            writeline(out, "\t}\n");

            // declare native advance and wipe
            writeline(out, "\tnative void advance(ByteBuffer net);");
            writeline(out, "");

            // override advance
            writeline(out, "\t@Override");
            writeline(out, "\tpublic final void advance() {");
            writeline(out, "\t\tadvance(net);");
            writeline(out, "\t}");
            writeline(out, "}");

            out.close();
            // compile
            String cmd = "javac " + javacFlags + " " + javaSourceDir + javaName(className);
            System.out.println("[Native] Compiling Java class file... [command: " + cmd + "]");
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compileJNI(String className, List<Node> top, Map<Node,Integer> indices) {
        String header = "#ifndef " + macroName(className) + "\n#define " + macroName(className) + "\n";
        String footer = "#endif";
        String include = "#include <jni.h>\n#include <string.h>\n";
        String JNIVoidHeader = "JNIEXPORT void JNICALL Java_";
        String wipeDecl = JNIVoidHeader + mangledClassName(fullClassName(packageName, className)) +
                "_wipeNative(JNIEnv* env, jobject obj, jobject buf)";
        String advanceDecl = JNIVoidHeader + mangledClassName(fullClassName(packageName, className)) +
                "_advance(JNIEnv *env, jobject obj, jobject buf)";
        String directAccess = "unsigned char* net = (unsigned char *) (*env)->GetDirectBufferAddress(env, buf);";

        try {
            // write source file
            BufferedWriter out = new BufferedWriter(new FileWriter(cSourceDir + srcName(className)));
            writeline(out, header);
            writeline(out, include);

            // write advance
            writeline(out, advanceDecl);
            writeline(out, "{");
            writeline(out, "\t" + directAccess);
            for (Node n : top) {
                StringBuilder nextLine = getAdvanceLine(n, indices);
                if (debug)
                    System.out.println("[NATIVE] Node: " + indices.get(n) + " line: " + nextLine);
                if (nextLine.length() > 0) {
                    writeline(out, "\t" + nextLine.toString());
                }
            }
            writeline(out, "}");
            writeline(out, footer);
            out.close();

            // compile
            String cmd = "gcc " + gccFlags + (cSourceDir + srcName(className)) + " -o " +
                    (libDir + libName(className));
            System.out.println("[Native] Compiling JNI lib... [command: " + cmd + "]");
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gives code that tells how to shift bit of node n to position p
     * @param n
     * @param shiftTo
     *      target bit pos (since byte, range[0,7])
     * @param indices
     *      mapping from node to index in topo order
     * @return
     */
    public static StringBuilder shiftTo(Node n, int shiftTo, Map<Node,Integer> indices) {
        Preconditions.checkArgument(indices.containsKey(n));
        StringBuilder ret = new StringBuilder();
        ret.append("(");
        int idx = indices.get(n);
        int bi = idx / Byte.SIZE;
        int bp = idx % Byte.SIZE;
        int d = shiftTo - bp;
        ret.append(nodeBlock(n, indices));
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

    public static StringBuilder nodeBlock(Node n, Map<Node,Integer> indices) {
        StringBuilder ret = new StringBuilder();
        int idx = indices.get(n);
        ret.append("net[").append(idx / Byte.SIZE).append("]");
        return ret;
    }

    public static StringBuilder getAdvanceLine(Node n, Map<Node,Integer> indices) {
        StringBuilder ret = new StringBuilder();
        if (n.inputs.isEmpty())
            return ret;
        Preconditions.checkArgument(indices.containsKey(n));
        ret.append(nodeBlock(n, indices));
        ret.append("|=");
        StringBuilder rhs = getRHS(n, indices);
        ret.append(rhs);
        ret.append(";");
        return ret;
    }

    public static StringBuilder getRHS(Node n, Map<Node,Integer> indices) {
        Preconditions.checkArgument(indices.containsKey(n));
        StringBuilder rhs = new StringBuilder();
        int idx = indices.get(n);
        int pos = idx % Byte.SIZE;
        StringBuilder target = new StringBuilder("0x");
        target.append(Integer.toHexString(1 << pos));
        char op = '|';
        if (n.fn == NodeFns.AND)
            op = '&';

        if (n.fn == NodeFns.NOT) {
            Preconditions.checkArgument(n.inputs.size() == 1);
        }

        rhs.append("(");
        for (Node in : n.inputs)
            rhs.append(shiftTo(in, pos, indices)).append(op);

        // should always be the case
        if (!n.inputs.isEmpty())
            rhs.deleteCharAt(rhs.length()-1);
        rhs.append(")");
        rhs.append("&");
        rhs.append(target);

        if (n.fn == NodeFns.NOT) {
            StringBuilder ret = new StringBuilder();
            ret.append("(").append(rhs).append(")").append("^").append(target);
            return ret;
            //rhs.append("^").append(target);
        }
        return rhs;
    }
}
