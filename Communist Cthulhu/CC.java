import java.util.ArrayList;
import java.util.List;
public class MyBot {
    public static void main(String[] args) throws java.io.IOException {
        final InitPackage iPackage = Networking.getInit();
        final int myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;
        
        //Scan the map for above average production areas
        final int avgProd = avgProd(gameMap);
        final int avgStr = avgStr(gameMap);
        Networking.sendInit("Communist Cthulhu Bot");
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
    	if(map.getDistance(nearbyLoc, location) < map.getDistance(nearbyEnemy, location))
    		decision = goToLocation(nearbyLoc, location, map);
    	else
    		decision = goToLocation(nearbyEnemy, location, map);
    	ArrayList<Direction> expansionOptions = new ArrayList<Direction>();
    	for(Direction d : Direction.CARDINALS){
    		Location tempLoc = map.getLocation(location, d);
    		Site tempSite = tempLoc.getSite();
    		if(tempSite.owner == 0 && worth(tempLoc, map) > (1.25 * avgProd) / avgStr)
    			expansionOptions.add(d);
    	}
    	if(expansionOptions.size() > 1)
    		decision = attack(location, map, expansionOptions);
    	Location neighbor = map.getLocation(location, decision);
    	Site nearbySite = neighbor.getSite();
    	if(nearbySite.owner != myID && nearbySite.strength >= site.strength || site.strength < 5 * site.production || site.strength < 20)
    		decision = Direction.STILL;
    	if(enemyCount(3, myID, location, map) >= 2 && site.strength > 5 * site.production || enemyCount(3, myID, location, map) >= 2 && site.strength > 20)
    		decision = nearestEnemyGroup(myID, location, map);
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
    	Direction dir = easiestPath(origin, map, possDirects);
    	Site target = map.getLocation(origin, dir).getSite();
    	if(target.strength + origin.getSite().strength >= 255 + 2 * target.production){
    		ArrayList<Direction> neutralAttacks = new ArrayList<Direction>();
    		for(Direction d : Direction.CARDINALS){
    			Location neighbor = map.getLocation(origin, d);
    			if(origin.getSite().owner != neighbor.getSite().owner && origin.getSite().strength > neighbor.getSite().strength)
    				neutralAttacks.add(d);
    		}
    		if(neutralAttacks.isEmpty())
    			dir = Direction.STILL;
    		else
    			dir = attack(origin, map, neutralAttacks);
    	}
    	return dir;
    }
    private static Direction easiestPath(Location location, GameMap map, ArrayList<Direction> favorableMoves){
    	for(Direction d : favorableMoves){
    		Location target = map.getLocation(location, d);
    		if(target.getSite().owner == location.getSite().owner && target.getSite().strength + location.getSite().strength  <= 255)
    			return d;
    	}
    	return attack(location, map, favorableMoves);
    }
    private static Direction attack(Location location, GameMap map, ArrayList<Direction> favorableMoves){
    	int best = 0;
    	for(int i = 0; i < favorableMoves.size(); i++){
			if(worth(map.getLocation(location, favorableMoves.get(best)), map) < worth(map.getLocation(location, favorableMoves.get(i)), map))
				best = i;
    	}
    	return favorableMoves.get(best);
    }
    private static Direction nearestEnemyGroup(int myID, Location location, GameMap map){
    	int enemyCtr;
    	int bestCtr = 0;
    	Direction dir = Direction.STILL;
    	for(Direction d : Direction.CARDINALS){
    		enemyCtr = 0;
    		Location loc = location;
        	Site s = map.getSite(loc, d);
    		for(int i = 3; i > 0; i--){
    			loc = map.getLocation(loc, d);
    			s = map.getSite(loc);
    			if(s.owner != 0 && s.owner != myID)
    				enemyCtr ++;
    		}
    		if(enemyCtr > bestCtr){
    			dir = d;
    		}
    	}
    	return dir;
    }
    //Utility Methods
    private static int enemyCount(int radius, int myID, Location location, GameMap map){
    	int ctr = 0;
    	for(Direction d : Direction.CARDINALS){
    		Location loc = location;
        	Site s = map.getSite(loc, d);
    		for(int i = radius; i > 0; i--){
    			loc = map.getLocation(loc, d);
    			s = map.getSite(loc);
    			if(s.owner != 0 && s.owner != myID)
    				ctr ++;
    		}
    	}
    	return ctr;
    }
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
