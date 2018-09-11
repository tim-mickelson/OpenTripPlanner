package org.opentripplanner.serializer;

import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GraphDeserializerService {

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);


    private GraphDeserializer getGraphDeserializer() {

        String deserializationMethod = System.getProperty("deserialization-method");

        GraphDeserializer graphDeserializer;
        if ("protostuff".equals(deserializationMethod)) {
            graphDeserializer = new ProtostuffGraphDeserializer();
        } else if ("kryo".equals(deserializationMethod)) {
            graphDeserializer = new KryoGraphDeserializer();
        } else {
            LOG.debug("Defaulting to java graph deserializer");
            graphDeserializer = new JavaGraphDeserializer();
        }
        return graphDeserializer;
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
        GraphDeserializer graphDeserializer = getGraphDeserializer();
        LOG.debug("Loading graph using: {}", graphDeserializer.getClass().getSimpleName());
        GraphWrapper graphWrapper = graphDeserializer.deserialize(is);
        LOG.debug("Deserialized graph using: {}", graphDeserializer.getClass().getSimpleName());

        return graphWrapper;
    }
}
