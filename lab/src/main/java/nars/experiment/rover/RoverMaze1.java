package nars.experiment.rover;

import nars.NARS;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.constraint.HingeConstraint;
import spacegraph.space3d.phys.shape.BoxShape;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.CylinderShape;

import static spacegraph.util.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {


    public static void main(String[] args) {
        Rover r = new Rover(new NARS().get()) {

            @Override protected void create(Dynamics3D world) {

                SimpleSpatial torso;
                add(torso = new SimpleSpatial("torso") {
                    @Override
                    protected CollisionShape newShape() {
                        //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                        //return new CylinderShape(v(0.5f, 1, 0.5f));
                        //return new CylinderShape(v(1f, 0.1f, 1f));
                        return new BoxShape(v(1.6f, 0.1f, 1f));
                    }

                    @Override
                    public float mass() {
                        return 40f;
                    }
                });


                SimpleSpatial neck;
                add(neck = new SimpleSpatial("neck") {
                    @Override
                    protected CollisionShape newShape() {
                        //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
                        return new CylinderShape(v(0.25f, 0.75f, 0.25f));
                    }

                    @Override
                    protected Body3D create(Dynamics3D world) {
                        torso.body.clearForces();

                        Body3D n = super.create(world);
                        HingeConstraint p = new HingeConstraint(torso.body, body, v(0, 0.2f, 0), v(0, -1f, 0), v(1, 0, 0), v(1, 0, 0));
                        p.setLimit(-1.0f, 1.0f);
                        add(p);
                        return n;
                    }

                    @Override
                    public float mass() {
                        return 10f;
                    }
                });
                neck.shapeColor[0] = 1f;
                neck.shapeColor[1] = 0.1f;
                neck.shapeColor[2] = 0.5f;
                neck.shapeColor[3] = 1f;

                RetinaGrid rg = new RetinaGrid("cam1", v(), v(0, 0, 1), v(0.1f, 0, 0), v(0, 0.1f, 0), 6, 6, 4f) {
                    @Override
                    protected Body3D create(Dynamics3D world) {

                        Body3D l = super.create(world);

                        //move(0,-1,0);
                        //body.clearForces();

                        l.clearForces();
                        HingeConstraint p = new HingeConstraint(neck.body, body, v(0, 0.6f, 0), v(0, -0.6f, 0), v(0, 1, 0), v(0, 1, 0));
                        p.setLimit(-0.75f, 0.75f);


//                        Point2PointConstraint p = new Point2PointConstraint(body, torso.body, v(2, 0, 0), v(-2, 0, 0));
//                        p.impulseClamp = 0.01f;
//                        //p.damping = 0.5f;
//                        p.tau = 0.01f;
                        add(p);
                        return l;
                    }


                };

                add(rg);
            }
        };



//        new SpaceGraph<>(
//                new Maze("x", 20, 20),
//                r
//        );//.setGravity(v(0, 0, -5)).show(1000, 1000);


    }

}
