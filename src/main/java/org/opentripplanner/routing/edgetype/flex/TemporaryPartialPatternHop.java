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

package org.opentripplanner.routing.edgetype.flex;

import com.vividsolutions.jts.geom.LineString;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

public class TemporaryPartialPatternHop extends PartialPatternHop implements TemporaryEdge {
    public TemporaryPartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex, double buffer) {
        super(hop, from, to, fromStop, toStop, startIndex, endIndex, buffer);
    }

    public TemporaryPartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex,
                             LineString startGeometry, int startVehicleTime, LineString endGeometry, int endVehicleTime, double buffer) {
        super(hop, from, to, fromStop, toStop, startIndex, endIndex, startGeometry, startVehicleTime, endGeometry, endVehicleTime, buffer);
    }

    // pass-thru for TemporaryDirectPatternHop
    public TemporaryPartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop) {
        super(hop, from, to, fromStop, toStop);
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }

}