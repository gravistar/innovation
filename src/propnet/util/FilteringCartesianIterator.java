package propnet.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/3/13
 * Time: 10:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilteringCartesianIterator<T> implements Iterator<List<T>> {
    public static interface FilterFn<T> {
        public boolean pred(List<T> current, T x);
    }
    public final int spaceSize;
    public final int[] positions;
    public List<List<T>> fullSubspaces;
    public ArrayDeque<List<T>> currentSubspaces;
    public ArrayDeque<T> current;
    public final FilterFn<T> filter;

    public FilteringCartesianIterator(List<List<T>> subspaces, FilterFn<T> filter) {
        this.fullSubspaces = subspaces;
        this.spaceSize = subspaces.size();
        this.positions = new int[spaceSize];
        this.currentSubspaces = Queues.newArrayDeque();
        this.current = Queues.newArrayDeque();
        this.filter = filter;
        initialize();
    }

    @Override
    public boolean hasNext() {
        return currentSubspaces.size() == spaceSize && spaceSize > 0;
    }

    @Override
    public List<T> next() {
        if (currentSubspaces.size() < spaceSize)
            throw new NoSuchElementException();
        List<T> ret = extractCurrent();
        advance();
        return ret;
    }

    @Override
    public void remove() {
        // stub
    }

    public List<T> extractCurrent() {
        List<T> ret = Lists.newArrayList();
        ret.addAll(current);
        return ret;
    }


    private void initialize() {
        Arrays.fill(positions,0);
        if (fullSubspaces.isEmpty())
            return;
        List<T> firstSubspace = filterSubspace(fullSubspaces.get(0));
        if (!firstSubspace.isEmpty()) {
            currentSubspaces.addLast(firstSubspace);
            current.addLast(firstSubspace.get(0));
        }
        if (spaceSize > 1)
            advance();
    }

    private void advance() {
        boolean increment = false;
        if (currentSubspaces.size() == spaceSize) {
            int lastSubspacePos = positions[currentSubspaces.size() - 1];

            // if can increment the pos in last subspace, do it
            if (lastSubspacePos < currentSubspaces.getLast().size() - 1) {
                positions[currentSubspaces.size() - 1]++;
                current.removeLast();
                current.addLast(currentSubspaces.getLast().get(positions[currentSubspaces.size() - 1]));
                return;
            }

            // can't increment
            currentSubspaces.removeLast();
            current.removeLast();
            increment = true;
        }

        while (currentSubspaces.size() < spaceSize) {
            // nothing left
            if (currentSubspaces.isEmpty() && increment)
                return;

            int subspaceIdx = currentSubspaces.size() - 1;
            if (increment)
                positions[subspaceIdx]++;
            int elemIdx = positions[subspaceIdx];

            // backtrack
            if (elemIdx == currentSubspaces.getLast().size()) {
                currentSubspaces.removeLast();
                current.removeLast();
                increment = true;
                continue;
            }

            current.removeLast();
            current.addLast(currentSubspaces.getLast().get(elemIdx));

            List<T> nextSubspace = filterSubspace(fullSubspaces.get(subspaceIdx+1));
            if (!nextSubspace.isEmpty()) {
                currentSubspaces.add(nextSubspace);
                positions[currentSubspaces.size()-1] = 0;
                current.add(nextSubspace.get(0));
                increment = false;
            } else {
                increment = true;
            }
        }
    }

    private List<T> filterSubspace(List<T> subspace) {
        List<T> ret = Lists.newArrayList();
        List<T> current = extractCurrent();
        for (T elem : subspace)
            if (filter.pred(current, elem))
                ret.add(elem);
        return ret;
    }


    public static class FilteringCartesianIterable<T> implements Iterable<List<T>> {

        public final List<List<T>> subspaces;
        public final FilterFn<T> filterFn;

        public FilteringCartesianIterable(List<List<T>> subspaces, FilterFn<T> filterFn) {
            this.subspaces = subspaces;
            this.filterFn = filterFn;
        }

        @Override
        public Iterator<List<T>> iterator() {
            return new FilteringCartesianIterator<T>(subspaces, filterFn);
        }
    }

}
