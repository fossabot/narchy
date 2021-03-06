package spacegraph.space2d.widget.console;

import com.googlecode.lanterna.TextCharacter;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractConsoleSurface extends Container implements Appendable {
    protected int rows, cols;

    public void resize(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
    }

    abstract public TextCharacter charAt(int col, int row);


    @Override
    public final void forEach(Consumer<Surface> o) {
        //empty
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        //empty
        return true;
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }

    @Override
    public int childrenCount() {
        return 0;
    }
}
