package org.opentripplanner.common.diff;

import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;

public class GenericObjectDifferTest {

    private GenericObjectDiffer genericObjectDiffer = new GenericObjectDiffer();

    private GenericDiffConfig genericDiffConfig = GenericDiffConfig.builder()
            .build();

    private DiffPrinter diffPrinter = new DiffPrinter();

    @Test
    public void testDiff() throws IllegalAccessException {

        Graph graph = new Graph();
        Graph graph2 = new Graph();

        List<Difference> differences = genericObjectDiffer.compareObjects(graph, graph2, genericDiffConfig);

        System.out.println(diffPrinter.diffListToString(differences));
    }

}