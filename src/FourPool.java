import bwapi.Pair;
import bwapi.UnitType;
import bwta.BaseLocation;

import java.util.LinkedList;

public class FourPool implements Strategy {
    BaseLocation baseLocation;
    BuildOrder buildOrder;

    FourPool(BaseLocation baseLocation) {
        this.baseLocation = baseLocation;
        this.buildOrder = new BuildOrder();
        this.buildOrder.addStep(4, UnitType.Zerg_Spawning_Pool);
        this.buildOrder.addStep(-1, UnitType.Zerg_Zergling);
    }


}
