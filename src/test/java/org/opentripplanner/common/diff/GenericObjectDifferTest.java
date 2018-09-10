package org.opentripplanner.common.diff;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.StaticStreetNotesSource;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitVertex;

import java.util.List;

public class GenericObjectDifferTest {

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();

    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
            .ignoreFields(Sets.newHashSet("graphBuilderAnnotations", "streetNotesService"))
            .identifiers(Sets.newHashSet("id", "index"))
            .useEqualsBuilder(Sets.newHashSet(TurnRestriction.class, StaticStreetNotesSource.class))
            .build();

    private DiffPrinter diffPrinter = new DiffPrinter();

    @Test
    public void testDiff() throws IllegalAccessException {

        Graph graph = new Graph();
        Graph graph2 = new Graph();

        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graph2, genericDiffConfig);

        System.out.println(diffPrinter.diffListToString(differences));
    }

    /**
     * Try to reproduce diff issue with vertex by id
     * @throws IllegalAccessException
     */
    @Test
    public void vertexByIdComparison() throws IllegalAccessException {


        /**
         * Graph.vertexById{1643706}: <osm:node:2795185141 lat,lng=63.297611800000006,10.2846021> => null
         Graph.vertexById{1643709}: <osm:node:2795185131 lat,lng=63.2967906,10.2838639> => null
         Graph.vertexById{1643708}: <osm:node:2795185132 lat,lng=63.296821400000006,10.283994900000001> => null
         Graph.vertexById{1643711}: <osm:node:839078399 lat,lng=60.6378958,5.956925200000001> => null
         Graph.vertexById{1643697}: <osm:node:4271444310 lat,lng=59.6856238,10.669030900000001> => null
         Graph.vertexById{1643696}: <osm:node:4271444309 lat,lng=59.68557920000001,10.668947800000002> => null
         Graph.vertexById{1643699}: <osm:node:3854930111 lat,lng=59.320310500000005,5.2893832000000005> => null
         Graph.vertexById{1643701}: <osm:node:3854930110 lat,lng=59.319918900000005,5.2901050000000005> => null
         Graph.vertexById{1643700}: <osm:node:3854930107 lat,lng=59.319870200000004,5.2895206> => null
         Graph.vertexById{1643702}: <osm:node:3854930112 lat,lng=59.3203523,5.2899677> => null
         Graph.vertexById{1643689}: <osm:node:4999806840 lat,lng=63.4386549,10.624022100000001> => null
         Graph.vertexById{1643688}: <osm:node:1149915588 lat,lng=63.438706700000004,10.624043100000002> => null
         Graph.vertexById{1643691}: <osm:node:1149915565 lat,lng=63.438519500000005,10.623294900000001> => null
         Graph.vertexById{1643690}: <osm:node:1149915551 lat,lng=63.438619300000006,10.6238408> => null
         */


        Graph graph = new Graph();
        Vertex vertex = new OsmVertex(graph, "1149915551", 63.438619300000006,10.6238408, 10L);
        graph.addVertex(vertex);
        graph.rebuildVertexAndEdgeIndices();


        Graph graph2 = new Graph();

        Vertex vertex2 = new OsmVertex(graph, "1149915551", 63.438619300000006,10.6238408, 10L);
        graph2.addVertex(vertex2);
        graph2.rebuildVertexAndEdgeIndices();

        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graph2, genericDiffConfig);

        System.out.println(diffPrinter.diffListToString(differences));
    }

}