package jcog.data.byt;

import jcog.Util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by me on 12/20/16.
 */
public class RawByteSeq implements ByteSeq, Serializable /*implements CharSequence*/ {
    public final byte[] bytes;

//        protected RawByteSeq(int capacity) {
//            this(new byte[capacity]);
//        }

    public RawByteSeq(DynByteSeq bytes) {
        this.bytes = bytes.array();
    }

    public RawByteSeq(byte[] bytes) {
        this.bytes = bytes;
    }

    public RawByteSeq(String s) {
        this(s.getBytes());
    }

    @Override
    public int hashCode() {
        return (int) Util.hashELF(bytes, 1);
    }

    @Override
    public boolean equals(Object obj) {
        return Arrays.equals(bytes, ((RawByteSeq)obj).bytes);
    }

    @Override
    public void toArray(byte[] c, int offset) {
        System.arraycopy(bytes, 0, c, offset, length());
    }

    @Override
    public byte[] array() {
        return bytes;
    }

    @Override
    public int length() {
        return bytes.length;
    }

    @Override
    public byte at(int index) {
        return this.bytes[index];
    }

    @Override
    public ByteSeq subSequence(int start, int end) {
        return subSeq(start, end);
    }

    public ByteSeq subSeq(int start, int end) {
        if (end - start == 1)
            return new OneByteSeq(at(start));

        if (start == 0 && end == length())
            return this; //no change

        return new WindowByteSeq(bytes, start, end);
    }

    public String toString() {
        return new String(bytes);
    }

}
