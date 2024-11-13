

/*
DSA PROJECT :
AirEase: A Flight Logistics System
By : UCE2023615
UCE2023609
UCE2023607
UCE2023604

Problem Statement :
AirEase aims to simplify the airline management process by providing a user-friendly system for both travelers and administrators.
The system will allow users to search for and book flights based on minimum cost or shortest travel distance between cities,
facilitating quick decision-making for travelers.
Admins can access a dedicated interface to manage flight bookings, reschedule flights based on real-time weather data,
and ensure a seamless experience for users despite weather disruptions.
This dual-interface system will be built to handle the complexities of real-world logistics with efficient data structures and algorithms.
 */


import java.io.BufferedReader;
import java.util.*;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


class WeatherService {

    private static final String API_KEY = "ba45b9824ab3e00ac24a08b49cd9e93b"; // Ensure there are no leading or trailing spaces
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";

    public static String getWeatherForCity(String cityName) {
        try {
            // URL encode the city name to handle spaces and special characters
            String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8.toString());
            String urlString = BASE_URL + encodedCityName + "&appid=" + API_KEY + "&units=metric";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            return parseWeatherData(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not fetch weather data.";
        }

    }

    // Add these parameters to the parsing function
    private static String parseWeatherData(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonObject main = jsonObject.getAsJsonObject("main");
        JsonObject weather = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();
        JsonObject wind = jsonObject.getAsJsonObject("wind");

        double temperature = main.get("temp").getAsDouble();
        String description = weather.get("description").getAsString();
        int humidity = main.get("humidity").getAsInt();
        double windSpeed = wind.get("speed").getAsDouble() * 3.6; // Convert from m/s to km/h
        int windDegree = wind.get("deg").getAsInt(); // Wind direction
        double visibility = jsonObject.has("visibility") ? jsonObject.get("visibility").getAsDouble() : 10000; // Visibility in meters (default to 10 km)

        // Safety check criteria
        boolean isSafe = true;
        StringBuilder message = new StringBuilder();

        if (windSpeed > 50) {
            isSafe = false;
            message.append("High wind speeds detected. Flights may be rescheduled. ");
        }

        if (visibility < 2000) {
            isSafe = false;
            message.append("Low visibility detected. Flights may be rescheduled. ");
        }

        if (description.toLowerCase().contains("thunderstorm") || description.toLowerCase().contains("snow")) {
            isSafe = false;
            message.append("Adverse weather conditions detected (thunderstorms, snow). Flights may be rescheduled. ");
        }

        // Default message for safe conditions
        if (isSafe) {
            message.append("Arrivals and departures are likely safe.");
        } else {
            message.append("Conditions indicate potential delays or rescheduling.");
        }

        // Return weather details along with the safety message
        return String.format("Temperature: %.1f°C, Condition: %s, Humidity: %d%%, Wind Speed: %.1f km/h, Visibility: %.0f meters, Status: %s",
                temperature, description, humidity, windSpeed, visibility, message.toString());
    }
}

class Flight {
    String airline;
    String destination;
    int cost;
    int time;
    String flightNo;
    public Flight(String airline, String destination, int cost, int time, String flightNo) {
        this.airline = airline;
        this.destination = destination;
        this.cost = cost;
        this.time = time;
        this.flightNo = flightNo;
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

class FlightLogisticsSystem {
    static Map<String, List<Flight>> flightsGraph = new HashMap<>(); // Adjacency list for storing flights between cities
    static List<String> badWeatherCities = new ArrayList<>();  //Stores cities with bad weather

    // Adds a flight to the graph if weather is safe at both source and destination
    public static void addFlight(String source, String destination, String airline, int cost, int time, String flightNo) {
        if(WeatherService.getWeatherForCity(source).contains("safe") && WeatherService.getWeatherForCity(destination).contains("safe")) {
            flightsGraph.putIfAbsent(source, new ArrayList<>());
            flightsGraph.get(source).add(new Flight(airline, destination, cost, time,flightNo));
        }else{
            //Adds cities with rescheduled flights to the bad weather list
            if(WeatherService.getWeatherForCity(source).contains("rescheduled") && !badWeatherCities.contains(source)) {
                badWeatherCities.add(source);
            }
            if(WeatherService.getWeatherForCity(destination).contains("rescheduled") && !badWeatherCities.contains(destination)){
                badWeatherCities.add(destination);
            }
        }
    }
    // method to remove a specific flight
    public static void removeFlight(String source, String destination, String airline, int cost, int time, String flightNo) {
        List<Flight> flights = flightsGraph.getOrDefault(source, new ArrayList<>());
        flights.removeIf(flight -> flight.destination.equals(destination) && flight.airline.equals(airline) &&
                flight.cost == cost && flight.time == time && flight.flightNo.equals(flightNo));
        System.out.println("Flight removed successfully!");
    }

    // Finds the cheapest route from src to dest using Dijkstra's algorithm based on cost
    public static void findCheapestFlight(String src, String dest) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.cost));
        Map<String, Integer> minCost = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minCost.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int costSoFar = currentNode.cost;

            // If destination is reached, print details and return
            if (currentCity.equals(dest)) {
                System.out.println("Minimum cost to " + dest + " is " + costSoFar + " with time " + currentNode.time + " minutes.");
                System.out.println("Flights to take:");
                String tempCity = src; // Set the starting city for the output
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + tempCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    tempCity = flight.destination;
                }
                return;
            }
            // Explore connected flights and update minimum cost path if a cheaper route is found
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

    // Finds all routes from src to dest and sorts them by cost
    public static void findAllFlightsSortedByCost(String src, String dest) {
        List<Node> allRoutes = new ArrayList<>();
        List<Flight> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // DFS for finding all paths with cost tracking
        dfsForCost(src, dest, 0, 0, currentPath, allRoutes, visited);
        allRoutes.sort(Comparator.comparingInt(node -> node.cost));

        // Print all routes if available, otherwise print no route found
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
    // DFS helper method to explore all paths by cost
    private static void dfsForCost(String currentCity, String dest, int currentCost, int currentTime, List<Flight> currentPath, List<Node> allRoutes, Set<String> visited) {
        if (currentCity.equals(dest)) {
            allRoutes.add(new Node(currentCity, currentCost, currentTime, new ArrayList<>(currentPath)));
            return;
        }
        visited.add(currentCity);
        for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
            if (!visited.contains(flight.destination)) {
                currentPath.add(flight);  // Add flight to the current path
                dfsForCost(flight.destination, dest, currentCost + flight.cost, currentTime + flight.time, currentPath, allRoutes, visited);
                currentPath.remove(currentPath.size() - 1);  // Remove flight after DFS call
            }
        }
        visited.remove(currentCity);
    }

    // Finds the fastest route from src to dest using Dijkstra's algorithm based on time
    public static void findFastestFlight(String src, String dest) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.time));
        Map<String, Integer> minTime = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minTime.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int timeSoFar = currentNode.time;
            // If destination is reached, print details and return
            if (currentCity.equals(dest)) {
                System.out.println("Minimum time to " + dest + " is " + timeSoFar + " minutes with cost " + currentNode.cost);
                System.out.println("Flights to take:");
                String tempCity = src; // Set the starting city for the output
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + tempCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    tempCity = flight.destination;
                }
                return;
            }
            // Explore connected flights and update minimum time path if a faster route is found
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

    // Finds all routes from src to dest sorted by travel time
    public static void findAllFlightsSortedByTime(String src, String dest) {
        List<Node> allRoutes = new ArrayList<>();
        List<Flight> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // DFS for finding all paths with time tracking
        dfsByTime(src, dest, 0, 0, currentPath, allRoutes, visited);
        allRoutes.sort(Comparator.comparingInt(node -> node.time));

        // Print all routes if available, otherwise print no route found
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
    // DFS method to explore all paths by time
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

    // Method to find and display direct flights between two cities
    public static void FlightsWithoutLayovers(String src, String dest) {

        boolean directFound = false;

        // Iterate through all flights from the source city
        for (Flight flight : flightsGraph.getOrDefault(src, new ArrayList<>())) {
            if (flight.destination.equals(dest)) {
                System.out.println("Direct flight found: Airline: " + flight.airline + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                directFound = true;
            }
        }
        if(!directFound){
            System.out.println("No direct flight between "+src+ " and "+dest);
        }
    }


    public static void main(String[] args) {
        System.out.println("Welcome to AirEase - Your Ultimate Flight Logistics Companion!");
        System.out.println("Effortlessly manage flights, track routes, and ensure a smooth journey for passengers and cargo alike. From real-time updates to optimized routing, AirEase is here to make airline management simpler and more efficient.");
        System.out.println("Fasten your seatbelt, and let’s get started!");

        System.out.println("We appreciate your patience while we are fetching real-time weather data to ensure your journey is safe and free of turbulent weather. ");
        addFlight("Chennai", "Delhi", "Spice Jet", 10000, 180, "SG801");
        addFlight("Chennai", "Kolkata", "Air India", 6000, 120,"AI801");

        addFlight("Delhi", "Kolkata", "Air India", 6000, 150,"AI802");
        addFlight("Delhi", "Mumbai", "Indigo", 6000, 120,"6E801");
        addFlight("Delhi", "Pune", "Air India", 8000, 120,"AI803");
        addFlight("Delhi", "Hyderabad", "Indigo", 7000, 180,"6E802");

        addFlight("Kolkata", "Bangalore", "Air India", 6000, 120,"AI804");
        addFlight("Kolkata", "Hyderabad", "Indigo", 7000, 90,"6E803");

        addFlight("Bhubaneswar", "Mumbai", "Air India", 6000, 150,"AI805");
        addFlight("Bhubaneswar", "Kolkata", "Indigo", 5000, 90,"6E804");

        addFlight("Jaipur", "Kolkata", "Air India", 6000, 90,"AI806");
        addFlight("Jaipur", "Bhopal", "Indigo", 5000, 60,"6E805");

        addFlight("Bhopal", "Hyderabad", "Spice Jet", 5000, 120,"SG802");
        addFlight("Bhopal", "Mumbai", "Spice Jet", 7000, 120,"SG803");

        addFlight("Mumbai", "Delhi", "Spice Jet", 5000, 120,"SG804");
        addFlight("Mumbai", "Bhopal", "Air India", 4000, 120,"AI807");
        addFlight("Mumbai", "Jaipur", "Indigo", 5000, 90,"6E806");
        addFlight("Mumbai", "Delhi", "Air India", 8000, 120,"AI808");
        addFlight("Mumbai", "Bhopal", "Indigo", 3000, 120,"6E807");
        addFlight("Mumbai", "Jaipur", "Air India", 7000, 90,"AI809");

        addFlight("Pune", "Hyderabad", "Spice Jet", 5000, 120,"SG805");
        addFlight("Pune", "Bangalore", "Air India", 6000, 120,"AI810");
        addFlight("Pune", "Hyderabad", "Air India", 5000, 120,"AI810");
        addFlight("Pune", "Bangalore", "Spice Jet", 3000, 120,"SG806");
        addFlight("Pune", "Mumbai", "Indigo", 2000, 20,"6E808");

        addFlight("Hyderabad", "Delhi", "Spice Jet", 7000, 150,"SG806");
        addFlight("Hyderabad", "Bangalore", "Air India", 3000, 45,"AI811");
        addFlight("Hyderabad", "Chennai", "Indigo", 2000, 45,"6E809");

        addFlight("Bangalore", "Bhubaneswar", "Spice Jet", 7000, 75,"SG807");
        addFlight("Bangalore", "Mumbai", "Air India", 7000, 90,"AI812");
        addFlight("Bangalore", "Delhi", "Indigo", 5000, 120,"6E810");


        System.out.println("Enter 1 to execute admin functionality \n 2 to execute user functionality \n 3 to exit");
        Scanner sc = new Scanner(System.in);
        int adminOrUser;
        do {
            System.out.println("Enter 1 to execute admin functionality and 2 to execute user functionality. Enter 3 to exit.");
            adminOrUser = sc.nextInt();
            sc.nextLine(); // Consume newline

            switch (adminOrUser) {
                case 1:
                    handleAdminFunctionality();
                    break;

                case 2:
                    handleUserFunctionality();
                    break;

                case 3:
                    System.out.println("Exiting application.");
                    break;

                default:
                    System.out.println("Invalid option! Please enter 1 for admin or 2 for user functionality, or 3 to exit.");
            }
        } while (adminOrUser != 3);
    }

    // Admin functionality for adding or removing flights
    private static void handleAdminFunctionality() {
        Scanner sc = new Scanner(System.in);
        int ch;

        do {
            System.out.println("Admin Menu: \n1. Add flight\n2. Remove flight\n3. Return to main menu");
            ch = sc.nextInt();
            sc.nextLine(); // Consume newline

            String src, dest, airline, flightNo;
            int cost, time;

            switch (ch) {
                case 1:
                    System.out.println("Enter source city:");
                    src = sc.nextLine();
                    System.out.println("Enter destination city:");
                    dest = sc.nextLine();
                    if (badWeatherCities.contains(src)) {
                        System.out.println("Sorry no flights available due to bad weather. Details of the weather are as follows: ");
                        System.out.println(WeatherService.getWeatherForCity(src));
                        return;
                    }

                    if (badWeatherCities.contains(dest)) {
                        System.out.println("Sorry no flights available due to bad weather. Details of the weather are as follows: ");
                        System.out.println(WeatherService.getWeatherForCity(dest));
                        return;
                    }

                    System.out.println("Enter airline:");
                    airline = sc.nextLine();
                    System.out.println("Enter cost:");
                    cost = sc.nextInt();
                    System.out.println("Enter time:");
                    time = sc.nextInt();
                    sc.nextLine(); // Consume newline
                    System.out.println("Enter flight number:");
                    flightNo = sc.nextLine();

                    addFlight(src, dest, airline, cost, time, flightNo);
                    break;

                case 2:
                    // Gather flight details for removing an existing flight
                    System.out.println("Enter source city:");
                    src = sc.nextLine();
                    System.out.println("Enter destination city:");
                    dest = sc.nextLine();
                    System.out.println("Enter airline:");
                    airline = sc.nextLine();
                    System.out.println("Enter cost:");
                    cost = sc.nextInt();
                    System.out.println("Enter time:");
                    time = sc.nextInt();
                    sc.nextLine(); // Consume newline
                    System.out.println("Enter flight number:");
                    flightNo = sc.nextLine();
                    removeFlight(src, dest, airline, cost, time, flightNo);
                    break;
                case 3:
                    System.out.println("Returning to main menu.");
                    break;

                default:
                    System.out.println("Invalid option! Please choose a valid option from the Admin Menu.");
            }
        } while (ch != 3);
    }

    // User functionality for finding flights and booking
    private static void handleUserFunctionality() {
        Scanner sc = new Scanner(System.in);
        int choice;
        do {
            System.out.println("User Menu: \n1. Minimum cost\n2. All possible routes sorted by cost\n3. Minimum time\n4. All possible routes sorted by time\n5. Flights with/without layovers\n6. Return to main menu");
            choice = sc.nextInt();
            sc.nextLine(); // Consume newline

            if (choice == 6) {
                System.out.println("Returning to main menu.");
                break;
            }

            System.out.println("Enter source city: ");
            String source = sc.nextLine();
            if (badWeatherCities.contains(source)) {
                System.out.println("Sorry no flights available due to bad weather. Details of the weather are as follows: ");
                System.out.println(WeatherService.getWeatherForCity(source));
                return;
            }
            System.out.println("Enter destination city: ");
            String destination = sc.nextLine();
            if (badWeatherCities.contains(destination)) {
                System.out.println("Sorry no flights available due to bad weather. Details of the weather are as follows: ");
                System.out.println(WeatherService.getWeatherForCity(destination));
                return;
            }

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
                    findAllFlightsSortedByTime(source, destination); // Implement this method
                    break;

                case 5:
                    FlightsWithoutLayovers(source, destination); // Implement this method
                    break;

                default:
                    System.out.println("Invalid option!");
                    continue;
            }

            System.out.println("Do you want to proceed to booking now? Enter Y for yes or N for no");
            String proceedToBooking = sc.nextLine();

            if (proceedToBooking.equalsIgnoreCase("Y")) {
                User.booking(); // Ensure the booking functionality is implemented in User class
            } else if (proceedToBooking.equalsIgnoreCase("N")) {
                System.out.println("Returning to main menu.");
            } else {
                System.out.println("Invalid entry! Returning to main menu.");
            }

        } while (choice != 6);
    }
}
class User {
    static String name;
    static long mobileNo;
    static int age;
    static String emailId;

    public static void user_details(int noOfPassengers) {//taking details of customer
        while (noOfPassengers != 0) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter the name of customer");
            name = sc.nextLine();
            System.out.println("Enter the age");
            age = sc.nextInt();
            System.out.println("Enter the mobileNo");
            mobileNo = sc.nextLong();
            System.out.println("Enter the email Id");
            emailId = sc.next();
            if (emailId.contains("@")) {
                System.out.println("Valid E mail Id");
            } else {
                System.out.println("Invalid Email Id");
            }
            //USE OF WHILE LOOP TILL THE USER ENTERS VALID EMAIL
            while (!emailId.contains("@")) {
                System.out.println("Enter the valid Email (It must contain @)");
                emailId = sc.next();
            }
            if (emailId.contains("@")) {
                System.out.println("Thank You!");
            }
            noOfPassengers--;
        }
        System.out.println("Your tickets have been booked successfully!" +
                " We hope you have a happy and safe journey");
    }

    public static void booking() {
        int noOfPassengers = 0;
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number of tickets you want to book.");
        int noOfTickets = sc.nextInt();
        sc.nextLine();

        while (noOfTickets != 0) {
            System.out.println("For Booking, please enter the following details: ");
            System.out.println("Enter the city FROM which you want to start your journey. ");
            String source = sc.nextLine();
            System.out.println("Enter the city TO which you want to end your journey. ");
            String destination = sc.nextLine();
            System.out.println("Enter the desired AIRLINE. ");
            String airline = sc.nextLine();
            if (FlightLogisticsSystem.flightsGraph.containsKey(source)) {
                boolean flightFound = false;
                for (Flight flight : FlightLogisticsSystem.flightsGraph.get(source)) {
                    if (flight.destination.equals(destination) && flight.airline.equals(airline)) {
                        System.out.println("Flight from source to destination of desired airline is available.");
                        flightFound = true;
                        break;
                    }
                }
                if (!flightFound) {
                    System.out.println("Flight from source to destination of desired airline is not available.");
                }
            } else {
                System.out.println("Source city not found in the flight graph.");
            }

            char seatMatrix[][] = new char[16][10];
            for (char[] row : seatMatrix) {
                Arrays.fill(row, 'A');
            }

            for (int i = 0; i < seatMatrix.length; i++) {
                seatMatrix[i][2] = ' ';
                seatMatrix[i][6] = ' ';
            }
            for (int i = 0; i < seatMatrix[0].length; i++) {
                seatMatrix[7][i] = '-';  // Separator row for Business and Economy classes
            }

            System.out.println("Towards the top is Business Class. Below the separator row is Economy Class.");
            System.out.println("0 1 2 3 4 5 6 7 8 ");  // Column indices
            for (int i=0; i < seatMatrix.length; i++) {
                System.out.print(i + " ");
                for (int j=0; j<seatMatrix[0].length; j++) {
                    System.out.print(seatMatrix[i][j] + " ");
                }
                System.out.println();
            }

            System.out.println("Enter the number of passengers. ");

            int passengers = sc.nextInt();
            noOfPassengers = passengers;

            while (passengers != 0) {
                System.out.println("Enter the row number: ");
                int rowNo = sc.nextInt();

                System.out.println("Enter the column number: ");
                int colNo = sc.nextInt();
                if (rowNo == 2 || rowNo == 6 || colNo == 7) {
                    System.out.println("Invalid no. Program will terminate. ");
                    System.exit(0);
                }
                if (seatMatrix[rowNo][colNo] != 'B') {
                    seatMatrix[rowNo][colNo] = 'B';
                    passengers--;
                    System.out.println("Now seat matrix is: ");
                    for (int i = 0; i < seatMatrix.length; i++) {
                        for (int j = 0; j < seatMatrix[0].length; j++) {
                            System.out.print(seatMatrix[i][j] + " ");
                        }
                        System.out.println();
                    }
                }
                else {
                    System.out.println("That seat is already booked!");
                }
            }
            System.out.println("Seats booked!");
            noOfTickets--;
            sc.nextLine();  // Clear newline after number input
        }
        User.user_details(noOfPassengers);
    }
}

/* "D:\Program Files\Java\jdk-21\bin\java.exe" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.1\lib\idea_rt.jar=59236:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.1\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\Admin\IdeaProjects\weatherTrying\target\classes;C:\Users\Admin\.m2\repository\com\google\code\gson\gson\2.8.8\gson-2.8.8.jar FlightLogisticsSystem
Welcome to AirEase - Your Ultimate Flight Logistics Companion!
Effortlessly manage flights, track routes, and ensure a smooth journey for passengers and cargo alike. From real-time updates to optimized routing, AirEase is here to make airline management simpler and more efficient.
Fasten your seatbelt, and let’s get started!
We appreciate your patience while we are fetching real-time weather data to ensure your journey is safe and free of turbulent weather.
Enter 1 to execute admin functionality
 2 to execute user functionality
 3 to exit
Enter 1 to execute admin functionality and 2 to execute user functionality. Enter 3 to exit.
1
Admin Menu:
1. Add flight
2. Remove flight
3. Return to main menu
1
Enter source city:
Pune
Enter destination city:
Hyderabad
Enter airline:
Air India
Enter cost:
10000
Enter time:
120
Enter flight number:
6C309
Admin Menu:
1. Add flight
2. Remove flight
3. Return to main menu
3
Returning to main menu.
Enter 1 to execute admin functionality and 2 to execute user functionality. Enter 3 to exit.
2
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
1
Enter source city:
Delhi
Sorry no flights available due to bad weather. Details of the weather are as follows:
Temperature: 23.1°C, Condition: haze, Humidity: 73%, Wind Speed: 0.0 km/h, Visibility: 1000 meters, Status: Low visibility detected. Flights may be rescheduled. Conditions indicate potential delays or rescheduling.
Enter 1 to execute admin functionality and 2 to execute user functionality. Enter 3 to exit.
2
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
1
Enter source city:
Pune
Enter destination city:
Chennai
Minimum cost to Chennai is 7000 with time 165 minutes.
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Do you want to proceed to booking now? Enter Y for yes or N for no
N
Returning to main menu.
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
2
Enter source city:
Pune
Enter destination city:
Mumbai
All possible routes from Pune to Mumbai sorted by cost:
Total cost: 2000, Total time: 20 minutes
Flights to take:
Airline: Indigo, From: Pune, To: Mumbai, Cost: 2000, Time: 20 minutes.

Total cost: 10000, Total time: 210 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 13000, Total time: 210 minutes
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 15000, Total time: 255 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 15000, Total time: 255 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 16000, Total time: 345 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 19000, Total time: 345 minutes
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 20000, Total time: 255 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 21000, Total time: 390 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 21000, Total time: 390 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 26000, Total time: 495 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 26000, Total time: 495 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 26000, Total time: 390 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 31000, Total time: 495 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.

Total cost: 32000, Total time: 630 minutes
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 32000, Total time: 630 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Total cost: 37000, Total time: 630 minutes
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Kolkata, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.

Do you want to proceed to booking now? Enter Y for yes or N for no
N
Returning to main menu.
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
3
Enter source city:
Pune
Enter destination city:
Kolkata
Minimum time to Kolkata is 285 minutes with cost 13000
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.
Do you want to proceed to booking now? Enter Y for yes or N for no
N
Returning to main menu.
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
4
Enter source city:
Pune
Enter destination city:
Kolkata
All possible routes from Pune to Kolkata sorted by time:
Total time: 285 minutes, Total cost: 13000
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 285 minutes, Total cost: 18000
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 285 minutes, Total cost: 13000
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 285 minutes, Total cost: 15000
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 285 minutes, Total cost: 18000
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 330 minutes, Total cost: 20000
Flights to take:
Airline: Spice Jet, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 330 minutes, Total cost: 20000
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 330 minutes, Total cost: 25000
Flights to take:
Airline: Air India, From: Pune, To: Hyderabad, Cost: 10000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 425 minutes, Total cost: 19000
Flights to take:
Airline: Indigo, From: Pune, To: Mumbai, Cost: 2000, Time: 20 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 425 minutes, Total cost: 18000
Flights to take:
Airline: Indigo, From: Pune, To: Mumbai, Cost: 2000, Time: 20 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 470 minutes, Total cost: 26000
Flights to take:
Airline: Indigo, From: Pune, To: Mumbai, Cost: 2000, Time: 20 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 470 minutes, Total cost: 25000
Flights to take:
Airline: Indigo, From: Pune, To: Mumbai, Cost: 2000, Time: 20 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Air India, From: Hyderabad, To: Bangalore, Cost: 3000, Time: 45 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Indigo, From: Bhubaneswar, To: Kolkata, Cost: 5000, Time: 90 minutes.

Total time: 615 minutes, Total cost: 30000
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 615 minutes, Total cost: 29000
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 615 minutes, Total cost: 27000
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 615 minutes, Total cost: 26000
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Air India, From: Bangalore, To: Mumbai, Cost: 7000, Time: 90 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 750 minutes, Total cost: 36000
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 750 minutes, Total cost: 35000
Flights to take:
Airline: Air India, From: Pune, To: Bangalore, Cost: 6000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 750 minutes, Total cost: 33000
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.
Airline: Air India, From: Mumbai, To: Bhopal, Cost: 4000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Total time: 750 minutes, Total cost: 32000
Flights to take:
Airline: Spice Jet, From: Pune, To: Bangalore, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bangalore, To: Bhubaneswar, Cost: 7000, Time: 75 minutes.
Airline: Air India, From: Bhubaneswar, To: Mumbai, Cost: 6000, Time: 150 minutes.
Airline: Indigo, From: Mumbai, To: Bhopal, Cost: 3000, Time: 120 minutes.
Airline: Spice Jet, From: Bhopal, To: Hyderabad, Cost: 5000, Time: 120 minutes.
Airline: Indigo, From: Hyderabad, To: Chennai, Cost: 2000, Time: 45 minutes.
Airline: Air India, From: Chennai, To: Kolkata, Cost: 6000, Time: 120 minutes.

Do you want to proceed to booking now? Enter Y for yes or N for no
N
Returning to main menu.
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
5
Enter source city:
Pune
Enter destination city:
Kolkata
No direct flight between Pune and Kolkata
Do you want to proceed to booking now? Enter Y for yes or N for no
N
Returning to main menu.
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
5
Enter source city:
Hyderabad
Enter destination city:
Bangalore
Direct flight found: Airline: Air India, Cost: 3000, Time: 45 minutes.
Do you want to proceed to booking now? Enter Y for yes or N for no
Y
Enter the number of tickets you want to book.
1
For Booking, please enter the following details:
Enter the city FROM which you want to start your journey.
Hyderabad
Enter the city TO which you want to end your journey.
Bangalore
Enter the desired AIRLINE.
Air India
Flight from source to destination of desired airline is available.
Towards the top is Business Class. Below the separator row is Economy Class.
0 1 2 3 4 5 6 7 8
0 A A   A A A   A A A
1 A A   A A A   A A A
2 A A   A A A   A A A
3 A A   A A A   A A A
4 A A   A A A   A A A
5 A A   A A A   A A A
6 A A   A A A   A A A
7 - - - - - - - - - -
8 A A   A A A   A A A
9 A A   A A A   A A A
10 A A   A A A   A A A
11 A A   A A A   A A A
12 A A   A A A   A A A
13 A A   A A A   A A A
14 A A   A A A   A A A
15 A A   A A A   A A A
Enter the number of passengers.
2
Enter the row number:
0
Enter the column number:
0
Now seat matrix is:
B A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
- - - - - - - - - -
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
Enter the row number:
0
Enter the column number:
1
Now seat matrix is:
B B   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
- - - - - - - - - -
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
A A   A A A   A A A
Seats booked!
Enter the name of customer
Sanika
Enter the age
19
Enter the mobileNo
998000000
Enter the email Id
sanika
Invalid Email Id
Enter the valid Email (It must contain @)
sanika@gmail.com
Thank You!
Enter the name of customer
Shrishti
Enter the age
20
Enter the mobileNo
9898989898
Enter the email Id
shrishti@gmail.com
Valid E mail Id
Thank You!
Your tickets have been booked successfully! We hope you have a happy and safe journey
User Menu:
1. Minimum cost
2. All possible routes sorted by cost
3. Minimum time
4. All possible routes sorted by time
5. Flights with/without layovers
6. Return to main menu
6
Returning to main menu.
Enter 1 to execute admin functionality and 2 to execute user functionality. Enter 3 to exit.
1
Admin Menu:
1. Add flight
2. Remove flight
3. Return to main menu
2
Enter source city:
Pune
Enter destination city:
Mumbai
Enter airline:
Indigo
Enter cost:
2000
Enter time:
20
Enter flight number:
6E890
Flight removed successfully!
Admin Menu:
1. Add flight
2. Remove flight
3. Return to main menu

Process finished with exit code 130

 */
