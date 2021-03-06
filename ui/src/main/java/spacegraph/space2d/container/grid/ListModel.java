package spacegraph.space2d.container.grid;

import jcog.TODO;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract public class ListModel<X> implements GridModel<X> {

    /** orientation, dynamically changeable. true=vertical, false=horizontal. default=vertical */
    public boolean vertical = true;

    private ScrollGrid<X> surface;

    public static <X> ListModel<X> of(X... items) {
        return of(List.of(items));
    }

    public static <X> ListModel<X> of(List<X> items) {
        return new ListModel<>() {

            @Override
            public X get(int index) {
                try {
                    return items.get(index);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("warning: " + e.getMessage());
                    return null;
                }
            }

            @Override
            public int size() {
                return items.size();
            }
        };
    }

    @Override
    public synchronized void start(ScrollGrid<X> x) {
        if (surface!=null)
            throw new TODO("support multiple observers");
        this.surface = x;
    }

    @Override
    public void stop(ScrollGrid<X> x) {
        this.surface = null;
    }

    public void onChange() {
        surface.refresh();
    }

    public void setOrientation(boolean vertical) {
        this.vertical = vertical;
    }

    abstract public X get(int index);
    abstract public int size();

    /** thickenss of the table, one by default */
    protected int depth() {
        return 1;
    }

    @Override
    public final int cellsX() {
        return vertical ? depth() : size();
    }

    @Override
    public final int cellsY() {
        return vertical ? size() : depth();
    }

    @Nullable
    @Override
    public final X get(int x, int y) {
        if ((vertical ? x : y) != 0)
            return null;
        return get(vertical ? y : x);
    }

    //    public static class ListTableModel<X,Y> extends ListModel<Y> {
//
//        public ListTableModel(List<X> items, extractor...)
//        @Override
//        public X get(int index) {
//            return null;
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//    }
}
