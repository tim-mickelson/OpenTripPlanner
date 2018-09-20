package org.opentripplanner.serializer;

import io.protostuff.*;
import io.protostuff.runtime.RuntimeSchema;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ProtostuffGraphSerializer implements GraphSerializer {

    private static final int SIZE_LIMIT = Integer.MAX_VALUE;
    private static final Logger LOG = LoggerFactory.getLogger(ProtostuffGraphSerializer.class);

    private final Schema<GraphWrapper> schema;
    private final int linkedBufferSize;

    public ProtostuffGraphSerializer() {
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");
        System.setProperty("protostuff.runtime.allow_null_array_element", "true");

        LOG.debug("Creating schema for protostuff");
        schema = RuntimeSchema.getSchema(GraphWrapper.class);
        linkedBufferSize = 1024 * 8;
    }

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {

        try {

            final long started = System.currentTimeMillis();

            GraphWrapper deserializedGraph = schema.newMessage();

            CodedInput input = new CodedInput(inputStream, true);
            input.setSizeLimit(Integer.MAX_VALUE);
            GraphCodedInput graphInput = new GraphCodedInput(input);
            schema.mergeFrom(graphInput, deserializedGraph);
            input.checkLastTagWas(0);

            LOG.debug("Returning wrapped graph object after {} ms", System.currentTimeMillis() - started);
            return deserializedGraph;
        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }
    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) throws GraphSerializationException {
        long writeToFileStarted = System.currentTimeMillis();

        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(linkedBufferSize);

        try {

            Graph graph = graphWrapper.graph;

            LOG.debug("Consolidating edges...");
            // this is not space efficient
            List<Edge> edges = new ArrayList<Edge>(graph.countEdges());
            for (Vertex v : graph.getVertices()) {
                // there are assumed to be no edges in an incoming list that are not
                // in an outgoing list
                edges.addAll(v.getOutgoing());
                if (v.getDegreeOut() + v.getDegreeIn() == 0)
                    LOG.debug("vertex {} has no edges, it will not survive serialization.", v);
            }
            LOG.debug("Assigning vertex/edge ID numbers...");
            graph.rebuildVertexAndEdgeIndices();
            graphWrapper.edges = edges;


            int written = GraphIOUtil.writeTo(outputStream, graphWrapper, schema, linkedBuffer);
            long millisSpent = System.currentTimeMillis() - writeToFileStarted;
            LOG.info("Written {} bytes to protostuff in {} ms", written, millisSpent);

        } catch (IOException e) {
            throw new GraphSerializationException("Cannot serialize graph", e);
        }

    }
}
