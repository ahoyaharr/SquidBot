import bwapi.*;
import bwapi.Game;

import bwta.BWTA;
import bwta.BaseLocation;

import java.util.*;
import java.util.stream.Collectors;

public class SquidBot extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    private Strategy strategy;

    HashMap<UnitType, List<Unit>> units = new HashMap<>();

    boolean hasSpawningPool = false;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        UnitType unitType = unit.getType();
        if (!units.containsKey(unitType)) {
            units.put(unitType, new ArrayList<>());
        }
        units.get(unitType).add(unit);

        System.out.println("New unit discovered " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");

        int i = 0;
        for (BaseLocation baseLocation : BWTA.getBaseLocations()) {
            System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
            for (Position position : baseLocation.getRegion().getPolygon().getPoints()) {
                System.out.print(position + ", ");
            }
            System.out.println();
        }
    }

    public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
        TilePosition ret = null;
        int maxDist = 3;
        int stopDist = 40;

        // Refinery, Assimilator, Extractor
        if (buildingType.isRefinery()) {
            for (Unit n : game.neutral().getUnits()) {
                if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
                        (Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist) &&
                        (Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist)
                        ) return n.getTilePosition();
            }
        }

        while ((maxDist < stopDist) && (ret == null)) {
            for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX() + maxDist; i++) {
                for (int j = aroundTile.getY() - maxDist; j <= aroundTile.getY() + maxDist; j++) {
                    if (game.canBuildHere(new TilePosition(i, j), buildingType, builder, false)) {
                        // units that are blocking the tile
                        boolean unitsInWay = false;
                        for (Unit u : game.getAllUnits()) {
                            if (u.getID() == builder.getID()) continue;
                            if ((Math.abs(u.getTilePosition().getX() - i) < 4) && (Math.abs(u.getTilePosition().getY() - j) < 4))
                                unitsInWay = true;
                        }
                        if (!unitsInWay) {
                            return new TilePosition(i, j);
                        }
                        // creep for Zerg
                        if (buildingType.requiresCreep()) {
                            boolean creepMissing = false;
                            for (int k = i; k <= i + buildingType.tileWidth(); k++) {
                                for (int l = j; l <= j + buildingType.tileHeight(); l++) {
                                    if (!game.hasCreep(k, l)) creepMissing = true;
                                    break;
                                }
                            }
                            if (creepMissing) continue;
                        }
                    }
                }
            }
            maxDist += 2;
        }

        if (ret == null) game.printf("Unable to find suitable build position for " + buildingType.toString());
        return ret;
    }

    private void dispatchIdleWorkers(List<Unit> workers, List<Unit> minerals) {
        for (Unit worker : workers) {
            //if it's a worker and it's idle, send it to the closest mineral patch
            if (worker.getType().isWorker() && worker.isIdle()) {
                Unit closestMineral = null;

                for (Unit mineral : minerals) {
                    if (closestMineral == null || worker.getDistance(mineral) < worker.getDistance(closestMineral)) {
                        closestMineral = mineral;
                    }
                }

                minerals.remove(closestMineral);

                //if a mineral patch was found, send the worker to gather it
                if (closestMineral != null) {
                    worker.gather(closestMineral, false);
                }
            }
        }
    }

    @Override
    public void onFrame() {
        //game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder units = new StringBuilder("My units:\n");



        // List of all of the minerals
        List<Unit> minerals = game.neutral().getUnits().stream()
                .filter(u -> u.getType().isMineralField()).collect(Collectors.toList());

        List<Unit> idleWorkers = new ArrayList<>();

        if (!hasSpawningPool && self.minerals() > 200) {
            Unit worker = self.getUnits().stream().filter(u -> u.getType().isWorker()).collect(Collectors.toList()).get(0);
            TilePosition buildLocation = getBuildTile(worker, UnitType.Zerg_Spawning_Pool, worker.getTilePosition());
            worker.build(UnitType.Zerg_Spawning_Pool, buildLocation);
        } else if (self.minerals() > 300) {
            Unit worker = self.getUnits().stream().filter(u -> u.getType().isWorker()).collect(Collectors.toList()).get(0);
            TilePosition buildLocation = getBuildTile(worker, UnitType.Zerg_Hatchery, worker.getTilePosition());
            worker.build(UnitType.Zerg_Hatchery, buildLocation);
        }

        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            if (!hasSpawningPool && myUnit.getType() == UnitType.Zerg_Spawning_Pool) {
                hasSpawningPool = true;
            }
            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Zerg_Larva && self.minerals() >= 50) {
                if (self.supplyTotal() - self.supplyUsed() < 2 && self.minerals() >= 100) {
                    myUnit.train(UnitType.Zerg_Overlord);
                } else if (hasSpawningPool && new Random().nextInt(100) > 50) {
                    myUnit.train(UnitType.Zerg_Zergling);
                } else {
                    myUnit.train(UnitType.Zerg_Drone);
                }
            }

            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                idleWorkers.add(myUnit);
            }
        }

        dispatchIdleWorkers(idleWorkers, minerals);


        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }

    public static void main(String[] args) {
        new SquidBot().run();
    }
}