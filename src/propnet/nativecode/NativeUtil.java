package propnet.nativecode;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * User: david
 * Date: 6/17/13
 * Time: 8:12 AM
 * Description:
 *      Some functions for management and debugging
 */
public class NativeUtil {

    public static void printBlock(byte b) {
        for (int i=0; i<8; i++)
            System.out.print(i + " ");
        System.out.println();
        for (int i=0; i<8; i++) {
            if ((b & (1<<i)) > 0)
                System.out.print(1);
            else
                System.out.print(0);
            System.out.print(" ");
        }
        System.out.println();
    }

    public static void deleteGeneratedFiles() {
        try {
            Runtime.getRuntime().exec("bash clear.sh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupLibraryPath() {
        System.setProperty( "java.library.path", "compiled/lib" );

        try {
            Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
            fieldSysPath.setAccessible( true );
            fieldSysPath.set( null, null );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
