package util;

import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/12/13
 * Time: 9:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilePathTest {
    @Test
    public void test1() {
        System.out.println("Working directory = " + System.getProperty("user.dir"));
    }
    public void advanceNative(ByteBuffer net) {}
    @Test
    public void test2() {

        for (Method m : FilePathTest.this.getClass().getMethods()) {
            System.out.println("Method name: " + m.getName());
            for (TypeVariable t : m.getTypeParameters())
                System.out.println("t: " + t);
            for (Class c : m.getParameterTypes())
                System.out.println("c: " + c);
            System.out.println();
        }
    }
}
