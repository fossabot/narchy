package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.Label;
import spacegraph.video.Draw;

import java.util.function.Function;

/** decoration inference for dynamic representational surfaces of arbitrary objects */
abstract public class IconBuilder<X> implements Function<X,Surface> {

    public static final IconBuilder<Object> simpleBuilder = new IconBuilder<>() {

        @Override
        public Surface apply(Object o) {
            return new Label(o.toString()) {

                final int classHash = o.getClass().hashCode();

                @Override
                protected void paintBelow(GL2 gl) {
                    super.paintBelow(gl);
                    Draw.colorHash(gl, classHash);
                    Draw.rect(gl, bounds);
                }
            };
        }
    };

}
