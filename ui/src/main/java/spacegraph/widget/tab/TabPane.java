package spacegraph.widget.tab;

import spacegraph.Surface;
import spacegraph.layout.Gridding;
import spacegraph.layout.MutableContainer;
import spacegraph.layout.Splitting;
import spacegraph.render.JoglSpace;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.button.ToggleButton;
import spacegraph.widget.sketch.Sketch2DBitmap;
import spacegraph.widget.text.Label;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends Splitting {


    private final ButtonSet tabs;
    private final MutableContainer content;

    private static final float CONTENT_VISIBLE_SPLIT = 0.9f;


    public TabPane(Map<String, Supplier<Surface>> builder) {
        super();

        content = new Gridding();

        tabs = new ButtonSet<>(ButtonSet.Mode.Multi, builder.entrySet().stream().map(x -> {
            final Surface[] created = {null};
            Supplier<Surface> creator = x.getValue();
            String label = x.getKey();
            return new CheckBox(label).on((cb, a) -> {
                synchronized (content) {
                    if (a) {
                        Surface cx;
                        try {
                            cx = creator.get();
                        } catch (Throwable t) {
                            String msg = t.getMessage();
                            if (msg == null)
                                msg = t.toString();
                            cx = new Label(msg);
                        }

                        content.add(created[0] = cx);
                        split(CONTENT_VISIBLE_SPLIT); //hide empty content area
                    } else {
                         if (created[0] !=null) {
                             content.remove(created[0]);
                             created[0] = null;
                         }
                         if (content.isEmpty()) {
                            split(0f); //hide empty content area
                         }
                    }
                }
            });
        }).toArray(ToggleButton[]::new));

        split(0).set(tabs, content);
    }


    public static void main(String[] args) {
        JoglSpace.window(new TabPane(Map.of(
                "a", () -> new Sketch2DBitmap(40, 40),
                "b", () -> new PushButton("x"))), 800, 800);
    }

}
