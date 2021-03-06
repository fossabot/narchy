package nars.gui;

import nars.NAR;
import nars.control.DurService;
import nars.util.TimeAware;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.UnitContainer;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface extends UnitContainer {
    protected final NAR nar;
    DurService on;

    protected DurSurface(Surface x, NAR nar) {
        super(x);
        this.nar = nar;
    }

    abstract protected void update();

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            assert(on == null);
            on = DurService.on(nar, this::update);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            on.off();
            on = null;
            return true;
        }
        return false;
    }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
    }

    public static DurSurface get(Surface x, NAR n, Consumer<TimeAware> eachDur) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                eachDur.accept(n);
            }

            @Override
            public String toString() {
                return "DurSurface[" + x + "," + eachDur + "]";
            }
        };
    }

    public static DurSurface get(Surface narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<TimeAware>)narConsumer);
    }

}
