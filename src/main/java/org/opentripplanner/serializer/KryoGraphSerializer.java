package org.opentripplanner.serializer;

import org.apache.commons.lang.NotImplementedException;

import java.io.InputStream;
import java.io.OutputStream;

public class KryoGraphSerializer implements GraphSerializer {

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        throw new NotImplementedException("Kryo not implemented yet");
    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) {
        throw new NotImplementedException("Kryo not implemented yet");
    }
}
