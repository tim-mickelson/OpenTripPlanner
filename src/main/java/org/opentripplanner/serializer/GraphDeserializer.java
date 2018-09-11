package org.opentripplanner.serializer;

import java.io.File;
import java.io.InputStream;

public interface GraphDeserializer {

    GraphWrapper deserialize(File file) throws GraphSerializationException;

    GraphWrapper deserialize(InputStream inputStream) throws GraphSerializationException;
}
