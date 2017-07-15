package nars.logic;

import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.NARS;
import nars.util.exe.TaskExecutor;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * Created by me on 4/17/17.
 */
public class TestEinsteinsRiddle {

    @Test
    public void testRiddle1() throws IOException, Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = new NARS().exe(new TaskExecutor(256, 256, 0.25f)).get();

        n.termVolumeMax.setValue(1024);
        n.log();
        URL resource = TestEinsteinsRiddle.class.getResource("einsteinsRiddle.nal");
        n.inputNarsese(
            resource.openStream()
        );
        n.run(128);



    }


}
