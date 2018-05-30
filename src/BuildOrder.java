import bwapi.Pair;
import bwapi.UnitType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BuildOrder extends ArrayList {
    private List<Pair<Integer, UnitType>> order;
    public List<UnitType> prerequisites;
    public Pair<Integer, UnitType> currentInstruction = new Pair<>(-1, null);
    private int index;

    BuildOrder() {
        this.order = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
        this.index = 0;
    }

    void addStep(int supply, UnitType action) {
        this.order.add(new Pair<>(supply, action));
    }

    boolean ready(int supply, HashSet<UnitType> currentBuildings) {
        for (UnitType u : this.prerequisites) {
            if (!currentBuildings.contains(u)) {
                return false;
            }
        }
        return supply >= this.currentInstruction.first;
    }

    UnitType nextBuilding() {
        return this.currentInstruction.second;
    }

    Pair<Integer, UnitType> getNext() {
        if (this.index < this.order.size()) {
            if (this.nextBuilding() != null) {
                this.prerequisites.add(nextBuilding());
            }
            this.index++;
        }
        this.currentInstruction = this.order.get(this.index);
        return this.currentInstruction;
    }
}
