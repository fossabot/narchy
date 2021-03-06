package nars.util.signal;

import jcog.signal.ArrayBitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.scalar.Scalar;
import nars.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class CameraSensorTest {

    @Test
    public void test1() {
        int w = 2, h = 2;

        NAR n = NARS.tmp();
        float[][] f = new float[w][h];

        Bitmap2DSensor c = new Bitmap2DSensor($.the("x"),
                new ArrayBitmap2D(f), n);


        n.log();

//        System.out.println("set to NaN");
//        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = Float.NaN;
//        assertEquals(c, f, n);

        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(),n);

        n.run(3); //delay


        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        c.input();
        n.run(3);  //delay
        assertEquals(c, f, n.time(),n);


        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(), n);

        n.run(3); //delay

        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(), n);

        //TODO test individual pixel motion
    }

    static void assertEquals(Bitmap2DSensor c, float[][] f, long when, NAR n) {
        final float tolerance = 0.47f;
        for (int i = 0; i < c.width; i++) {
            for (int j = 0; j < c.height; j++) {
                Scalar p = c.get(i, j);
                Truth t = n.beliefTruth(p, when);
                if (t == null || Math.abs(f[i][j] - t.freq()) > tolerance) {
                    System.err.println("pixel " + p + " incorrect @ t=" + n.time());
                    n.beliefTruth(p, n.time());
                    p.beliefs().print(System.out);
                }
                assertNotNull(t, ()->p.term + " is null");
                Assertions.assertEquals(f[i][j], t.freq(), tolerance, ()->p + " has inaccurate result @ t=" + n.time());
            }
        }
    }

}