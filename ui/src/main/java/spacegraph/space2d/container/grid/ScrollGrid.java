package spacegraph.space2d.container.grid;

import jcog.TODO;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMove;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;

import static jcog.Util.short2Int;
import static spacegraph.space2d.widget.slider.SliderModel.KnobVert;

/** see also:
 *      https://www.youtube.com/watch?v=ffMpyLQ9YA0
 *      https://www.youtube.com/watch?v=Gi_hoOLFtpo
 * */
public class ScrollGrid<X> extends Bordering {


    static final float MIN_DISPLAYED_CELLS = 0.25f; //max zoom-in hard limit
    static final int MAX_DISPLAYED_CELLS_X = 32;    //max zoom-out hard limit
    static final int MAX_DISPLAYED_CELLS_Y = 32;    //max zoom-out hard limit

    private final GridModel<X> model;
    private final GridRenderer<X> render;
    private final ScrollGridContainer content;




    /** proportional in scale to bounds */
    static final float defaultScrollEdge = 0.12f;

    private final FloatSlider scrollX, scrollY, scaleW, scaleH;

    /**
     * current view, in local grid coordinate
     */
    private volatile RectFloat2D view = RectFloat2D.Zero;

    private static final boolean autoHideScrollForSingleColumnOrRow = true;

    /** layout temporary values */
    private transient float cw, ch, dx, dy;


    public ScrollGrid(GridModel<X> model, GridRenderer<X> render, int visX, int visY) {
        this(model, render);
        view(0, 0, visX, visY);
    }

    /**
     * by default, only the first cell will be visible
     */
    public ScrollGrid(GridModel<X> model, GridRenderer<X> render) {
        super();
        edge(defaultScrollEdge);
        this.model = model;
        this.render = render;

        set(C, new Clipped(content = new ScrollGridContainer<X>() {

            @Override
            protected Surface surface(short x, short y, X nextValue) {
                return render.apply(x, y, nextValue);
            }

            @Override
            protected X value(short sx, short sy) {
                return model.get(sx, sy);
            }

            @Override
            public Surface tryTouch(Finger finger) {
                Surface inner = super.tryTouch(finger);
                final int moveDragButton = 1;
                if ((inner == null || inner == this) && finger.pressing(moveDragButton)) {
                    if (finger.tryFingering(new FingerMove(moveDragButton,
                            0.05f, 0.05f) {

                        final float sx = view.x;
                        final float sy = view.y;

                        @Override
                        public float xStart() {
                            return sx;
                        }

                        @Override
                        public float yStart() {
                            return sy;
                        }

                        @Override
                        public void move(float tx, float ty) {
                            view(tx, ty);
                        }
                    }))
                        return this;
                }
                return inner;
            }

        }));

        set(S, this.scrollX = new FloatSlider("X",
                new FloatSlider.FloatSliderModel(0 /* left initial pos */) {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsX();
                    }
                }
        ).type(SliderModel.KnobHoriz));

        set(E, this.scrollY = new FloatSlider("Y",
                new FloatSlider.FloatSliderModel(0) {
                    @Override
                    public float min() {
                        return 0;
                    }

                    @Override
                    public float max() {
                        return model.cellsY();
                    }
                }
        ).type(KnobVert)); //TODO make vertical


        set(N, new Gridding(
                new EmptySurface(), //HACK
                this.scaleW = new FloatSlider("W",
                        new FloatSlider.FloatSliderModel(1) {

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                //TODO if constrain to bounds ...
                                //return Math.min(model.cellsX()+1, MAX_DISPLAYED_CELLS_X);
                                //return MAX_DISPLAYED_CELLS_X;
                                return Math.min(model.cellsX() * 1.25f, MAX_DISPLAYED_CELLS_X);
                            }
                        }
                ),
                new EmptySurface()  //HACK
        ));
        set(W, new Gridding(
                new EmptySurface(), //HACK
                this.scaleH = new FloatSlider("H",
                        new FloatSlider.FloatSliderModel(1) {

                            @Override
                            public float min() {
                                return MIN_DISPLAYED_CELLS;
                            }

                            @Override
                            public float max() {
                                //TODO if constrain to bounds ...
                                //return Math.min(model.cellsX()+1, MAX_DISPLAYED_CELLS_Y);
                                //return MAX_DISPLAYED_CELLS_Y;
                                return Math.min(model.cellsY() * 1.25f, MAX_DISPLAYED_CELLS_Y);
                            }
                        }
                ).type(KnobVert),
                new EmptySurface()  //HACK
        ));
        scrollX.on((sx, x) -> view(x, view.y));
        scrollY.on((sy, y) -> view(view.x, y));
        scaleW.on((sx, w) -> view(view.x, view.y, w, view.h));
        scaleH.on((sy, h) -> view(view.x, view.y, view.w, h));


        //default initial view: attempt to view entire model
        view(0,0, model.cellsX(), model.cellsY());
    }

    /** the current view */
    public final RectFloat2D view() {
        return view;
    }

    /** set the view window's center of focus, re-using the current width and height */
    public final ScrollGrid<X> view(float x, float y) {
        return view(x, y, view.w, view.h);
    }

    /** set the view window's center and size of focus, in grid coordinates */
    public final ScrollGrid<X> view(RectFloat2D v) {
        return view(v.x, v.y, v.w, v.h);
    }


    /** enables requesting entries from the -1'th row and -1'th column of
     * the model to use as 'pinned' row header cells
     */
    public ScrollGrid<X> setHeader(boolean rowOrColumn, boolean enabled) {
        throw new TODO();
    }

    /** enables or disables certain scrollbar-related features per axis */
    public ScrollGrid<X> setScrollBar(boolean xOrY, boolean scrollVisible, boolean scaleVisible) {
        if (xOrY) {
            scrollX.visible(scrollVisible);
            edge(S, scrollVisible ? defaultScrollEdge : 0);
            scaleW.visible(scaleVisible);
            edge(N, scaleVisible ? defaultScrollEdge : 0);
        } else {
            scrollY.visible(scrollVisible);
            edge(E, scrollVisible ? defaultScrollEdge : 0);
            scaleH.visible(scaleVisible);
            edge(W, scaleVisible ? defaultScrollEdge : 0);
        }
        return this;
    }

    /** limits the scaling range per axis */
    public ScrollGrid<X> setCellScale(boolean xOrY, float minScale, float maxScale) {
        throw new TODO();
    }

    /** limits the viewing range per axis */
    public ScrollGrid<X> setCellView(boolean xOrY, float minCoord, float maxCoord) {
        throw new TODO();
    }

    /**
     * sets the x, y position as a fraction of the entire model bounds.
     * if a coordinate is NaN, that coordinate is not affected,
     * allowing shift of either or both X and Y coordinates of the
     * visible cell window.
     */
    public ScrollGrid<X> view(float x, float y, float w, float h) {

        RectFloat2D v = view;

        float x1, x2, y1, y2;

        float maxW = model.cellsX();
        if (maxW == 1 && autoHideScrollForSingleColumnOrRow) {
            x1 = 0;
            x2 = 1;
            setScrollBar(true, false, false);
        } else {
            if (w < maxW) {
                x = ((((x / maxW) - 0.5f) * 2 /* -1..+1 */ * (1f - w / maxW)) / 2 + 0.5f) * maxW;
            } else {
                x = maxW/2; //align center
            }
            x1 = (x - w / 2);
            x2 = (x + w / 2);
//            if (x1 < 0) {
//                x1 = 0;
//                x2 = w;
//            }
//            if (x2 > maxW) {
//                x2 = maxW;
//                x1 = maxW - w;
//            }
        }

        float maxH = model.cellsY();
        if (maxH == 1 && autoHideScrollForSingleColumnOrRow) {
            y1 = 0;
            y2 = 1;
            setScrollBar(false, false, false);
        } else {

            //h = Math.min(h, maxH);
            if (h < maxH) {
                y = ((((y / maxH) - 0.5f) * 2 /* -1..+1 */ * (1f - h / maxH)) / 2 + 0.5f) * maxH;
            } else {
                //align center
                y = maxH / 2;
            }
            y1 = (y - h / 2);
            y2 = (y + h / 2);
//            if (y1 < 0) {
//                y1 = 0;
//                y2 = h;
//            }
//            if (y2 > maxH) {
//                y2 = maxH;
//                y1 = maxH - h;
//            }
        }

        RectFloat2D nextView = RectFloat2D.XYXY(x1, y1, x2, y2);
        if (!v.equals(nextView, Spatialization.EPSILONf)) {
//            sliderX.value(px); //for when invoked by other than the slider
//            sliderY.value(py); //for when invoked by other than the slider
//            scaleW.value(w); //for when invoked by other than the slider
//            scaleH.value(h); //for when invoked by other than the slider
        }

        view = nextView;

        content.layout(view,
                (short) Math.max(0, Math.floor(x1)),
                (short) Math.max(0, Math.floor(y1)),
                (short) Math.min(maxW,Math.ceil(x2 + 1)),
                (short) Math.min(maxH,Math.ceil(y2 + 1))
        ); //layout regardless because the sub-grid position may have changed

        //System.out.println(view + " " + content.x1 + "," + content.y1 + "," + content.x2 + "," + content.y2);

        return this;
    }


    @Override
    public boolean start(SurfaceBase parent) {
        model.start(this);
        return super.start(parent);
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            model.stop(this);
            return true;
        }
        return false;
    }

    public final void refresh() {
        view(view);
    }


    @FunctionalInterface
    public interface GridRenderer<X> {
        Surface apply(int x, int y, X value);
    }



    /** hashes 2D cell entries in 16-bit pairs of x,y coordinates */
    abstract static class ScrollGridContainer<X> extends MutableMapContainer<Integer,X> {

        volatile short x1=0, y1=0, x2=1, y2=1;
        private transient RectFloat2D view;
        private transient float dx, dy, cw, ch;

        public ScrollGridContainer() {
            super();
        }

        /**
         * test if a cell is currently visible
         */
        public boolean cellVisible(short x, short y) {
            return (x >= x1 && x < x2)
                    &&
                    (y >= y1 && y < y2);
        }

        public void layout(RectFloat2D view, short x1, short y1, short x2, short y2) {
            this.view = view;

            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;

            layout();
        }



        @Override
        protected void doLayout(int dtMS) {

            if (parent == null)
                return;

            dx = x();
            dy = y();
            float ww = w();
            float hh = h();
            cw = ww / view.w;
            ch = hh / view.h;


            //remove or hibernate cache entry surfaces which are not visible
            //and set the layout positions of those which are
            cache.forEachValue(e -> {
                int cellID = e.key;
                Surface s = e.surface;

                boolean deleted = false;
                if (s == null) { //remove the unused entry
                    deleted = true;
                } else {
                    short sx = (short) (cellID >> 16);
                    short sy = (short) (cellID & 0xffff);
                    if (!cellVisible(sx, sy)) {
                        deleted = true; //!onCellInvisible(e);
                    }
                }

                if (deleted) {
                    boolean removed = remove(cellID); assert(removed);
                }
            });

            short x1 = this.x1;
            short y1 = this.y1;
            short x2 = this.x2;
            short y2 = this.y2;

            for (short sx = x1; sx < x2; sx++) {
                for (short sy = y1; sy < y2; sy++) {
                    CacheCell<Integer, X> e = set(sx, sy, value(sx, sy), true);
                    if (e!=null) {
                        Surface s = e.surface;
                        if (s!=null)
                            doLayout(s, sx, sy);
                    }
                }
            }


            super.doLayout(dtMS);

        }



        protected void doLayout(Surface s, short sx, short sy) {
            float cx = dx + (sx - view.x + 0.5f) * cw;
            float cy = dy + (sy - view.y + 0.5f) * ch;
            cellVisible(s, cw, ch, cx, cy);
        }

        abstract protected X value(short sx, short sy);

        protected void cellVisible(Surface s, float cw, float ch, float cx, float cy) {
            s.pos(RectFloat2D.XYWH(cx, cy, cw, ch));
        }



        @Override
        public boolean tangible() {
            return true;
        }

        public final void set(short x, short y, @Nullable X v) {
            set(x, y, v, false);
        }

        /**
         * allows a model to asynchronously report changes, which may be visible or not.
         * set 'v' to null to remove an entry (followed by a subsequent non-null 'v'
         * is a way to force rebuilding of a cell.)
         * returns if there was a change
         */
        protected CacheCell<Integer, X> set(short x, short y, @Nullable X nextValue, boolean force) {
            if (!force && !cellVisible(x, y))
                return null;

            return put(short2Int(x,y), nextValue, this::renderer);
        }

        private Surface renderer(int cellID, X value) {
            short sx = (short) (cellID >> 16);
            short sy = (short) (cellID & 0xffff);
            return surface(sx, sy, value);
        }

        abstract protected Surface surface(short x, short y, X nextValue);

    }
}

//package automenta.spacenet.space.object.data;
//
//        import java.util.Map;
//
//        import javolution.util.FastMap;
//        import automenta.spacenet.Starts;
//        import automenta.spacenet.act.Repeat;
//        import automenta.spacenet.space.geom2.Rect;
//        import automenta.spacenet.space.object.widget.slider.Slider;
//        import automenta.spacenet.space.object.widget.slider.Slider.SliderType;
//        import automenta.spacenet.var.number.DoubleVar;
//        import automenta.spacenet.var.number.IfDoubleChanges;
//        import automenta.spacenet.var.number.IntegerVar;
//        import automenta.spacenet.var.vector.IfVector2Changes;
//        import automenta.spacenet.var.vector.Vector2;
//
//
///** displays a sub-matrix of a 2D matrix array of rectangles, useful for grid arrangements and (editable) text rectangles */
//public class MatrixRect extends Rect implements Starts {
//
//
//
//    DoubleVar cellAspect = new DoubleVar(1.0);
//
//
//    DoubleVar minX = new DoubleVar(0);
//    DoubleVar minY = new DoubleVar(0);
//    DoubleVar maxX = new DoubleVar(0);
//    DoubleVar maxY = new DoubleVar(0);
//
//    DoubleVar visCX = new DoubleVar(0);
//    DoubleVar visCY = new DoubleVar(0);
//    DoubleVar visWidth = new DoubleVar(0);
//    DoubleVar visHeight = new DoubleVar(0);
//
//    private Map<Integer, Map<Integer,Rect>> cell = new FastMap();
//
//    protected Rect content;
//
//    private Slider xSlider, ySlider;
//
//
//    private DoubleVar updatePeriod = new DoubleVar(0.02);
//
//
//    protected boolean needsRefresh = false;
//
//
//    private IntegerVar numCells = new IntegerVar(0);
//
//
//    private DoubleVar cellAspectMin = new DoubleVar(1);
//    private DoubleVar cellAspectMax = new DoubleVar(1);
//
//
//    private DoubleVar maxWidth = new DoubleVar(0);
//    private DoubleVar maxHeight = new DoubleVar(0);
//
//
//    private Slider widthSlider;
//
//
//    private Slider heightSlider;
//
//
//    protected DoubleVar sliderCX = new DoubleVar(0);
//    protected DoubleVar sliderCY = new DoubleVar(0);
//
//    private DoubleVar sliderMaxCX = new DoubleVar(0);
//    private DoubleVar sliderMaxCY = new DoubleVar(0);
//    private DoubleVar sliderMinCX = new DoubleVar(0);
//    private DoubleVar sliderMinCY = new DoubleVar(0);
//
//    private DoubleVar sliderVisWidth = new DoubleVar(0);
//    private DoubleVar sliderVisHeight = new DoubleVar(0);
//    private DoubleVar sliderMaxWidth = new DoubleVar(0);
//    private DoubleVar sliderMaxHeight = new DoubleVar(0);
//
//
//    private DoubleVar autoAspectScale = new DoubleVar(-1.0);
//
//
//    private DoubleVar sliderMaxScale = new DoubleVar(5.0);
//
//
//    private Slider scaleSlider;
//
//    public MatrixRect() {
//        super();
//    }
//
//    @Override public void start() {
//
//        content = add(new Rect());
//        content.tangible(false);
//
//        xSlider = add(new Slider(sliderCX, sliderMinCX, sliderMaxCX, new DoubleVar(0.1), SliderType.Horizontal));
//        ySlider = add(new Slider(sliderCY, sliderMinCY, sliderMaxCY, new DoubleVar(0.1), SliderType.Vertical));
//        xSlider.span(-0.4, -0.45 , 0.4, -0.5);
//        ySlider.span(0.45, -0.4 , 0.5, 0.4);
//
//
//        scaleSlider = add(new Slider(sliderVisHeight, new DoubleVar(1), sliderMaxScale, new DoubleVar(0.2), SliderType.Vertical));
//        widthSlider = add(new Slider(sliderVisWidth, new DoubleVar(1), sliderMaxWidth, new DoubleVar(0.2), SliderType.Horizontal));
//        heightSlider = add(new Slider(sliderVisHeight, new DoubleVar(1), sliderMaxHeight, new DoubleVar(0.2), SliderType.Vertical));
//
//        add(new IfDoubleChanges(getVisCX(), getVisCY(), getVisWidth(), getVisHeight()) {
//            @Override public void afterDoubleChanges(DoubleVar doubleVar, Double previous, Double next) {
//                sliderCX.set(getVisCX().d());
//                sliderCY.set(getVisCY().d());
//                sliderVisWidth.set(getVisWidth().d());
//                sliderVisHeight.set(getVisHeight().d());
//            }
//        });
//
//        add(new IfDoubleChanges(sliderCX, sliderCY, sliderVisWidth, sliderVisHeight) {
//            @Override public void afterDoubleChanges(DoubleVar doubleVar, Double previous, Double next) {
//                needsRefresh = true;
//            }
//        });
//        add(new IfVector2Changes(getAbsoluteSize()) {
//            @Override public void afterVectorChanged(Vector2 v, double dx, double dy) {
//                double autoAspectScale = getAutoAspectScale().d();
//                if (autoAspectScale!=-1) {
//                    needsRefresh = true;
//                }
//            }
//        });
//
//
//        add(new Repeat() {
//            @Override public double repeat(double t, double dt) {
//                updateMatrix();
//                return getUpdatePeriod().get();
//            }
//        });
//
//        needsRefresh = true;
//        updateMatrix();
//    }
//
//    protected void updateMatrix() {
//        if (needsRefresh) {
//            layout();
//        }
//    }
//
//    private DoubleVar getMaxWidth() {
//        return maxWidth;
//    }
//    private DoubleVar getMaxHeight() {
//        return maxHeight ;
//    }
//
//
//    protected DoubleVar getUpdatePeriod() {
//        return updatePeriod ;
//    }
//
//    @Override public void stop() {	}
//
//    public void removeAll() {
//        synchronized (cell) {
//
//            getNumCells().set(0);
//            cell.clear();
//
//            if (content!=null)
//                content.clear();
//
//            needsRefresh = true;
//        }
//
//    }
//
//    public void put(int x, int y, Rect r) {
//        synchronized (cell) {
//
//            Map<Integer, Rect> row = cell.get(y);
//            if (row == null) {
//                row =new FastMap<Integer,Rect>();
//                cell.put(y, row);
//            }
//
//            if (row.get(x)!=null) {
//                Rect removed = row.remove(x);
//                if (removed == r)
//                    return;
//                content.remove(removed);
//            }
//            else {
//                getNumCells().add(1);
//            }
//
//            row.put(x, r);
//
//            if (getNumCells().i() == 1) {
//                minX.set(x);
//                maxX.set(x);
//                minY.set(y);
//                maxY.set(y);
//            }
//            else {
//                if (x < minX.i()) minX.set(x);
//                if (x > maxX.i()) maxX.set(x);
//                if (y < minY.i()) minY.set(y);
//                if (y > maxY.i()) maxY.set(y);
//            }
//
//
//            content.add(r);
//
//            needsRefresh = true;
//        }
//    }
//
//    public Rect get(int x, int y) {
//        try {
//            return cell.get(y).get(x);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
//
//
//    protected void layout() {
//        synchronized (cell) {
//
//            maxWidth.set(getMaxX().i() - getMinX().i());
//            maxHeight.set(getMaxY().i() - getMinY().i());
//
//
//            double d = Math.ceil(sliderVisHeight.d());
//            d = Math.min( d, getMaxY().d() - getMinY().d() );
//            d = Math.max(1, d);
//            visHeight.set(d);
//
//            d = Math.ceil(sliderVisWidth.d());
//            d = Math.min( d, getMaxX().d() - getMinX().d() );
//            d = Math.max(1, d);
//            visWidth.set(d);
//
//            double autoAspectScale = getAutoAspectScale().d();
//            if (autoAspectScale!=-1) {
//                getAutoAspectScale().set( scaleSlider.getValue().d() );
//                double sx = getAbsoluteSize().x();
//                double sy = getAbsoluteSize().y();
//                double sa = sy / sx;
//
//
//                visWidth.set( (1.0 / sa) * autoAspectScale);
//                visHeight.set(  sa * autoAspectScale );
//
//                scaleSlider.span(-0.5, 0.25, -0.45, -0.25);
//                scaleSlider.visible(true);
//
//                widthSlider.visible(false);
//                heightSlider.visible(false);
//            }
//            else {
//                widthSlider.span(-0.25, 0.5, 0.25, 0.45);
//                heightSlider.span(-0.5, 0.25, -0.45, -0.25);
//
//                widthSlider.visible(true);
//                heightSlider.visible(true);
//
//                scaleSlider.visible(false);
//            }
//
//
//            double v = sliderCX.d();
//            v = Math.max(getMinX().d(), v);
//            v = Math.min(getMaxX().d(), v);
//            visCX.set(v);
//
//            v = sliderCY.d();
//            v = Math.max(getMinY().d(), v);
//            v = Math.min(getMaxY().d(), v);
//            visCY.set(v);
//
//
//
//            double dx = 1 + maxX.d() - minX.d();
//            double dy = 1 + maxY.d() - minY.d();
//
//
//
//            if (getNumCells().i() == 0) {
//                needsRefresh = false;
//                return;
//            }
//
//            content.scale(0.9, 0.9);
//
//            double height = 0;
//            double width = 0;
//
//            double cellAspect = (getCellAspectMax().d() + getCellAspectMin().d()) / 2.0;
//
//            //invert to match slider's output
//            double vy = getMaxY().d() * ( 1.0 - ( getVisCY().d() / (getMaxY().d())));
//
//            int startY = (int)Math.floor(vy - getVisHeight().d()/2.0);
//            int stopY = (int)Math.ceil(vy + getVisHeight().d()/2.0);
//            int startX = (int)Math.floor(getVisCX().d() - getVisWidth().d()/2.0);
//            int stopX = (int)Math.ceil(getVisCX().d() + getVisWidth().d()/2.0);
//
//            if (startX < getMinX().i()) {
//                startX = getMinX().i();
//                stopX = getMinX().i() + getVisWidth().i();
//            }
//            if (stopX > getMaxX().i()) {
//                stopX = getMaxX().i();
//                startX = getMaxX().i() - getVisWidth().i();
//            }
//            if (startY < getMinY().i()) {
//                startY = getMinY().i();
//                stopY = getMinY().i() + getVisHeight().i();
//            }
//            if (stopY > getMaxY().i()) {
//                stopY = getMaxY().i();
//                startY = getMaxY().i() - getVisHeight().i();
//            }
//
//            startX = Math.max( startX, getMinX().i() );
//            stopX = Math.min( getMaxX().i(), stopX );
//
//            startY = Math.max( startY, getMinY().i() );
//            stopY = Math.min( getMaxY().i(), stopY );
//
//            //calculate width and height to normalize
//            for (int y = startY; y <= stopY; y++) {
//                double yScale = cellAspect;
//
//                Map<Integer, Rect> row = cell.get(y);
//
//                double w = 0;
//                if (row!=null) {
//                    for (int x = startX; x <= stopX; x++) {
//                        double xScale = 1.0 / cellAspect;
//                        w += xScale;
//                    }
//                }
//                if (w > width)
//                    width = w;
//
//                height += yScale;
//            }
//
//            double px;
//            double py = -0.5;
//
//            for (int y = getMinY().i(); y <= getMaxY().i(); y++) {
//                px = -0.5;
//                double yScale = cellAspect/height;
//                Map<Integer, Rect> row = cell.get(y);
//
//                if (row!=null) {
//                    for (int x = getMinX().i(); x <= getMaxX().i(); x++) {
//                        Rect r = row.get(x);
//                        if (r!=null) {
//                            r.visible(false);
//                        }
//                    }
//                }
//            }
//
//            for (int y = stopY; y >= startY; y--) {
//                px = -0.5;
//                double yScale = cellAspect/height;
//                Map<Integer, Rect> row = cell.get(y);
//
//                if (row!=null) {
//                    for (int x = startX; x <= stopX; x++) {
//                        double xScale = 1.0 / cellAspect / width;
//                        Rect r = row.get(x);
//                        if (r!=null) {
//                            r.getPosition().set(px + xScale/2.0, py + yScale/2.0, 0);
//                            r.getSize().set(xScale, yScale);
//                            r.visible(true);
//                        }
//                        px += xScale;
//                    }
//                }
//                py += yScale;
//            }
//
////			sliderCX.set(visCX.d());
////			sliderCY.set(visCY.d());
//
//            sliderMaxWidth.set(getMaxWidth().d());
//            sliderMaxHeight.set(getMaxHeight().d());
//
//            xSlider.getKnobLength().set( Math.min(1.0, getVisWidth().d() / getMaxWidth().d()) );
//            ySlider.getKnobLength().set( Math.min(1.0, getVisHeight().d() / getMaxHeight().d()) );
//
//            sliderMinCX.set(getMinX().d() );
//            sliderMaxCX.set(getMaxX().d() );
//
//            sliderMinCY.set(getMinY().d() );
//            sliderMaxCY.set(getMaxY().d() );
//
//            //System.out.println(getVisWidth().d() + ", " + getVisHeight().d() + " : " + startX + ".." + stopX + "," + startY + ".." + stopY);
//
//
////			System.out.println(visWidth.d() + ": " + sliderMinCX.d() + " < " +sliderCX.d() + " < " + sliderMaxCX.d());
////			System.out.println(visHeight.d() + ": " + sliderMinCY.d() + " < " +sliderCY.d() + " < " + sliderMaxCY.d());
////			System.out.println(sliderMaxWidth.d() + " , " + sliderMaxHeight.d());
//
//            needsRefresh = false;
//        }
//
//    }
//
//
//    /** if =-1, auto aspect is disabled */
//    public DoubleVar getAutoAspectScale() {
//        return autoAspectScale ;
//    }
//
//    public IntegerVar getNumCells() {
//        return numCells ;
//    }
//
//    /** maximum cell X coordinate */
//    public DoubleVar getMaxX() {	return maxX;	}
//
//    /** maximum cell Y coordinate */
//    public DoubleVar getMaxY() {	return maxY;	}
//
//    /** minimum cell X coordinate */
//    public DoubleVar getMinX() {	return minX;	}
//
//    /** minimum cell Y coordinate */
//    public DoubleVar getMinY() {	return minY;	}
//
//    /** center X of visible cells */
//    public DoubleVar getVisCX() {
//        return visCX;
//    }
//
//    /** center Y of visible cells */
//    public DoubleVar getVisCY() {
//        return visCY;
//    }
//
//    /** number of visible cells tall */
//    public DoubleVar getVisHeight() {
//        return visHeight;
//    }
//
//    /** number of visible cells wide */
//    public DoubleVar getVisWidth() {
//        return visWidth;
//    }
//
//    public DoubleVar getCellAspectMin() {
//        return cellAspectMin;
//    }
//    public DoubleVar getCellAspectMax() {
//        return cellAspectMax;
//    }
//
//    //	protected double getXScale(double x) {
//    //	double dx = Math.abs( x - getCx().d() );
//    //	double dw = getWidth().d()/2.0;
//    //	if (dx < dw) {
//    //		return 1.0;
//    //	}
//    //	double sx = 1.0 / (dx - dw + 1.0);
//    //	if (sx < getVisibleThreshold()) {
//    //		return 0.0;
//    //	}
//    //	return sx;
//    //}
//    //
//    //protected double getYScale(double y) {
//    //	double dy = Math.abs( y - getCy().d() );
//    //	double dh = getHeight().d()/2.0;
//    //	if (dy < dh) {
//    //		return 1.0;
//    //	}
//    //	double sy = 1.0 / (dy - dh + 1.0);
//    //	if (sy < getVisibleThreshold()) {
//    //		return 0.0;
//    //	}
//    //	return sy;
//    //}
//    //
//    //public double getVisibleThreshold() {
//    //	return 0.05;
//    //}
//
//    public Rect getContent() {
//        return content;
//    }
//
//}
