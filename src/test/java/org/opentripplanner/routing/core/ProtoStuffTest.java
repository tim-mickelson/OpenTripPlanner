/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import com.google.common.collect.Lists;
import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.diff.DiffPrinter;
import org.opentripplanner.common.diff.Difference;
import org.opentripplanner.common.diff.GenericDiffConfig;
import org.opentripplanner.common.diff.GenericObjectDiffer;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.util.TestUtils;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ProtoStuffTest extends TestCase {

    AStar aStar = new AStar();

    private static Graph graph;

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();
    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder().build();
    private DiffPrinter diffPrinter = new DiffPrinter();


    @Override
    public void setUp() {
        GraphBuilder graphBuilder = new GraphBuilder(new File(""), mock(GraphBuilderParameters.class));

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File("/home/cristoffer/rutebanken/opentripplanner/norway-latest.osm.pbf"));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
        osmModule.edgeFactory = streetEdgeFactory;
        osmModule.skipVisibility = true;
        graphBuilder.addModule(osmModule);
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.OSLO_MINIMAL_GTFS));
        gtfsBundle.linkStopsToParentStations = true;
        gtfsBundle.parentStationTransfers = true;
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        graph = graphBuilder.getGraph();
        graph.index(new DefaultStreetVertexIndexFactory());
    }

    @Test
    public void testProtoStuff() throws IOException, JAXBException, XMLStreamException, SAXException {

        // Seems like I have to use GraphIOUtil instead of ProtostuffIOUtil to avoid stack overflow exception with SIRI

        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");

        long schemaStarted = System.currentTimeMillis();
        Schema<Graph> schema = RuntimeSchema.getSchema(Graph.class);
        System.out.println("Schema created in " + (System.currentTimeMillis() - schemaStarted) + " ms");

        long bufferStarted = System.currentTimeMillis();

        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        byte[] protostuff = GraphIOUtil.toByteArray(graph, schema, buffer);

        buffer.clear();

        System.out.println("Written to protostuff bya in  " + (System.currentTimeMillis() - bufferStarted) + " ms");

        System.out.println("The byte array length is " + protostuff.length);
        long beforeWrite = System.currentTimeMillis();
        IOUtils.copy(new ByteArrayInputStream(protostuff), new FileOutputStream("protostuff.file"));
        System.out.println("Wrote protostuff file to disk in " + (System.currentTimeMillis() - beforeWrite) + " ms");

        long serializeBack = System.currentTimeMillis();

        Graph graphFromProtobuf = schema.newMessage();
        GraphIOUtil.mergeFrom(protostuff, graphFromProtobuf, schema);

        System.out.println("Deserialized from protobuf in  " + (System.currentTimeMillis() - serializeBack) + " ms");


        assertEquals(graph.getEdges().size(), graphFromProtobuf.getEdges().size());



    }

    @Test
    public void testProtoStuffWithEdge() throws IOException, JAXBException, XMLStreamException, SAXException, IllegalAccessException {

        // Seems like I have to use GraphIOUtil instead of ProtostuffIOUtil to avoid stack overflow exception with SIRI

        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");

        long schemaStarted = System.currentTimeMillis();
        Schema<EdgeInfo> schema = RuntimeSchema.getSchema(EdgeInfo.class);
        System.out.println("Schema created in " + (System.currentTimeMillis() - schemaStarted) + " ms");



        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        EdgeInfo edgeInfo = new EdgeInfo();
        edgeInfo.edges = graph.getEdges();
        edgeInfo.graph = graph;
        long bufferStarted = System.currentTimeMillis();

        byte[] protostuff = GraphIOUtil.toByteArray(edgeInfo, schema, buffer);

        buffer.clear();

        System.out.println("Written to protostuff bya in  " + (System.currentTimeMillis() - bufferStarted) + " ms");

        System.out.println("The byte array length is " + protostuff.length);
        long beforeWrite = System.currentTimeMillis();
        IOUtils.copy(new ByteArrayInputStream(protostuff), new FileOutputStream("protostuff.file"));
        System.out.println("Wrote protostuff file to disk in " + (System.currentTimeMillis() - beforeWrite) + " ms");

        long serializeBack = System.currentTimeMillis();

        EdgeInfo edgeInfoFromProtostuff = schema.newMessage();
        GraphIOUtil.mergeFrom(protostuff, edgeInfoFromProtostuff, schema);

        System.out.println("Deserialized from protobuf in  " + (System.currentTimeMillis() - serializeBack) + " ms");


        assertNotNull(edgeInfoFromProtostuff);

        assertTrue(edgeInfoFromProtostuff.edges.size() > 0);

        assertNotNull(edgeInfoFromProtostuff.graph);


        System.out.println(edgeInfoFromProtostuff.edges.size());



        for (Edge e : edgeInfoFromProtostuff.edges) {

            if(e.fromv.incoming == null) {
                e.fromv.incoming = new Edge[0];
            }
            if(e.fromv.outgoing == null) {
                e.fromv.outgoing = new Edge[0];
            }

            if(e.tov.incoming == null) {
                e.tov.incoming = new Edge[0];
            }

            if(e.tov.outgoing  == null) {
                e.tov.outgoing = new Edge[0];
            }



            e.fromv.addOutgoing(e);
            e.tov.addIncoming(e);

            edgeInfoFromProtostuff.graph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
            edgeInfoFromProtostuff.graph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
        }


        edgeInfoFromProtostuff.graph.index(new DefaultStreetVertexIndexFactory());


        System.out.println("Comparing graph object after deserializing it from protostuff");

        List<Difference> differences = genericObjectDiffer.compareObjects(graph, edgeInfoFromProtostuff.graph, genericDiffConfig);
        System.out.println(diffPrinter.diffListToString(differences));

        testKissAndRide(edgeInfoFromProtostuff.graph);

    }

    public void testKissAndRide(Graph graphToUse) {
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("Europe/Oslo"
                , 2017, 10, 15, 7, 0, 0);
        options.from = new GenericLocation(59.9113032, 10.7489964);
        options.to = new GenericLocation(59.90808, 10.607298);
        options.setNumItineraries(1);
        options.setRoutingContext(graphToUse);
        options.kissAndRide = true;
        options.modes = TraverseModeSet.allModes();
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPaths().get(0);

        // Car leg before transit leg
        boolean carLegSeen = false;
        boolean transitLegSeen = false;
        for (int i = 0; i < path.states.size(); i++) {
            TraverseMode mode = path.states.get(i).getBackMode();
            if (mode != null) {
                assertFalse(transitLegSeen && mode.isDriving());
                if (mode.isDriving()) {
                    carLegSeen = true;
                }
                if (mode.isTransit()) {
                    transitLegSeen = true;
                }
            }
        }
        assertTrue(carLegSeen && transitLegSeen);
    }



    public static class EdgeInfo {

        public Collection<Edge> edges;
        public Graph graph;

    }
}
