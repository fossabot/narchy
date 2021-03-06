package spacegraph.space2d.widget.slider;

import spacegraph.SpaceGraph;

import static spacegraph.space2d.container.grid.Gridding.*;

class SliderModelTest {
    public static void main(String[] args) {

        SpaceGraph.window(
                grid(
                        new XYSlider(), new XYSlider(), new XYSlider(),
                        col(
                                new SliderModel(0.75f),
                                new SliderModel(0.25f).type(SliderModel.KnobHoriz),
                                new SliderModel(0.5f)
                        ), row(
                                new SliderModel(0.5f).type(SliderModel.KnobVert),
                                new SliderModel(0.5f).type(SliderModel.KnobVert)
                        )
                )
                , 800, 800);
    }

    //    public static void main(String[] args) {
//        new GraphSpace<Surface>(
//
//                (Surface vt) -> new SurfaceMount(null, vt),
//
//                new XYPadSurface()
//
//        ).show(800,800);
//    }

}