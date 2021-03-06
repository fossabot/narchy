package spacegraph.space2d.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.widget.text.Label;
import spacegraph.video.ImageTexture;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {




    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        super();
        content(new EmptySurface());
    }

    public PushButton(String s) {
        this(new Label(s));
    }

    public PushButton(Surface content) {
        content(content);
    }

    public PushButton(Consumer<PushButton> onClick) {
        this();
        click(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        this(s, (p) -> onClick.run());
    }

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        click(onClick);
    }

    public PushButton click(@Nullable Runnable onClick) {
        return click((cb)->onClick.run());
    }

    public PushButton click(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
        return this;
    }

    public PushButton icon(String s) {
        content(new ImageTexture(s).view());
        return this;
    }
    public PushButton label(String s) {
        content(new Label(s));
        return this;
    }

    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
