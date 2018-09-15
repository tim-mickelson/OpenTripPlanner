package org.opentripplanner.serializer;

import com.google.common.io.ByteStreams;
import io.protostuff.*;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ProtostuffGraphDeserializer implements GraphDeserializer {

    private static final int SIZE_LIMIT = 2000000000;
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


            CodedInput input = new CodedInput(inputStream, true);
            input.setSizeLimit(SIZE_LIMIT);
            GraphCodedInput graphInput = new GraphCodedInput(input);
            schema.mergeFrom(graphInput, graphWrapperFromProtostuff);
            input.checkLastTagWas(0);

            LOG.debug("Returning wrapped graph object after {} ms", System.currentTimeMillis() - started);
            return graphWrapperFromProtostuff;
        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize graph", e);
        }
    }
}
