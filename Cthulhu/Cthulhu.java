import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class MyBot {
    public static void main(String[] args) throws java.io.IOException {
        final InitPackage iPackage = Networking.getInit();
        final int myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;
        
        //Scan the map for above average production areas
        int avgProd = avgProd(gameMap);
        int avgStr = avgStr(gameMap);
        /*
    	ArrayList<Location> productiveLocations = new ArrayList<Location>();
    	ArrayList<Location> worthlessLocations = new ArrayList<Location>();
    	for(int y = 0; y < gameMap.height; y++){
        	for(int x = 0; x < gameMap.width; x++){
        		Location l = gameMap.getLocation(x, y);
        		Site s = l.getSite();
        		if(s.production >= (4 * avgProd) / 3)
        			productiveLocations.add(l);
        		else if(s.production <= avgProd / 2)
        			worthlessLocations.add(l);
        	}
        }
        */
        Networking.sendInit("Cthulhu Bot");
        while(true) {
            List<Move> moves = new ArrayList<Move>();

            Networking.updateFrame(gameMap);

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    final Site site = location.getSite();
                    if(site.owner == myID) {
                        moves.add(new Move(location, getMove(avgProd, avgStr, myID, site, location, gameMap)));
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }
    //Main Decision Making Methods
    private static Direction getMove(int avgProd, int avgStr, int myID, Site site, Location location, GameMap map){
    	ArrayList<Location> enemyLocation = updateEnemyLocations(myID, map);
    	ArrayList<Location> goodLocations = updateGoodLocations(avgProd, avgStr, map);
    	Location nearbyLoc = findClosestLocation(goodLocations, location, map);
    	Location nearbyEnemy = findClosestLocation(enemyLocation, location, map);
    	Direction decision;
    	if(map.getDistance(nearbyLoc, location) / 1.5 < map.getDistance(nearbyEnemy, location))
    		decision = goToLocation(nearbyLoc, location, map);
    	else
    		decision = goToLocation(nearbyEnemy, location, map);
    	Location neighbor = map.getLocation(location, decision);
    	Site nearbySite = neighbor.getSite();
    	if(nearbySite.owner != myID && nearbySite.strength >= site.strength || site.strength < 5 * site.production || site.strength < 20)
    		decision = Direction.STILL;
    	return decision;
    }
    private static Direction goToLocation(Location destination, Location origin, GameMap map){
    	double origDist = map.getDistance(destination, origin);
    	ArrayList<Direction> possDirects = new ArrayList<Direction>();
    	for(Direction d : Direction.CARDINALS){
    		double newDist = map.getDistance(destination, map.getLocation(origin, d));
    		if(newDist < origDist)
    			possDirects.add(d);
    	}
    	Direction dir = attack(origin, map, possDirects);
    	if(map.getLocation(origin, dir).getSite().strength + origin.getSite().strength >= 255 && possDirects.size() > 1)
    		for(Direction d : possDirects)
    			if(!d.equals(dir))
    				return d;
    	return dir;
    }
    private static Direction attack(Location location, GameMap map, ArrayList<Direction> favorableMoves){
    	int best = 0;
    	for(int i = 0; i < favorableMoves.size(); i++){
			if(i == 0)
    			best = i;
			if(worth(map.getLocation(location, favorableMoves.get(best)), map) < worth(map.getLocation(location, favorableMoves.get(i)), map))
				best = i;
    	}
    	return favorableMoves.get(best);
    }
    //Utility Methods
    private static Location findClosestLocation(ArrayList<Location> arr, Location origin, GameMap map){
    	int best = 0;
    	double bestDistance = map.getDistance(arr.get(best), origin);
    	for(int i = 1; i < arr.size(); i++){
    		double d = map.getDistance(arr.get(i), origin);
    		if(d < bestDistance){
    			best = i;
    			bestDistance = d;
    		}
    	}
    	return arr.get(best);
    }
    private static ArrayList<Location> updateEnemyLocations(int myID, GameMap map){
    	ArrayList<Location> locations = new ArrayList<Location>();
    	for(int y = 0; y < map.height; y++){
        	for(int x = 0; x < map.width; x++){
        		Location l = map.getLocation(x, y);
        		Site s = l.getSite();
        		if(s.owner != 0 && s.owner != myID)
        			locations.add(l);
        	}
        }
    	return locations;
    }
    private static ArrayList<Location> updateGoodLocations(int avgProd, int avgStr, GameMap map){
    	ArrayList<Location> locations = new ArrayList<Location>();
    	double avgWorth = avgProd / (double) avgStr;
    	for(int y = 0; y < map.height; y++){
        	for(int x = 0; x < map.width; x++){
        		Location l = map.getLocation(x, y);
        		Site s = l.getSite();
        		if(s.owner == 0 && worth(l, map) > 1.25 * avgWorth)
        			locations.add(l);
        	}
        }
    	return locations;
    }
    //Calculation Methods
    private static double worth(Location location, GameMap map){
    	Site site = location.getSite();
    	if(site.owner == 0 && site.strength > 0)
    		return site.production / (double)site.strength;
    	if(site.owner == 0 && site.strength == 0)
    		return site.production;
		//Choose the attack with the most impact in terms of health
		int potential = 0;
		for(Direction d : Direction.CARDINALS){
			Site s = map.getSite(location, d);
			if(s.owner > 0 && s.owner != site.owner){
				potential += site.strength;
			}
		}
		return potential;
   	} 
    private static int avgProd(GameMap map){
    	int total = 0;
    	for(int y = 0; y < map.height; y++){
        	for(int x = 0; x < map.width; x++){
        		total += map.getLocation(x, y).getSite().production;
        	}
        }
    	return total / (map.width*map.height) + 1;
    }
    private static int avgStr(GameMap map){
    	int total = 0;
    	for(int y = 0; y < map.height; y++){
        	for(int x = 0; x < map.width; x++){
        		total += map.getLocation(x, y).getSite().strength;
        	}
        }
    	return total / (map.width*map.height) + 1;
    }
}
