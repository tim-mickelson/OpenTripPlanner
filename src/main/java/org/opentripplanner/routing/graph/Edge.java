package org.opentripplanner.routing.graph;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;

import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;

/**
 * This is the standard implementation of an edge with fixed from and to Vertex instances;
 * all standard OTP edges are subclasses of this.
 */
public abstract class Edge implements Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    protected Vertex fromv;

    protected Vertex tov;

    protected Edge(Vertex v1, Vertex v2) {
        if (v1 == null || v2 == null) {
            String err = String.format("%s constructed with null vertex : %s %s", this.getClass(),
                    v1, v2);
            throw new IllegalStateException(err);
        }
        this.fromv = v1;
        this.tov = v2;
        fromv.addOutgoing(this);
        tov.addIncoming(this);
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }
    
    /**
     * Returns true if this edge is partial - overriden by subclasses.
     */
    public boolean isPartial() {
        return false;
    }
    
    /**
     * Checks equivalency to another edge. Default implementation is trivial equality, but subclasses may want to do something more tricky.
     */
    public boolean isEquivalentTo(Edge e) {
        return this == e;
    }
    
    /**
     * Returns true if this edge is the reverse of another.
     */
    public boolean isReverseOf(Edge e) {
        return (this.getFromVertex() == e.getToVertex() &&
                this.getToVertex() == e.getFromVertex());
    }
    
    /**
     * Get a direction on paths where it matters, or null
     * 
     * @return
     */
    public String getDirection() {
        return null;
    }

    /**
     * This should only be called inside State; other methods should call
     * org.opentripplanner.routing.core.State.getBackTrip()
     * 
     * @author mattwigway
     */
    public Trip getTrip() {
        return null;
    }

    // Notes are now handled by State

    @Override
    public int hashCode() {
        return fromv.hashCode() * 31 + tov.hashCode();
    }

    /**
     * Edges are not roundabouts by default.
     */
    public boolean isRoundabout() {
        return false;
    }

    /**
     * Traverse this edge.
     * 
     * @param s0 The State coming into the edge.
     * @return The State upon exiting the edge.
     */
    public abstract State traverse(State s0);

    public State optimisticTraverse(State s0) {
        return this.traverse(s0);
    }

    /**
     * Returns a lower bound on edge weight given the routing options.
     * 
     * @param options
     * @return edge weight as a double.
     */
    public double weightLowerBound(RoutingRequest options) {
        // Edge weights are non-negative. Zero is an admissible default lower
        // bound.
        return 0;
    }

    /**
     * Returns a lower bound on traversal time given the routing options.
     * 
     * @param options
     * @return edge weight as a double.
     */
    public double timeLowerBound(RoutingRequest options) {
        // No edge should take less than zero time to traverse.
        return 0;
    }


    /**
     * Gets english localized name
     * @return english localized name
     */
    public abstract String getName();

    /**
     * Gets wanted localization
     * @param locale wanted locale
     * @return Localized in specified locale name
     */
    public abstract String getName(Locale locale);

    // TODO Add comments about what a "bogus name" is.
    public boolean hasBogusName() {
        return false;
    }

    public String toString() {
        return String.format("%s (%s -> %s)", getClass().getName(), fromv, tov);
    }

    // The next few functions used to live in EdgeNarrative, which has now been
    // removed
    // @author mattwigway

    public LineString getGeometry() {
        return null;
    }

    /**
     * Returns the azimuth of this edge from head to tail.
     * 
     * @return
     */
    public double getAzimuth() {
        // TODO(flamholz): cache?
        return getFromVertex().azimuthTo(getToVertex());
    }

    public double getDistanceMeters() {
        return 0;
    }

    /**
     * This gets the effective length for walking, taking slopes into account. This can be divided
     * by the speed on a flat surface to get the duration.
     */
    public double getEffectiveWalkDistance() {
        return 0;
    }

    /* SERIALIZATION */

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // edge lists are transient, reconstruct them
        fromv.addOutgoing(this);
        tov.addIncoming(this);
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        if (fromv == null) {
            System.out.printf("fromv null %s \n", this);
        }
        if (tov == null) {
            System.out.printf("tov null %s \n", this);
        }
        out.defaultWriteObject();
    }

    /* GRAPH COHERENCY AND TYPE CHECKING */

    @SuppressWarnings("unchecked")
    private static final ValidVertexTypes VALID_VERTEX_TYPES = new ValidVertexTypes(Vertex.class,
            Vertex.class);

    @XmlTransient
    public ValidVertexTypes getValidVertexTypes() {
        return VALID_VERTEX_TYPES;
    }

    /*
     * This may not be necessary if edge constructor types are strictly specified
     */
    public final boolean vertexTypesValid() {
        return getValidVertexTypes().isValid(fromv, tov);
    }

    public static final class ValidVertexTypes {
        private final Class<? extends Vertex>[] classes;

        // varargs constructor:
        // a loophole in the law against arrays/collections of parameterized
        // generics
        public ValidVertexTypes(Class<? extends Vertex>... classes) {
            if (classes.length % 2 != 0) {
                throw new IllegalStateException("from/to/from/to...");
            } else {
                this.classes = classes;
            }
        }

        public boolean isValid(Vertex from, Vertex to) {
            for (int i = 0; i < classes.length; i += 2) {
                if (classes[i].isInstance(from) && classes[i + 1].isInstance(to))
                    return true;
            }
            return false;
        }
    }
    
}
