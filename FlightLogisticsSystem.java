import java.util.*;

class Flight {
    String airline;
    String destination;
    int cost;
    int time;

    public Flight(String airline, String destination, int cost, int time) {
        this.airline = airline;
        this.destination = destination;
        this.cost = cost;
        this.time = time;
    }
}

class Node {
    String city;
    int cost;
    int time;
    List<Flight> path;

    public Node(String city, int cost, int time, List<Flight> path) {
        this.city = city;
        this.cost = cost;
        this.time = time;
        this.path = new ArrayList<>(path);
    }
}

public class FlightLogisticsSystem {
    private static Map<String, List<Flight>> flightsGraph = new HashMap<>();

    public static void addFlight(String source, String destination, String airline, int cost, int time) {
        flightsGraph.putIfAbsent(source, new ArrayList<>());
        flightsGraph.get(source).add(new Flight(airline, destination, cost, time));
    }

    public static void findCheapestFlight(String src, String dest) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.cost));
        Map<String, Integer> minCost = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minCost.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int costSoFar = currentNode.cost;

            if (currentCity.equals(dest)) {
                System.out.println("Minimum cost to " + dest + " is " + costSoFar + " with time " + currentNode.time + " minutes.");
                System.out.println("Flights to take:");
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + currentCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    currentCity = flight.destination;
                }
                return;
            }

            for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
                int newCost = costSoFar + flight.cost;
                if (newCost < minCost.getOrDefault(flight.destination, Integer.MAX_VALUE)) {
                    minCost.put(flight.destination, newCost);
                    List<Flight> newPath = new ArrayList<>(currentNode.path);
                    newPath.add(flight);
                    pq.add(new Node(flight.destination, newCost, currentNode.time + flight.time, newPath));
                }
            }
        }
        System.out.println("No route available from " + src + " to " + dest);
    }
    public static void findAllFlightsSortedByCost(String src, String dest) {
        List<Node> allRoutes = new ArrayList<>();
        List<Flight> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        dfs(src, dest, 0, 0, currentPath, allRoutes, visited);
        allRoutes.sort(Comparator.comparingInt(node -> node.cost));
        if (allRoutes.isEmpty()) {
            System.out.println("No routes available from " + src + " to " + dest);
        } else {
            System.out.println("All possible routes from " + src + " to " + dest + " sorted by cost:");
            for (Node route : allRoutes) {
                System.out.println("Total cost: " + route.cost + ", Total time: " + route.time + " minutes");
                System.out.println("Flights to take:");
                String startCity = src;
                for (Flight flight : route.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + startCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    startCity = flight.destination;
                }
                System.out.println();
            }
        }
    }

    private static void dfs(String currentCity, String dest, int currentCost, int currentTime, List<Flight> currentPath, List<Node> allRoutes, Set<String> visited) {
        if (currentCity.equals(dest)) {
            allRoutes.add(new Node(currentCity, currentCost, currentTime, new ArrayList<>(currentPath)));
            return;
        }
        visited.add(currentCity);
        for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
            if (!visited.contains(flight.destination)) {
                currentPath.add(flight);  // Add flight to the current path
                dfs(flight.destination, dest, currentCost + flight.cost, currentTime + flight.time, currentPath, allRoutes, visited);
                currentPath.remove(currentPath.size() - 1);  // Remove flight after DFS call
            }
        }
        visited.remove(currentCity);
    }

    public static void findFastestFlight(String src, String dest) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.time));
        Map<String, Integer> minTime = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minTime.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int timeSoFar = currentNode.time;

            if (currentCity.equals(dest)) {
                System.out.println("Minimum time to " + dest + " is " + timeSoFar + " minutes with cost " + currentNode.cost);
                System.out.println("Flights to take:");
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + currentCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    currentCity = flight.destination;
                }
                return;
            }

            for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
                int newTime = timeSoFar + flight.time;
                if (newTime < minTime.getOrDefault(flight.destination, Integer.MAX_VALUE)) {
                    minTime.put(flight.destination, newTime);
                    List<Flight> newPath = new ArrayList<>(currentNode.path);
                    newPath.add(flight);
                    pq.add(new Node(flight.destination, currentNode.cost + flight.cost, newTime, newPath));
                }
            }
        }
        System.out.println("No route available from " + src + " to " + dest);
    }
    public static void findAllFlightsSortedByTime(String src, String dest) {
        List<Node> allRoutes = new ArrayList<>();
        List<Flight> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfsByTime(src, dest, 0, 0, currentPath, allRoutes, visited);
        allRoutes.sort(Comparator.comparingInt(node -> node.time));
        if (allRoutes.isEmpty()) {
            System.out.println("No routes available from " + src + " to " + dest);
        } else {
            System.out.println("All possible routes from " + src + " to " + dest + " sorted by time:");
            for (Node route : allRoutes) {
                System.out.println("Total time: " + route.time + " minutes, Total cost: " + route.cost);
                System.out.println("Flights to take:");
                String startCity = src;
                for (Flight flight : route.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + startCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    startCity = flight.destination;
                }
                System.out.println();
            }
        }
    }

    private static void dfsByTime(String currentCity, String dest, int currentCost, int currentTime, List<Flight> currentPath, List<Node> allRoutes, Set<String> visited) {
        if (currentCity.equals(dest)) {
            allRoutes.add(new Node(currentCity, currentCost, currentTime, new ArrayList<>(currentPath)));
            return;
        }
        visited.add(currentCity);
        for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
            if (!visited.contains(flight.destination)) {
                currentPath.add(flight);
                dfsByTime(flight.destination, dest, currentCost + flight.cost, currentTime + flight.time, currentPath, allRoutes, visited);
                currentPath.remove(currentPath.size() - 1);
            }
        }
        visited.remove(currentCity);
    }
    public static void FlightsWithoutLayovers(String src, String dest) {
        System.out.println("Choose option:\n1. Direct Flights Only\n2. Flights With Layovers");
        Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();
        boolean directFound = false;
        for (Flight flight : flightsGraph.getOrDefault(src, new ArrayList<>())) {
            if (flight.destination.equals(dest)) {
                System.out.println("Direct flight found: Airline: " + flight.airline + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                directFound = true;
            }
        }
    }

        public static void main(String[] args) {
        addFlight("Chennai", "Delhi", "Spice Jet", 10000, 180);
        addFlight("Chennai", "Kolkata", "Air India", 6000, 120);

        addFlight("Delhi", "Kolkata", "Air India", 6000,150);
        addFlight("Delhi", "Mumbai", "Indigo", 6000,120);
        addFlight("Delhi", "Pune", "Air India", 8000,120);
        addFlight("Delhi", "Hyderabad", "Indigo", 7000,180);

        addFlight("Kolkata", "Bangalore", "Air India", 6000,120);
        addFlight("Kolkata", "Hyderabad", "Indigo", 7000,90);

        addFlight("Bhubaneswar", "Mumbai", "Air India", 6000,150);
        addFlight("Bhubaneswar", "Kolkata", "Indigo", 5000,90);

        addFlight("Jaipur", "Kolkata", "Air India", 6000,90);
        addFlight("Jaipur", "Bhopal", "Indigo", 5000,60);

        addFlight("Bhopal", "Hyderabad", "Spice Jet", 5000,120);
        addFlight("Bhopal", "Mumbai", "Spice Jet", 7000,120);

        addFlight("Mumbai", "Delhi", "Spice Jet", 5000,120);
        addFlight("Mumbai", "Bhopal", "Air India", 4000,120);
        addFlight("Mumbai", "Jaipur", "Indigo", 5000,90);
        addFlight("Mumbai", "Delhi", "Air India", 8000,120);
        addFlight("Mumbai", "Bhopal", "Indigo", 3000,120);
        addFlight("Mumbai", "Jaipur", "Air India", 7000,90);

        addFlight("Pune", "Hyderabad", "Spice Jet", 5000,120);
        addFlight("Pune", "Bangalore", "Air India", 6000,120);
        addFlight("Pune", "Hyderabad", "Air India", 5000,120);
        addFlight("Pune", "Bangalore", "Spice Jet", 3000,120);
        addFlight("Pune", "Mumbai", "Indigo", 2000,20);

        addFlight("Hyderabad", "Delhi", "Spice Jet", 7000,150);
        addFlight("Hyderabad", "Bangalore", "Air India", 3000,45);
        addFlight("Hyderabad", "Chennai", "Indigo", 2000,45);

        addFlight("Bangalore", "Bhubaneswar", "Spice Jet", 7000,75);
        addFlight("Bangalore", "Mumbai", "Air India", 7000,90);
        addFlight("Bangalore", "Delhi", "Indigo", 5000,120);

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter source city: ");
        String source = sc.nextLine();
        System.out.println("Enter destination city: ");
        String destination = sc.nextLine();

        System.out.println("Choose an option: \n1. Minimum cost\n2. All possible routes sorted by cost\n3. Minimum time\n4. All possible routes sorted by time\n5. Show only direct flights");
        int choice = sc.nextInt();

        switch (choice) {
            case 1:
                findCheapestFlight(source, destination);
                break;
            case 2:
                findAllFlightsSortedByCost(source, destination);
                break;
            case 3:
                findFastestFlight(source, destination);
                break;
            case 4:
                findAllFlightsSortedByTime(source, destination);
                break;
            case 5:
                FlightsWithoutLayovers(source, destination);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }
}
