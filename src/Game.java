import bwapi.TechType;
import bwapi.UnitType;

public class Game {

    /**
     * Determines if the agent has the requisite technologies and buildings to produce a unit.
     * @param unitType a unitType from the UnitType enum
     * @return true if all prerequisites met, false otherwise.
     */
    boolean hasPrerequisitesToProduce(UnitType unitType) {
        unitType.requiredTech();
        return false;
    }
}
