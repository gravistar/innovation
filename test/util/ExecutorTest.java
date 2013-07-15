package util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;

/**
 * User: david
 * Date: 7/14/13
 * Time: 5:44 PM
 * Description:
 */
public class ExecutorTest {
    @Test
    public void test() {
        long wait = 5000;
        try {
            ExecutorService service = Executors.newSingleThreadExecutor();
            long starttime = System.currentTimeMillis();
            List<Callable<Object>> task = Lists.<Callable<Object>>newArrayList(new Callable() {
                @Override
                public Object call() throws Exception {
                    int i=0;
                    while(i == 0) {
                        i*=2;
                    }
                    return null;
                }
            });
            List<Future<Object>> futures = service.invokeAll(task, wait, TimeUnit.MILLISECONDS);
            Future<Object> f = futures.get(0);
            long futureMakeTime = System.currentTimeMillis();
            System.out.println("Future made after: " + (futureMakeTime - starttime) + " ms");
            try {
                f.get();
            } catch (ExecutionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            long endtime = System.currentTimeMillis();
            System.out.println("Task ran for " + (endtime - starttime) + " ms");
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
