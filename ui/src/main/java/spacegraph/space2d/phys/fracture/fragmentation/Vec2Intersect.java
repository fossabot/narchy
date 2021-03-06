package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.fracture.Fragment;
import spacegraph.util.math.Tuple2f;

/**
 * Bod prieniku 2 polygonov (primarneho a konvexneho z voronoi diagramu)
 *
 * @author Marek Benovic
 */
public class Vec2Intersect extends Vec2 {
    /**
     * Fragmenty, ktore rozdeluje hrana, na ktorej sa nachadza prienikovy bod
     */
    public Fragment p1, p2;

    /**
     * Indexy v ramci polygonov p1 a p2 na ktorych sa nachadza bod prieniku
     */
    public int i1, i2;

    /**
     * Index, na ktorom sa nachadza dany bod v ramci primarneho polygonu (ten je len jeden, nepotrebuje referenciu)
     */
    public int index;

    /**
     * (v2.x - v1.x) * k + v1 = Vec2Intersect, kde v1 a v2 su 1. a 2. vrchol jednej hrany
     */
    public final double k;

    /**
     * Pomocna premenna pre vypocet
     */
    public boolean visited;

    /**
     * Suradnice prienikoveho bodu
     */
    public final Tuple2f vec2;

    /**
     * Inicializuje bod prieniku
     *
     * @param a Bod prieniku
     * @param k Pozicia na hrane primaneho polygonu (podla k prebehne triedenie)
     */
    public Vec2Intersect(Tuple2f a, double k) {
        this.x = a.x;
        this.y = a.y;
        this.k = k;
        this.vec2 = a;
    }
}
