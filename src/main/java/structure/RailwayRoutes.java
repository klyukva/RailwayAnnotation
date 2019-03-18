package structure;

import java.util.*;

public class RailwayRoutes {

  private RailwayRoutes () {
    throw new IllegalAccessError();
  }

  public static String railwayCheck(String inputString) {

    boolean collision = false;
    if (inputString == "" || inputString == null )
      return "Incorrect arguments";

    String[] routesAndTrainRides = inputString.split(";");
    Map<Long, Integer> routes = new HashMap<>();

    try {
      Arrays.stream(routesAndTrainRides[0].split(","))
              .map(s -> Arrays.stream(s.split(" ")).mapToInt(Integer::parseInt).toArray())
              .filter(s -> s.length == 3 && Arrays.stream(s).allMatch(k -> k > 0) && s[0] != s[1])
              .forEach(s -> routes.putIfAbsent(twoIntToLong(s[0], s[1]), s[2]));
    } catch (NumberFormatException e) {
      return "Incorrect arguments";
    }


    Map<Long, Set<PairStationDirection>> roadsVisited = new HashMap<>();
    Set<StationVisitTime> stationsVisited = new HashSet<>();

    for (String oneTrip : routesAndTrainRides[1].split(",")) {
      int [] parsedOneTrip = Arrays.stream(oneTrip.split(" ")).mapToInt(Integer::parseInt).toArray();

      if (parsedOneTrip.length <= 1)
        continue;

      int currentTime = 0;

      if (meetOnStation(stationsVisited, currentTime, parsedOneTrip[0]))
        collision = true;

      for (int i = 0; i < parsedOneTrip.length - 1; i++){
        PairStationDirection pairStationDirection;
        Long key;

        if (parsedOneTrip[i] < parsedOneTrip[i+1]){
          pairStationDirection = new PairStationDirection(currentTime, true);
        } else if (parsedOneTrip[i] > parsedOneTrip[i+1]) {
          pairStationDirection = new PairStationDirection(currentTime, false);
        } else {
          continue;
        }

        key = twoIntToLong(parsedOneTrip[i], parsedOneTrip[i+1]);

        if (!routes.containsKey(key)){
          return "Non-existent route";
        }

        int pathTime = routes.get(key);

        if (meetOnStation(stationsVisited, currentTime + pathTime, parsedOneTrip[i+1]))
          collision = true;

        if(!roadsVisited.containsKey(key)) {
          roadsVisited.put(key, new HashSet<>());
        } else {
          if (pairStationDirection.crossingTrains(roadsVisited.get(key), pathTime)){
            collision = true;
          }
        }

        roadsVisited.get(key).add(pairStationDirection);
        currentTime += pathTime;
      }
    }
    if (collision)
      return "Collision";
    return "No collision";
  }

  public static boolean meetOnStation (Set<StationVisitTime> alreadyCheckedStations,
                                       int currentTime, int stationNumber) {

    StationVisitTime checkingStation = new StationVisitTime(currentTime, stationNumber);
    if (alreadyCheckedStations.contains(checkingStation))
      return true;

    alreadyCheckedStations.add(checkingStation);
    return false;
  }

  public static Long twoIntToLong (int a, int b) {
    if (a < b)
      return ((long) b << 32) + a;
    else
      return ((long) a << 32) + b;
  }

  static class StationVisitTime {
    private int visitTime;
    private int numberStation;

    public StationVisitTime(int visitTime, int numberStation) {

      this.visitTime = visitTime;
      this.numberStation = numberStation;
    }

    @Override
    public boolean equals(Object o) {

      if (this == o) return true;
      if (!(o instanceof StationVisitTime)) return false;

      StationVisitTime that = (StationVisitTime) o;

      if (visitTime != that.visitTime) return false;
      return numberStation == that.numberStation;
    }

    @Override
    public int hashCode() {

      int result = visitTime;
      result = 31 * result + numberStation;
      return result;
    }
  }

  static class PairStationDirection {

    private int timePassingStation;
    private boolean direction;

    public PairStationDirection(int timePassingStation, boolean direction){

      this.timePassingStation = timePassingStation;
      this.direction = direction;
    }

    public boolean crossingTrains (Set<PairStationDirection> pairStationDirections, int travelTime){

      for (PairStationDirection pairStationDirection : pairStationDirections){
        if (this.timePassingStation == pairStationDirection.timePassingStation)
          return true;

        if (this.direction != pairStationDirection.direction
                && Math.abs(this.timePassingStation - pairStationDirection.timePassingStation) <= travelTime)
          return true;
      }
      return false;
    }
  }
}


