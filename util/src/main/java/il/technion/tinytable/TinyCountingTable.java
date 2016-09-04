package il.technion.tinytable;


import il.technion.tinytable.bit.Chains;
import il.technion.tinytable.hash.FingerPrint;
import il.technion.tinytable.hash.RankIndexing;
import org.eclipse.collections.api.list.primitive.IntList;

import java.util.function.Function;

/**
 * A simple application, that extend TinyTableCBF class with the capability to efficiently handle variable sized values.
 * The basic idea is that TinyTableCBF is used as is, however we force fingerprint items to always have 1 in their lower bit.
 * That way we can distinguish them from counters items (that are always stored with 0 in their lower bit).
 * <p>
 * TinyTable remains the same in functionality to store items.  Note that if this class is used, do not use TinyTable's functions
 * yourself when using it.
 *
 * @author gilga
 */
public class TinyCountingTable extends TinyTable {

    public TinyCountingTable(int itemsize, int bucketcapacity, int nrBuckets) {
        super(itemsize, bucketcapacity, nrBuckets);
    }

    public long get(String item) {
        return get(item.getBytes());
    }

    public long get(byte[] item) {
        return get(hash.hash(item));
    }

    public <X> long get(X x, Function<X,byte[]> toBytes) {
        return get(toBytes.apply(x));
    }

    public long get(FingerPrint fpaux) {
        return this.size(fpaux.bucketId, fpaux.chainId, fpaux.fingerprint | 1L);
    }


    /**
     * If the value of an item is reduced we may need a new number of items.
     * We therefore update the index and remove an arbitrary item from its chain.
     *
     * @param bucketId
     * @param chainId
     */
    private void shrinkChain(int bucketId, int chainId) {
		this.removeAndShrink(bucketId);
		int bucket = 0;
		for (int i = bucketId + 1; i < bucketId + this.I0.length; i++) {
            bucket = (i) % this.I0.length;
            if (A[bucket] > 0) {
                removeAndShrink(bucket);
                A[bucket]--;
                continue;
            } else {

                break;
            }
        }
        removeItemFromIndex(new FingerPrint(bucketId, chainId, 1L));
    }

    /**
     * The basic function of this functionality, stores a value of up to 64 bits and associate it with the item.
     *
     * @param item
     * @param value
     */
    public void set(String item, long value) {
        set(item.getBytes(), value);
    }

    public <X> void set(X item, Function<X,byte[]> toBytes, long value) {
        set(toBytes.apply(item), value);
    }

    public void set(byte[] item, long value) {
        set(hash.hash(item), value);
    }

    public void set(FingerPrint fpaux, long value) {
        fpaux.fingerprint |= 1L;
        if (!this.contains(fpaux)) {
            this.add(fpaux);
        }
        long[] chain = this.getChain(fpaux.bucketId, fpaux.chainId);
        chain = Chains.storeValue(chain, fpaux.fingerprint, this.itemSize, value);
        this.storeChain(fpaux.bucketId, fpaux.chainId, chain);
    }

    /**
     * After storing a value the number of items in the chain may change, this function adjust the number to be sufficient for all fingerprint items and counter items.
     *
     * @param bucketId
     * @param chainId
     * @param items
     * @return
     */
    private IntList adjustChainToItems(int bucketId, int chainId,
                                       long[] items) {
        IntList chain = RankIndexing.getChain(chainId, I0[bucketId], IStar[bucketId]);
        FingerPrint fpaux = new FingerPrint(bucketId, chainId, 1L);
        // if the chain is shorter than needed we add dummy items.
        if (chain.size() < items.length) {
            int diff = items.length - chain.size();
            while (diff > 0) {
                this.add(fpaux);
                diff--;
            }
        }
        // if the chain is longer than needed we remove items.
        if (chain.size() > items.length) {
            int diff = chain.size() - items.length;
            while (diff > 0) {
                shrinkChain(bucketId, chainId);
                diff--;
                this.nrItems--;

            }

        }
        chain = RankIndexing.getChain(chainId, I0[bucketId], IStar[bucketId]);
        return chain;
    }

    /**
     * stores the following chain of items, in TinyTable.
     *
     * @param bucketId
     * @param chainId
     * @param items
     */

    private void storeChain(int bucketId, int chainId, long[] items) {
        // we change the chain in the table to be the same size as the new chain.
        IntList chainIndexes = adjustChainToItems(bucketId, chainId, items);
        // at this point we are sure that they are the same size.
//		System.out.println(Items[bucketId]);
//		Assert.assertTrue(chainIndexes.size() == items.length);

        //then we put the items in the appropriate indices.
        for (int i = 0; i < items.length; i++) {
            int itemOffset = chainIndexes.get(i);
            if (itemOffset < 0)
                return;
            this.set(bucketId, itemOffset, items[i]);
        }
        return;

    }
}
