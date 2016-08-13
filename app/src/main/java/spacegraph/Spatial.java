package spacegraph;

import com.jogamp.opengl.GL2;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.constraint.TypedConstraint;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public abstract class Spatial<X> implements BiConsumer<GL2, Dynamic> {


    public final X key;
    public final int hash;


    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    transient public short order;



    public Spatial() {
        this(null);
    }

    public Spatial(X k) {
        this.key = k!=null ? k : (X) this;
        this.hash = k!=null ? k.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {

        return key + "<" +
                //(body!=null ? body.shape() : "shapeless")  +
                ">";
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || key.equals(((Spatial) obj).key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    public boolean preactive;


    /** returns true if this is an initialization cycle, or false if it is a subsequent one (already initialized) */
    public void update(SpaceGraph<X> s) {
        preactive = true;
    }

    public void update(Dynamics world) {
        //create and update any bodies and constraints
    }

    public final boolean active(short nextOrder, Dynamics world) {
        if (active()) {
            this.order = nextOrder;
            update(world);
            return true;
        } else {
            return false;
        }
    }

    public final boolean active() {
        return preactive || order > -1;
    }


    public final void preactivate(boolean b) {
        this.preactive = b;
    }


    public boolean collidable() {
        return true;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onTouch(Collidable body, ClosestRay hitPoint, short[] buttons) {
        return false;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        return false;
    }


    public void stop(Dynamics s) {
        order = -1;
        preactive = false;
    }

    /** schedules this node for removal from the engine, where it will call stop(s) to complete the removal */
    public void stop() {
        order = -1;
    }

    abstract public List<Collidable<X>> bodies();
    abstract public List<TypedConstraint> constraints();

}
