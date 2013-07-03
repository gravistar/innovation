package propnet.nativecode;

/**
 * User: david
 * Date: 6/17/13
 * Time: 8:12 AM
 * Description:
 *      Some functions for debugging
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

}
