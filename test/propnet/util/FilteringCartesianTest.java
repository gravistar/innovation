package propnet.util;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/7/13
 * Time: 10:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilteringCartesianTest {
    @Test
    public void test1Space() {
        List<List<Integer>> subspaces = Lists.newArrayList();
        List<Integer> subspace = Lists.newArrayList(1,2);
        subspaces.add(subspace);
        FilteringCartesianIterator.FilteringCartesianIterable<Integer> fci = new FilteringCartesianIterator.FilteringCartesianIterable<Integer>
                (subspaces, new FilteringCartesianIterator.FilterFn<Integer>() {
                    @Override
                    public boolean pred(List<Integer> current, Integer x) {
                        return true;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                });

        int count = 0;
        for (List<Integer> x : fci) {
            System.out.println("Element " + x);
            count++;
        }
        assertEquals(2, count);
    }


    @Test
    public void test1() {

        int spaces[][] = {
                {1,2,3,4},
                {5,6,7,8},
                {9,10,11,12}
        };

        List<List<Integer>> subspaces = Lists.newArrayList();
        for (int i=0; i<spaces.length; i++) {
            List<Integer> space = Lists.newArrayList();
            for (int j=0; j<spaces[i].length; j++)
                space.add(spaces[i][j]);
            subspaces.add(space);
        }

        FilteringCartesianIterator<Integer> fci = new FilteringCartesianIterator<Integer>(subspaces,
                new FilteringCartesianIterator.FilterFn<Integer>() {
            @Override
            public boolean pred(List<Integer> current, Integer x) {
               return x%2 == 0;
            }
        });

        int count = 0;
        while(fci.hasNext()) {
            count++;
            System.out.println("Element " + count + ": " + fci.next());
        }

        assertEquals(8, count);
    }

    @Test
    public void emptyTest(){
        List<List<Integer>> subspaces = Lists.newArrayList();
        FilteringCartesianIterator.FilteringCartesianIterable<Integer> fci = new
                FilteringCartesianIterator.FilteringCartesianIterable<Integer>(subspaces,
                new FilteringCartesianIterator.FilterFn<Integer>() {
                    @Override
                    public boolean pred(List<Integer> current, Integer x) {
                        return x%2 == 0;
                    }
                });
        int count = 0;
        for (List<Integer> a : fci)
            count++;
        assertEquals(0, count);
    }


}
