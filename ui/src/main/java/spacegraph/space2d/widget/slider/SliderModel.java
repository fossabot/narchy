package spacegraph.space2d.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

/**
 * abstract 1D slider/scrollbar
 */
public class SliderModel extends Surface {


    /** dead-zone at the edges to latch min/max values */
    static private final float margin =
            //0.02f;
            0.0001f;

    //private static final float EPSILON = Float.MIN_NORMAL;

    @Nullable ObjectFloatProcedure<SliderModel> change;

    private float p;

    public SliderModel(float p) {
        this.p = p;
    }


    public SliderModel on(ObjectFloatProcedure<SliderModel> c) {
        this.change = c;
        return this;
    }

    @Override
    protected void paint(GL2 gl, int dtMS) {
        Draw.bounds(gl, bounds, (g)-> ui.draw(this.p, g));
    }

    SliderUI ui = SolidLeft;

    public interface SliderUI {
        void draw(float p, GL2 gl);

        /** resolves the 2d hit point to a 1d slider value */
        float p(v2 hitPoint);

    }

    public SliderModel type(SliderUI draw) {
        this.ui = draw;
        return this;
    }

    @Override
    public Surface tryTouch(Finger finger) {


        if (finger!=null && finger.pressing(0)) {
            if (finger.tryFingering(new FingerDragging(0) {
                @Override protected boolean drag(Finger f) {
                    v2 hitPoint = finger.relativePos(SliderModel.this);
                    _set(ui.p(hitPoint));
                    return true;
                }
            }))
                return this;
        }
        return null; //transparent
    }


    void _set(float p) {
        changed(p);
    }

    protected void changed(float p) {
        if (this.p == this.p && Util.equals(this.p, p, Float.MIN_NORMAL))
            return;

        this.p = p;

        if (change!=null)
            change.value(this, value());
    }

    public float value() {
        return v(p);
    }

    public void value(float v) {
        this.p = p(v);
    }

    /** normalize: gets the output value given the proportion (0..1.0) */
    protected float v(float p) {
        return p;
    }

    /**
     * unnormalize: gets proportion from external value
     */
    protected float p(float v) {
        return v;
    }

    static float pHorizontal(v2 hitPoint) {
        float x = hitPoint.x;
        if (x <= margin)
            return 0;
        else if (x >= (1f-margin))
            return 1f;
        else
            return x;
    }
    static float pVertical(v2 hitPoint) {
        float y = hitPoint.y;
        if (y <= margin)
            return 0;
        else if (y >= (1f-margin))
            return 1f;
        else
            return y;
    }

    static final SliderUI SolidLeft = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float W = 1; //TODO use correct w() and h()
            float H = 1;
            float barSize = W * p;

            //inset
            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, barSize, 0, W-barSize, H);

            //growing knob from left
            gl.glColor4f(0.75f * 1f - p, 0.75f * p, 0f, 0.8f);
            Draw.rect(gl, 0, 0, barSize, H);
        }

        @Override
        public float p(v2 hitPoint) {
            return pHorizontal(hitPoint);
        }
    };


    public static final SliderUI KnobHoriz = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float knobWidth = 0.05f;
            float W = 1;
            float H = 1;
            float x = W * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, 0, 0, W, H);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(gl, x-knobWidth/2f, 0, knobWidth, H);
        }

        @Override
        public float p(v2 hitPoint) {
            return pHorizontal(hitPoint);
        }
    };

    public static final SliderUI KnobVert = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float knobWidth = 0.05f;
            float W = 1;
            float H = 1;
            float x = W * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, 0, 0, W, H);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(gl, 0, x-knobWidth/2f, W, knobWidth);
        }

        @Override
        public float p(v2 hitPoint) {
            return pVertical(hitPoint);
        }
    };


//    public void denormalized(double... n) {
//        double mn = min.get();
//        double mx = max.get();
//
//        for (int i = 0; i < n.length; i++) {
//            double nn = n[i];
//            double v = (nn) * (mx - mn) + mn;
//            if (v < mn) v = mn; //clip to bounds
//            if (v > mx) v = mx;
//            value(i, v);
//        }
//
//    }
//
//    public double[] normalized() {
//        double[] n = normalized;
//        if (n == null) {
//            n = normalized = new double[dimensions];
//        }
//
//        //TODO only compute this if invalidated
//        for (int i = 0; i < dimensions; i++) {
//            n[i] = p(value[i].get());
//        }
//
//        return n;
//    }


//
//    public interface Vis {
//
//        /**
//         * @param vector vector of the normalized proportional positions, between 0 and 1.0
//         * @param canvas target canvas
//         * @param w      parent width
//         * @param h      parent height
//         * @param g      re-usable graphics context
//         */
//        void redraw(double[] vector, Canvas canvas, double w, double h, GraphicsContext g) throws RuntimeException;
//
//        static double getFirstAndOnlyDimension(double[] vector) {
//            if (vector.length != 1) throw new RuntimeException("invalid dimension");
//            return vector[0];
//        }
//    }
//
//
//    public static final Vis BarSlider = (vector, canvas1, W, H, g1) -> {
//
//
//        double p = Vis.getFirstAndOnlyDimension(vector);
//
//        double margin = 4;
//        double mh = margin / 2.0;
//
//
//        g1.setLineWidth(mh * 2);
//        g1.setStroke(Color.GRAY);
//        g1.strokeRect(0, 0, W, H);
//
//        g1.setLineWidth(0);
//        double hp = 0.5 + 0.5 * p;
//        g1.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double barSize = W * p;
//        g1.fillRect(mh, mh, barSize - mh * 2, H - mh * 2);
//    };
//
//    public static final Vis NotchSlider = (v, canvas, W, H, g) -> {
//
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        double margin = 4;
//
//
//        g.setLineWidth(0);
//
//        //TODO use a x,w calculation that keeps the notch within bounds that none if it goes invisible at the extremes
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double notchRadius = W * 0.1;
//        double mh = margin / 2.0;
//        double barSize = W * p;
//        g.fillRect(mh + barSize - notchRadius, mh, notchRadius * 2, H - mh * 2);
//    };
//
//    public static final Vis CircleKnob = (v, canvas, W, H, g) -> {
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        g.clearRect(0, 0, W, H);
//        //g.setFill(Color.BLACK);
//        //g.fillRect(0, 0, W, H);
//
//        double angleStart = 0;
//        double circumferenceActive = 0.5; //how much of the circumference of the interior circle is active as a dial track
//
//
////        double x = W/2 + (W/2-margin) * Math.cos(theta);
////        double y = H/2 + (H/2-margin) * Math.sin(theta);
////        double t = 4;
//        /*g.setFill(Color.WHITE);
//        g.fillOval(x-t, y-t, t*2,t*2);*/
//
//        double ew = W;
//        double eh = H;
//
//        double np = 0.75 + (p * 0.25);
//
//        //double ews = ew * np, ehs = eh * np; //scale by prop
//
//        g.setFill(Color.DARKGRAY);
//        double ut = (H - eh) / 2.0;
//        double ul = (W - ew) / 2.0;
//        g.fillOval(ul, ut, ew, eh);
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//
//
//        double theta = angleStart + (1 - p) * circumferenceActive * (2 * Math.PI);
//        double atheta = theta * 180.0 / Math.PI; //radian to degree
//        double knobArc = 60;
//        g.fillArc(ul, ut, ew, eh,
//                atheta - knobArc / 2, knobArc, ArcType.ROUND);
//
//
//    };
//
//


//    public NSlider bind(DoubleProperty... p) {
//        ensureDimension(p.length);
//
//        for (int i = 0; i < p.length; i++)
//            p[i].bindBidirectional(value[i]);
//
//        return this;
//    }


//    public static void makeDraggable(final Stage stage, final Node byNode) {
//        final Delta dragDelta = new Delta();
//        byNode.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.HAND);
//                }
//            }
//        });
//        byNode.setOnMouseExited(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.DEFAULT);
//                }
//            }
//        });
//    }


//    public static void main(String[] args) {
//        FX.run((a, b) -> {
//
//            FlowPane p = new FlowPane(16, 16);
//
//            p.getChildren().setAll(
//                    new NSlider("Bar", 256, 96, NSlider.BarSlider, 0.5),
//                    new NSlider("Notch", 128, 45, NSlider.NotchSlider, 0.25),
//                    new NSlider("Notch--", 64, 25, NSlider.NotchSlider, 0.75),
//                    new NSlider("Knob", 256, 256, NSlider.CircleKnob, 0.5),
//                    new NSlider("Ranged", 256, 256, NSlider.BarSlider, 75)
//                            .range(0, 100).on(0, c -> System.out.println(Arrays.toString(c.normalized())))
//            );
//
//
//            b.setScene(new Scene(p, 800, 800));
//            b.show();
//        });
//    }
//
//    private NSlider range(double min, double max) {
//        this.min.set(min);
//        this.max.set(max);
//        return this;
//    }
//    private NSlider on(int dimension, Consumer<NSlider> callback) {
//
//        //TODO save listener so it can be de-registered
//        value[0].addListener(c -> callback.accept(NSlider.this));
//
//        return this;
//    }
//
//    public static class LeftRightDrag implements Control, EventHandler<MouseEvent> {
//
//        private Canvas canvas;
//        private NSlider n;
//
//        @Override
//        public void start(NSlider n) {
//            canvas = n.canvas;
//            this.n = n;
//
//            canvas.setOnMouseDragged(this);
//            canvas.setOnMousePressed(this); //could also work as released
//            canvas.setOnMouseReleased(this);
//
//            canvas.setCursor(Cursor.CROSSHAIR);
//
//        }
//
//        @Override
//        public void stop() {
//            throw new RuntimeException("unimpl");
//        }
//
//        @Override
//        public void handle(MouseEvent e) {
//
//            canvas.setCursor(
//                    (e.getEventType()==MouseEvent.MOUSE_RELEASED) ? Cursor.CROSSHAIR : Cursor.MOVE
//            );
//
//            n.denormalized(e.getX() / canvas.getWidth());
//
//            //System.out.println(dx + " " + dy + " " + value.get());
//
//            e.consume();
//        }
//
//        double p(double dx) {
//            return dx / canvas.getWidth();
//        }
//
//    }
//
}
