package org.opentripplanner.serializer;

import java.io.InputStream;

public interface GraphDeserializer {

    GraphWrapper deserialize(InputStream inputStream) throws GraphSerializationException;
}
