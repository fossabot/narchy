/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.jbox2d.dynamics.contacts;


import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.ContactID;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.ManifoldPoint;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.pooling.IWorldPool;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

/**
 * The class manages contact between two shapes. A contact exists for each overlapping AABB in the
 * broad-phase (except if filtered). Therefore a contact object may exist that has no contact
 * points.
 *
 * @author daniel
 */
public abstract class Contact {

    // Flags stored in m_flags
    // Used when crawling contact graph when forming islands.
    public static final int ISLAND_FLAG = 0x0001;
    // Set when the shapes are touching.
    public static final int TOUCHING_FLAG = 0x0002;
    // This contact can be disabled (by user)
    public static final int ENABLED_FLAG = 0x0004;
    // This contact needs filtering because a fixture filter was changed.
    public static final int FILTER_FLAG = 0x0008;
    // This bullet contact had a TOI event
    public static final int BULLET_HIT_FLAG = 0x0010;

    public static final int TOI_FLAG = 0x0020;

    public int m_flags;

    // World pool and list pointers.
    public Contact m_prev;
    public Contact m_next;

    // Nodes for connecting bodies.
    public ContactEdge m_nodeA = null;
    public ContactEdge m_nodeB = null;

    public Fixture aFixture;
    public Fixture bFixture;

    public int aIndex;
    public int bIndex;

    public final Manifold m_manifold;

    public float m_toiCount;
    public float m_toi;

    public float m_friction;
    public float m_restitution;

    public float m_tangentSpeed;

    public float m_angularVelocity_bodyA;
    public final Tuple2f m_linearVelocity_bodyA = new v2();

    public float m_angularVelocity_bodyB;
    public final Tuple2f m_linearVelocity_bodyB = new v2();

    protected final IWorldPool pool;

    protected Contact(IWorldPool argPool) {
        aFixture = null;
        bFixture = null;
        m_nodeA = new ContactEdge();
        m_nodeB = new ContactEdge();
        m_manifold = new Manifold();
        pool = argPool;
    }

    /**
     * initialization for pooling
     */
    public void init(Fixture fA, int indexA, Fixture fB, int indexB) {
        m_flags = ENABLED_FLAG;

        aFixture = fA;
        bFixture = fB;

        aIndex = indexA;
        bIndex = indexB;

        m_manifold.pointCount = 0;

        m_prev = null;
        m_next = null;

        m_nodeA.contact = null;
        m_nodeA.prev = null;
        m_nodeA.next = null;
        m_nodeA.other = null;

        m_nodeB.contact = null;
        m_nodeB.prev = null;
        m_nodeB.next = null;
        m_nodeB.other = null;

        m_toiCount = 0;
        m_friction = Contact.mixFriction(fA.friction, fB.friction);
        m_restitution = Contact.mixRestitution(fA.restitution, fB.restitution);

        m_tangentSpeed = 0;
    }

    /**
     * Get the contact manifold. Do not set the point count to zero. Instead call Disable.
     */
    public Manifold getManifold() {
        return m_manifold;
    }

    /**
     * Get the world manifold.
     */
    public void getWorldManifold(WorldManifold worldManifold) {
        final Body2D bodyA = aFixture.getBody();
        final Body2D bodyB = bFixture.getBody();
        final Shape shapeA = aFixture.shape();
        final Shape shapeB = bFixture.shape();

        worldManifold.initialize(m_manifold, bodyA.getXform(), shapeA.m_radius,
                bodyB.getXform(), shapeB.m_radius);
    }

    /**
     * Is this contact touching
     *
     * @return
     */
    public boolean isTouching() {
        return (m_flags & TOUCHING_FLAG) == TOUCHING_FLAG;
    }

    /**
     * Enable/disable this contact. This can be used inside the pre-solve contact listener. The
     * contact is only disabled for the current time step (or sub-step in continuous collisions).
     *
     * @param flag
     */
    public void setEnabled(boolean flag) {
        if (flag) {
            m_flags |= ENABLED_FLAG;
        } else {
            m_flags &= ~ENABLED_FLAG;
        }
    }

    /**
     * Has this contact been disabled?
     *
     * @return
     */
    public boolean isEnabled() {
        return (m_flags & ENABLED_FLAG) == ENABLED_FLAG;
    }

    /**
     * Get the next contact in the world's contact list.
     *
     * @return
     */
    public Contact getNext() {
        return m_next;
    }

    public void setFriction(float friction) {
        m_friction = friction;
    }

    public float getFriction() {
        return m_friction;
    }

    public void resetFriction() {
        m_friction = Contact.mixFriction(aFixture.friction, bFixture.friction);
    }

    public void setRestitution(float restitution) {
        m_restitution = restitution;
    }

    public float getRestitution() {
        return m_restitution;
    }

    public void resetRestitution() {
        m_restitution = Contact.mixRestitution(aFixture.restitution, bFixture.restitution);
    }

    public void setTangentSpeed(float speed) {
        m_tangentSpeed = speed;
    }

    public float getTangentSpeed() {
        return m_tangentSpeed;
    }

    public abstract void evaluate(Manifold manifold, Transform xfA, Transform xfB);

    /**
     * Flag this contact for filtering. Filtering will occur the next time step.
     */
    public void flagForFiltering() {
        m_flags |= FILTER_FLAG;
    }

    // djm pooling
    private final Manifold oldManifold = new Manifold();

    public void update(ContactListener listener) {

        oldManifold.set(m_manifold);

        // Re-enable this contact.
        m_flags |= ENABLED_FLAG;

        boolean touching = false;
        boolean wasTouching = (m_flags & TOUCHING_FLAG) == TOUCHING_FLAG;

        boolean sensorA = aFixture.isSensor();
        boolean sensorB = bFixture.isSensor();
        boolean sensor = sensorA || sensorB;

        Body2D bodyA = aFixture.getBody();
        Body2D bodyB = bFixture.getBody();
        Transform xfA = bodyA.getXform();
        Transform xfB = bodyB.getXform();
        // log.debug("TransformA: "+xfA);
        // log.debug("TransformB: "+xfB);

        if (sensor) {
            Shape shapeA = aFixture.shape();
            Shape shapeB = bFixture.shape();
            touching = pool.getCollision().testOverlap(shapeA, aIndex, shapeB, bIndex, xfA, xfB);

            // Sensors don't generate manifolds.
            m_manifold.pointCount = 0;
        } else {
            evaluate(m_manifold, xfA, xfB);
            touching = m_manifold.pointCount > 0;

            // Match old contact ids to new contact ids and copy the
            // stored impulses to warm start the solver.
            for (int i = 0; i < m_manifold.pointCount; ++i) {
                ManifoldPoint mp2 = m_manifold.points[i];
                mp2.normalImpulse = 0.0f;
                mp2.tangentImpulse = 0.0f;
                ContactID id2 = mp2.id;

                for (int j = 0; j < oldManifold.pointCount; ++j) {
                    ManifoldPoint mp1 = oldManifold.points[j];

                    if (mp1.id.isEqual(id2)) {
                        mp2.normalImpulse = mp1.normalImpulse;
                        mp2.tangentImpulse = mp1.tangentImpulse;
                        break;
                    }
                }
            }

            if (touching != wasTouching) {
                bodyA.setAwake(true);
                bodyB.setAwake(true);
            }
        }

        if (touching) {
            m_flags |= TOUCHING_FLAG;
        } else {
            m_flags &= ~TOUCHING_FLAG;
        }

        if (!sensor && touching) {
            m_angularVelocity_bodyA = aFixture.body.velAngular;
            m_linearVelocity_bodyA.set(aFixture.body.vel);

            m_angularVelocity_bodyB = bFixture.body.velAngular;
            m_linearVelocity_bodyB.set(bFixture.body.vel);
        }

        if (listener == null) {
            return;
        }

        if (!wasTouching && touching) {
            listener.beginContact(this);
        }

        if (wasTouching && !touching) {
            listener.endContact(this);
        }

        if (!sensor && touching) {
            listener.preSolve(this, oldManifold);
        }
    }

    /**
     * Friction mixing law. The idea is to allow either fixture to drive the restitution to zero. For
     * example, anything slides on ice.
     *
     * @param friction1
     * @param friction2
     * @return
     */
    public static final float mixFriction(float friction1, float friction2) {
        return (float) Math.sqrt(friction1 * friction2);
    }

    /**
     * Restitution mixing law. The idea is allow for anything to bounce off an inelastic surface. For
     * example, a superball bounces on anything.
     *
     * @param restitution1
     * @param restitution2
     * @return
     */
    public static final float mixRestitution(float restitution1, float restitution2) {
        return restitution1 > restitution2 ? restitution1 : restitution2;
    }
}