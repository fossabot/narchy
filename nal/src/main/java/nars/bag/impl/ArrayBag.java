package nars.bag.impl;

import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.util.data.list.FasterList;
import nars.util.data.sorted.SortedArray;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends SortedListTable<V, BLink<V>> implements Bag<V> {

    /** this default value must be changed */
    @NotNull protected BudgetMerge mergeFunction = BudgetMerge.nullMerge;

    private static final float reinsertionThreshold = 0.02f;

    public ArrayBag(int cap) {
        super(BLink[]::new,
                new HashMap<>(cap), //Global.newHashMap(cap),
                SortedArray.SearchType.BinarySearch);
        setCapacity(cap);
    }

    @Override
    protected void removeWeakest(Object reason) {
        remove(weakest()).delete(reason);
    }

    @Override
    public final int compare(@NotNull BLink o1, @NotNull BLink o2) {
        float f1 = o1.priIfFiniteElseNeg1();
        float f2 = o2.priIfFiniteElseNeg1();
        if (f1 < f2)
            return 1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 0;
    }



    /** true iff o1 > o2 */
    static final boolean cmpGT(@NotNull BLink o1, @NotNull BLink o2) {
        float f1 = o1.priIfFiniteElseNeg1();
        float f2 = o2.priIfFiniteElseNeg1();
        return (f1 < f2);
    }
    /** true iff o1 < o2 */
    static final boolean cmpLT(@NotNull BLink o1, @NotNull BLink o2) {
        float f1 = o1.priIfFiniteElseNeg1();
        float f2 = o2.priIfFiniteElseNeg1();
        return (f1 > f2);
    }

    @NotNull
    public Bag<V> merge(@NotNull BudgetMerge mergeFunction) {
        this.mergeFunction = mergeFunction;
        return this;
    }

    @Override
    public final V key(@NotNull BLink<V> l) {
        return l.get();
    }


    //    @Override public V put(V k, Budget b) {
//        //TODO use Map.compute.., etc
//
//        BagBudget<V> v = getBudget(k);
//
//        if (v!=null) {
//            v.set(b);
//            return k;
//        } else {
//            index.put(k, b);
//            return null;
//        }
//    }

    //    protected CurveMap newIndex() {
//        return new CurveMap(
//                //new HashMap(capacity)
//                Global.newHashMap(capacity()),
//                //new UnifiedMap(capacity)
//                //new CuckooMap(capacity)
//                items
//        );
//    }

    @Nullable
    @Override
    public BLink<V> sample() {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> sample(int n, @NotNull Consumer<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

//    @NotNull
//    @Override
//    public Bag<V> filter(@NotNull Predicate<BLink> forEachIfFalseThenRemove) {
//
//        int n = items.size();
//        BLink<V>[] l = items.array();
//        if (n > 0) {
//            for (int i = 0; i < n; i++) {
//                BLink<V> h = l[i];
//                if (!forEachIfFalseThenRemove.test(h)) {
//                    removeKeyForValue(h); //only remove key, we remove the item here
//                    h.delete();
//                    items.remove(i--);
//                    n--;
//                }
//            }
//        }
//        return this;
//    }

    //    public void validate() {
//        int in = ArrayTable.this.size();
//        int is = items.size();
//        if (Math.abs(is - in) > 0) {
////                System.err.println("INDEX");
////                for (Object o : index.values()) {
////                    System.err.println(o);
////                }
////                System.err.println("ITEMS:");
////                for (Object o : items) {
////                    System.err.println(o);
////                }
//
//            Set<V> difference = Sets.symmetricDifference(
//                    new HashSet(((CollectorMap<V, L>) ArrayTable.this).values()),
//                    new HashSet(items)
//            );
//
//            System.err.println("DIFFERENCE");
//            for (Object o : difference) {
//                System.err.println("  " + o);
//            }
//
//            throw new RuntimeException("curvebag fault: " + in + " index, " + is + " items");
//        }
//
////            //test for a discrepency of +1/-1 difference between name and items
////            if ((is - in > 2) || (is - in < -2)) {
////                System.err.println(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
////                /*System.out.println(nameTable);
////                System.out.println(items);
////                if (is > in) {
////                    List<E> e = new ArrayList(items);
////                    for (E f : nameTable.values())
////                        e.remove(f);
////                    System.out.println("difference: " + e);
////                }*/
////                throw new RuntimeException(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
////            }
//    }

    //    /**
//     * Get an Item by key
//     *
//     * @param key The key of the Item
//     * @return The Item with the given key
//     */
//    @Override
//    public V get(K key) {
//        //TODO combine into one Map operation
//        V v = index.get(key);
//        if (v!=null && v.getBudget().isDeleted()) {
//            index.remove(key);
//            return null;
//        }
//        return v;
//    }


//    /**
//     * Choose an Item according to priority distribution and take it out of the
//     * Bag
//     *
//     * @return The selected Item, or null if this bag is empty
//     */
//    @Nullable
//    @Override
//    public BLink<V> pop() {
//        throw new UnsupportedOperationException();
//    }


    /**
     * Insert an item into the itemTable
     *
     * @param i The Item to put in
     * @return The updated budget
     */
    @Nullable
    @Override
    public final BLink<V> put(@NotNull V i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflow) {

        BLink<V> existing = get(i);

        return (existing != null) ?
                    putExists(b, scale, existing, overflow) : (
                       isFull() ?
                            putQueue(i, b, scale) :
                            putNew(i, link(i, b, scale))
                    );

    }

    /**
     * the applied budget will not become effective until commit()
     */
    @NotNull
    private final BLink<V> putExists(@NotNull Budgeted b, float scale, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {

        if (existing != b) {

            /*if (overflow > 0)
                target.charge(overflow);*/
            float o = mergeFunction.merge(existing, b, scale);

            if (overflow != null)
                overflow.add(o);

        }

        return existing;
    }

    protected BLink<V> link(V i, Budgeted b, float scale) {
        if (b instanceof BLink)
            return (BLink)b;
        if (i instanceof BLink)
            return (BLink)i;
        return newLink(i, b, scale);
    }

    protected BLink<V> newLink(V i, Budgeted b, float scale) {
        return new BLink.StrongBLink(i, b, scale);
    }

    @Nullable
    protected BLink<V> putNew(V i, BLink<V> newBudget) {
        newBudget.commit(); //?? necessary
        return put(i, newBudget);
    }

    private final FasterList<BLink<V>> pending = new FasterList();

    protected BLink<V> putQueue(V x, @NotNull Budgeted b, float scale) {
        BLink<V> link = link(x, b, scale);
        pending.add(link);
        return null;
    }


    @NotNull
    @Override
    @Deprecated public Bag<V> commit() {
//        super.commit();
//        sampler.commit(this);
//        return this;
        return commit(null);
    }

    /** applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted */
    @Override public Bag<V> commit(@Nullable Consumer<BLink> each) {
        int s = size();
        if (s > 0) {
            updateExisting(each, s);
        }

        if (!pending.isEmpty())
            addPending();

        return this;
    }

    private void addPending() {
        //add pending items now that the bag is updated
        for (int i1 = 0, pendingSize = pending.size(); i1 < pendingSize; i1++) {
            BLink<V> link = pending.get(i1);
            V key = link.get();
            BLink<V> existing = get(key);
            if (existing != null) {
                mergeFunction.merge(existing, link, 1f);
                if (existing.commit()) {
                    //reorder
                    removeItem(existing);
                    addItem(existing);
                }
            }
            else
                putNew(key, link);

        }
        pending.clear0();
    }

    private void updateExisting(@Nullable Consumer<BLink> each, int s) {
//        int dirtyStart = -1;
        int lowestUnsorted = -1;

        final boolean eachNotNull = each!=null;

        BLink<V>[] l = items.array();
        int t = s - 1;
        @NotNull BLink<V> lower = l[t]; //compares with self below to avoid a null check in subsequent iterations
        for (int i = t; i >= 0; i--) {
            BLink<V> b = l[i];

            if (eachNotNull)
                each.accept(b);

            b.commit();

            if (lowestUnsorted == -1 && cmpGT(b, lower)) {
                lowestUnsorted = i+1;
            }

            lower = b;
        }

        if (lowestUnsorted != -1)  {
            qsort(qsortStack, items.array(), 0 /*dirtyStart - 1*/, items.size());
        } // else: perfectly sorted


        removeDeletedAtBottom();
    }

    private void removeDeletedAtBottom() {
        //remove deleted items they will collect at the end
        int i = size()-1;
        BLink<V> ii;
        BLink<V>[] l = items.array();

        while (i > 0 && (ii = l[i]).isDeleted()) {
            if (removeKeyForValue(ii) == null) {
                //
                //throw new RuntimeException("Bag fault while trying to remove key by item value");

                //exhaustive removal, since the BLink has lost its key
                removeKey((BLink<BLink<V>>) ii);
            }

            //remove by known index rather than have to search for it by key or something
            //different from removeItem which also removes the key, but we have already done that above
            items.remove(i);
            i--;
        }
    }


    final private int[] qsortStack = new int[16];




    public static void qsort(int[] stack, BLink[] c, int start, int size) {
        int left = start, right = size - 1, stack_pointer = -1;
        while (true) {
            int i;
            int j;
            BLink swap;
            if (right - left <= 7) {
                //bubble sort on a region of size less than 8?
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    while (i >= left && cmpGT(c[i], swap)) {
                        c[i + 1] = c[i--];
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {
                int median = (left + right) >> 1;
                i = left + 1;
                j = right;
                swap = c[median];
                c[median] = c[i];
                c[i] = swap;
                if (cmpGT(c[left], c[right])) {
                    swap = c[left];
                    c[left] = c[right];
                    c[right] = swap;
                }
                if (cmpGT(c[i], c[right])) {
                    swap = c[i];
                    c[i] = c[right];
                    c[right] = swap;
                }
                if (cmpGT(c[left], c[i])) {
                    swap = c[left];
                    c[left] = c[i];
                    c[i] = swap;
                }
                BLink temp = c[i];

                while (true) {
                    while (cmpLT(c[++i], temp)) ;
                    while (cmpGT(c[--j], temp)) ;
                    if (j < i) {
                        break;
                    }
                    swap = c[i];
                    c[i] = c[j];
                    c[j] = swap;
                }

                c[left + 1] = c[j];
                c[j] = temp;

                int a, b;
                if ((right - i + 1) >= (j - left)) {
                    a = i;
                    b = right;
                    right = j - 1;
                } else {
                    a = left;
                    b = j - 1;
                    left = i;
                }

                stack[++stack_pointer] = a;
                stack[++stack_pointer] = b;
            }
        }
    }

    //    final Comparator<? super BLink<V>> comparator = (a, b) -> {
//        return Float.compare(items.score(b), items.score(a));
//    };


//        if (!v.hasDelta()) {
//            return;
//        }
//
////
////        int size = ii.size();
////        if (size == 1) {
////            //its the only item
////            v.commit();
////            return;
////        }
//
//        SortedIndex ii = this.items;
//
//        int currentIndex = ii.locate(v);
//
//        v.commit(); //after finding where it was, apply its updates to find where it will be next
//
//        if (currentIndex == -1) {
//            //an update for an item which has been removed already. must be re-inserted
//            put(v.get(), v);
//        } else if (ii.scoreBetween(currentIndex, ii.size(), v)) { //has position changed?
//            ii.reinsert(currentIndex, v);
//        }
//        /*} else {
//            //otherwise, it remains in the same position and a move is unnecessary
//        }*/
//    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public float priMax() {
        return isEmpty() ? 0 : items.first().pri();
    }

    @Override
    public float priMin() {
        return isEmpty() ? 0 : items.last().pri();
    }

//    public final void popAll(@NotNull Consumer<BLink<V>> receiver) {
//        forEach(receiver);
//        clear();
//    }

//    public void pop(@NotNull Consumer<BLink<V>> receiver, int n) {
//        if (n == size()) {
//            //special case where size <= inputPerCycle, the entire bag can be flushed in one operation
//            popAll(receiver);
//        } else {
//            for (int i = 0; i < n; i++) {
//                receiver.accept(pop());
//            }
//        }
//    }

//    public final float priAt(int cap) {
//        return size() <= cap ? 1f : item(cap).pri();
//    }
//

//    public final static class BudgetedArraySortedIndex<X extends Budgeted> extends ArraySortedIndex<X> {
//        public BudgetedArraySortedIndex(int capacity) {
//            super(1, capacity);
//        }
//
//
//        @Override
//        public float score(@NotNull X v) {
//            return v.pri();
//        }
//    }
}


//        if (dirtyStart != -1) {
//            //Needs sorted
//
//            int dirtyRange = 1 + dirtyEnd - dirtyStart;
//
//            if (dirtyRange == 1) {
//                //Special case: only one unordered item; remove and reinsert
//                BLink<V> x = items.remove(dirtyStart); //remove directly from the decorated list
//                items.add(x); //add using the sorted list
//
//            } else if ( dirtyRange < Math.max(1, reinsertionThreshold * s) ) {
//                //Special case: a limited number of unordered items
//                BLink<V>[] tmp = new BLink[dirtyRange];
//
//                for (int k = 0; k < dirtyRange; k++) {
//                    tmp[k] = items.remove( dirtyStart /* removal position remains at the same index as items get removed */);
//                }
//
//                //TODO items.get(i) and
//                //   ((FasterList) items.list).removeRange(dirtyStart+1, dirtyEnd);
//
//                for (BLink i : tmp) {
//                    if (i.isDeleted()) {
//                        removeKeyForValue(i);
//                    } else {
//                        items.add(i);
//                    }
//                }
//
//            } else {
//            }
//        }
