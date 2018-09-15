package org.opentripplanner.serializer;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JavaGraphDeserializer implements GraphDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(JavaGraphDeserializer.class);

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        try {
            ObjectInputStream in = new ObjectInputStream(inputStream);
            GraphWrapper graphWrapper = new GraphWrapper();
            graphWrapper.graph = (Graph) in.readObject();
            LOG.debug("Loading edges...");
            graphWrapper.edges = (ArrayList<Edge>) in.readObject();
            LOG.debug("Loading graph builder annotations (if any)");
            graphWrapper.graphBuilderAnnotations = (List<GraphBuilderAnnotation>) in.readObject();

            return graphWrapper;

        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize incoming date", e);
        } catch (ClassNotFoundException ex) {
            LOG.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
            throw new GraphSerializationException("Stored Graph version error", ex);
        }
    }
}
