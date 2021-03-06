package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.video.Draw;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public class Widget extends Bordering {


    public static final int RAW = 0;
    public static final int META = 1;




    /**
     * z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     * zero: neutral state, default for components
     * negative: how raised
     */
    protected float dz = 0;



//MARGIN
//    @Override
//    public void setParent(Surface s) {
//        super.setParent(s);
//
//        float proportion = 0.9f;
//        float margin = 0.0f;
//        //float content = 1f - margin;
//        float x = margin / 2f;
//
//        Surface content = content();
//        content.scaleLocal.set(proportion, proportion);
//        content.translateLocal.set(x, 1f - proportion, 0);
//
//    }

    /** if non-null, a finger which is currently hovering on this */
    @Nullable Finger touchedBy;


        /**
         * indicates current level of activity of this component, which can be raised by various
         * user and system actions and expressed in different visual metaphors.
         * positive: active, hot, important
         * zero: neutral
         * negative: disabled, hidden, irrelevant
         */
        float temperature = 0;

        @Override
        public void prePaint(int dtMS) {

            //hover glow
            if (dtMS > 0) {
                if (touchedBy != null) {
                    temperature = Math.min(1f, temperature + dtMS / 100f);
                }

                if (temperature != 0) {
                    float decayRate = (float) Math.exp(-dtMS / 1000f);
                    temperature *= decayRate;
                    if (Math.abs(temperature) < 0.01f)
                        temperature = 0f;
                }
            }


        }


        @Override
        protected void paintBelow(GL2 gl) {

            if (Widget.this.tangible()) {
                float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
                float bri = 0.25f * dim;
                float r, g, b;
                r = g = b = bri;


                float t = this.temperature;
                if (t >= 0) {
                    //fire palette TODO improve
                    //            r += t / 2f;
                    //            g += t / 4f;

                    r += t / 4f;
                    g += t / 4f;
                    b += t / 4f;
                } else {
                    //ice palette TODO improve
                    b += -t / 2f;
                    g += -t / 4f;
                }

                gl.glColor4f(r, g, b, 0.5f);

                Draw.rect(gl, bounds);
            }

            //rainbow backgrounds
            //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
            //Draw.rect(gl, 0, 0, 1, 1);
        }

        @Override
        public Surface tryTouch(Finger finger) {
            Surface x = super.tryTouch(finger);
            if (x == this)
                return this;
            return x;
        }

        @Override
        protected final void paintIt(GL2 gl) {
            paintWidget(gl, bounds);
        }

        @Override
        protected void paintAbove(GL2 gl, int dtMS) {
            if (touchedBy != null) {
                Draw.colorHash(gl, getClass().hashCode(), 0.5f + dz / 2f);
                //gl.glColor3f(1f, 1f, 0f);
                gl.glLineWidth(6 + dz * 6);
                Draw.rectStroke(gl, x(), y(), w(), h());
            }
        }





    public boolean isTouched() {
        return touchedBy!=null;
    }

    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

    }

    public Widget() {
        this(new EmptySurface());
    }

    public Widget(Surface content) {
        super();

        edge(0.1f);


        content(content);
//        states(
//            ()-> content,
//            ()->{
//                //meta
//                return new MetaFrame(this);
//            }
//        );
    }


    @Nullable public Surface content() {
        return getSafe(C);
    }


//    public final Widget add(Surface x) {
//        content.add(x);
//        return this;
//    }
//    public final Widget remove(Surface x) {
//        content.remove(x);
//        return this;
//    }


    @Override
    public boolean tangible() {
        return true;
    }

    public void onFinger(@Nullable Finger finger) {

        touchedBy = finger;
        if (finger != null) {

            if (finger.clickedNow(1, this)) { //released middle button

//                int curState = switched;
//                int nextState = nextState(curState);
//                state(nextState); //toggle

            } else if (finger.releasedNow(2 /*right button*/, this)) {
//                /** hold to zoom */
//                finger.tryFingering(new FingerDragging(2) {
//
//                    SurfaceRoot r = root();
//
//                    @Override
//                    public void start(Finger f) {
//
//                    }
//
//                    @Override
//                    protected boolean drag(Finger f) {
//                        ((Ortho)r).zoom(bounds, 0.5f);
//                        return true;
//                    }
//                });

                /** auto-zoom */
                SurfaceRoot r = root();
                if (r!=null) {
//                    switch (curState) {
//                        case STATE_META:
                            r.zoom(this);
//                            break;
//                        case STATE_ZOOM:
//                            r.unzoom();
//                            break;
//                    }
                }

            }

        }
    }

    public Widget content(Surface next) {
        super.content(next);
        return this;
    }

    private int nextState(int curState) {
        switch (curState) {
            case RAW:
                return META;
            case META:
                return RAW;
            default:
                throw new UnsupportedOperationException();
        }
    }

}
