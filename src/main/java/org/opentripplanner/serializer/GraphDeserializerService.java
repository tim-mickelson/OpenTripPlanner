package org.opentripplanner.serializer;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class GraphDeserializerService {

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);
    public static final String DESERIALIZATION_METHOD_PROP = "deserialization-method";

    /**
     * Load debug data ?
     */
    private boolean debugData = true;

    private final GraphDeserializer graphDeserializer;


    public GraphDeserializerService() {
        this.graphDeserializer = getGraphDeserializer(System.getProperty(DESERIALIZATION_METHOD_PROP));
    }

    public GraphDeserializerService(String deserializationMethod) {
        this.graphDeserializer = getGraphDeserializer(deserializationMethod);
    }

    public GraphWrapper deserialize(File file) {
        try {
            LOG.info("Reading graph from file: " + file.getAbsolutePath() + " ...");
            return deserialize(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new GraphSerializationException("Cannot read file " + file.getName(), e);
        }
    }

    public GraphWrapper deserialize(InputStream is) {
        GraphWrapper graphWrapper = graphDeserializer.deserialize(is);
        LOG.debug("Deserialized graph using: {}", graphDeserializer.getClass().getSimpleName());

        return graphWrapper;
    }


    public Graph load(File file, Graph.LoadLevel level) {
        GraphWrapper graphWrapper = deserialize(file);
        return load(graphWrapper, level, new DefaultStreetVertexIndexFactory());
    }

    public Graph load(InputStream inputStream, Graph.LoadLevel level) {
        GraphWrapper graphWrapper = deserialize(inputStream);
        return load(graphWrapper, level, new DefaultStreetVertexIndexFactory());
    }

    public Graph load(InputStream inputStream, Graph.LoadLevel level, StreetVertexIndexFactory streetVertexIndexFactory) {
        GraphWrapper graphWrapper = deserialize(inputStream);
        return load(graphWrapper, level, streetVertexIndexFactory);
    }

    private Graph load(GraphWrapper graphWrapper, Graph.LoadLevel level, StreetVertexIndexFactory indexFactory) {

        // Because some fields are marked as transient
        Graph deserializedGraph = graphWrapper.graph;
        List<Edge> edges = graphWrapper.edges;
        List<GraphBuilderAnnotation> graphBuilderAnnotations = graphWrapper.graphBuilderAnnotations;

        LOG.debug("Basic graph info read.");
        if (deserializedGraph.graphVersionMismatch())
            throw new RuntimeException("Graph version mismatch detected.");
        if (level == Graph.LoadLevel.BASIC)
            return deserializedGraph;
        // vertex edge lists are transient to avoid excessive recursion depth
        // vertex list is transient because it can be reconstructed from edges

        deserializedGraph.vertices = new HashMap<>();

        for (Edge e : edges) {
            deserializedGraph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
            deserializedGraph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
        }

        LOG.info("Main graph read. |V|={} |E|={}", deserializedGraph.countVertices(), deserializedGraph.countEdges());
        deserializedGraph.index(indexFactory);

        if (level == Graph.LoadLevel.FULL) {
            return deserializedGraph;
        }

        if (debugData) {
            deserializedGraph.getGraphBuilderAnnotations().addAll(graphBuilderAnnotations);
            LOG.debug("Debug info read.");
        } else {
            LOG.warn("Graph file does not contain debug data.");
        }
        return deserializedGraph;


    }

    private static GraphDeserializer getGraphDeserializer(String deserializationMethod) {

        GraphDeserializer graphDeserializer;
        if ("protostuff".equals(deserializationMethod)) {
            graphDeserializer = new ProtostuffGraphDeserializer();
        } else if ("kryo".equals(deserializationMethod)) {
            graphDeserializer = new KryoGraphDeserializer();
        } else {
            LOG.debug("Defaulting to java graph deserializer");
            graphDeserializer = new JavaGraphDeserializer();
        }
        LOG.debug("Using the following deserializer for graph loading: {}", graphDeserializer.getClass().getSimpleName());
        return graphDeserializer;
    }
}
