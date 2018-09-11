package org.opentripplanner.serializer;

import com.google.common.io.ByteStreams;
import io.protostuff.CodedInput;
import io.protostuff.GraphIOUtil;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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

        // Set these elsewhere
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");
        System.setProperty("protostuff.runtime.allow_null_array_element", "true");


        try {

            final long started = System.currentTimeMillis();
            LOG.debug("Creating schema");
            // Can be cached
            Schema<GraphWrapper> schema = RuntimeSchema.getSchema(GraphWrapper.class);

            LOG.debug("Deserializing");

            GraphWrapper graphWrapperFromProtostuff = schema.newMessage();

            // Using inputstream directly caused the following exception:
            // Protocol message was too large.  May be malicious.  Use CodedInput.setSizeLimit() to increase the size limit.
            byte[] protostuff = ByteStreams.toByteArray(inputStream);

            GraphIOUtil.mergeFrom(protostuff, graphWrapperFromProtostuff, schema);
            LOG.debug("Returning wrapped graph object after {} ms", System.currentTimeMillis() - started);
            return graphWrapperFromProtostuff;
        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }
    }
}
