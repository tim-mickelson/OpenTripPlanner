/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.raptor;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayerMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the Raptor transitLayer from the Graph
 */
public class RaptorGraphUpdater extends PollingGraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(RaptorGraphUpdater.class);

    private GraphUpdaterManager updaterManager;

    // Here the updater can be configured using the properties in the file 'Graph.properties'.
    // The property frequencySec is already read and used by the abstract base class.
    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        frequencySec = config.path("frequencySec").asInt(60);
        LOG.info("Configured Raptor polling updater: frequencySec={}", frequencySec);
    }

    // Here the updater gets to know its parent manager to execute GraphWriterRunnables.
    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        LOG.info("Raptor polling updater: updater manager is set");
        this.updaterManager = updaterManager;
    }

    // Here the updater can be initialized.
    @Override
    public void setup() {
    }

    // This is where the updater thread receives updates and applies them to the graph.
    // This method will be called every frequencySec seconds.
    @Override
    protected void runPolling() {
        LOG.info("Run Raptor polling updater with hashcode: {}", this.hashCode());
        // Execute example graph writer
        updaterManager.execute(new ExampleGraphWriter());
    }

    // Here the updater can cleanup after itself.
    @Override
    public void teardown() {
        LOG.info("Teardown Raptor polling updater");
    }
    
    // This is a private GraphWriterRunnable that can be executed to modify the graph
    private class ExampleGraphWriter implements GraphWriterRunnable {
        @Override
        public void run(Graph graph) {
            LOG.info("Raptor {} runnable is run on the "
                            + "graph writer scheduler.", this.hashCode());
            TransitLayerMapper transitLayerMapper = new TransitLayerMapper();
            graph.transitLayerRealTime = transitLayerMapper.map(graph, false);
        }
    }
}
