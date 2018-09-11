package org.opentripplanner.serializer;

import org.apache.commons.lang.NotImplementedException;

import java.io.File;
import java.io.InputStream;

public class KryoGraphDeserializer implements GraphDeserializer {

    @Override
    public GraphWrapper deserialize(File file) {
        throw new NotImplementedException("Kryo not implemented yet");
    }

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        throw new NotImplementedException("Kryo not implemented yet");
    }
}
