package org.opentripplanner.serializer;

import io.protostuff.GraphIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ProtostuffGraphDeserializer implements GraphDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(ProtostuffGraphDeserializer.class);

    @Override
    public GraphWrapper deserialize(File file) {
        return null;
    }

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {

        final long started = System.currentTimeMillis();
        LOG.debug("Creating schema");
        // Can be cached
        Schema<GraphWrapper> schema = RuntimeSchema.getSchema(GraphWrapper.class);
        LOG.debug("Deserializing");

        GraphWrapper graphWrapperFromProtostuff = schema.newMessage();
        try {
            GraphIOUtil.mergeFrom(inputStream, graphWrapperFromProtostuff, schema);
            LOG.debug("Returning wrapped graph object after {} ms", System.currentTimeMillis() - started);
            return graphWrapperFromProtostuff;
        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }
    }
}
