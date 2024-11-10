import java.io.BufferedReader;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


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

        // Safety check criteria (you can adjust the thresholds as necessary)
        boolean isSafe = true;
        StringBuilder message = new StringBuilder();

        if (windSpeed > 50) { // Wind speed > 50 km/h is considered risky for flights
            isSafe = false;
            message.append("High wind speeds detected. Flights may be rescheduled. ");
        }

        if (visibility < 2000) { // Visibility less than 2 km could be problematic
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

//import static org.example.WeatherService.getWeatherForCity;

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

public class FlightLogisticsSystem {
    static Map<String, List<Flight>> flightsGraph = new HashMap<>();
    static List<String> badWeatherCities = new ArrayList<>();
    public static void addFlight(String source, String destination, String airline, int cost, int time, String flightNo) {
        if(WeatherService.getWeatherForCity(source).contains("safe") && WeatherService.getWeatherForCity(destination).contains("safe")) {
            flightsGraph.putIfAbsent(source, new ArrayList<>());
            flightsGraph.get(source).add(new Flight(airline, destination, cost, time,flightNo));
        }else{
//            System.out.println("for "+source+ ": "+ getWeatherForCity(source));
//            System.out.println("for "+destination+ ": "+ getWeatherForCity(destination));
            if(WeatherService.getWeatherForCity(source).contains("rescheduled") && !badWeatherCities.contains(source)) {
                badWeatherCities.add(source);
            }
            if(WeatherService.getWeatherForCity(destination).contains("rescheduled") && !badWeatherCities.contains(destination)){
                badWeatherCities.add(destination);
            }
        }
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
        List<Node> flightsList = new ArrayList<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.cost));
        Map<String, Integer> minCost = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minCost.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int costSoFar = currentNode.cost;

            if (currentCity.equals(dest)) {
                System.out.println("Cost to " + dest + " from " + src + " is " + costSoFar + " with time " + currentNode.time + " minutes.");
                System.out.println("Flights to take:");
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + src + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                }
                flightsList.add(currentNode);
                System.out.println();
                continue;
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

        if (flightsList.isEmpty()) {
            System.out.println("No route available from " + src + " to " + dest);
        }
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
                    System.out.println("Airline: " + flight.airline + ", From: " + src + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
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
        List<Node> flightsList = new ArrayList<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.time));
        Map<String, Integer> minTime = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minTime.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;
            int timeSoFar = currentNode.time;

            if (currentCity.equals(dest)) {
                System.out.println("Time to " + dest + " from " + src + " is " + timeSoFar + " with cost " + currentNode.cost + " minutes.");
                System.out.println("Flights to take:");
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + src + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                }
                flightsList.add(currentNode);
                System.out.println();
                continue;
            }

            for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
                int newTime = timeSoFar + flight.time;
                if (newTime < minTime.getOrDefault(flight.destination, Integer.MAX_VALUE)) {
                    minTime.put(flight.destination, newTime);
                    List<Flight> newPath = new ArrayList<>(currentNode.path);
                    newPath.add(flight);
                    pq.add(new Node(flight.destination, newTime, currentNode.time + flight.time, newPath));
                }
            }
        }

        if (flightsList.isEmpty()) {
            System.out.println("No route available from " + src + " to " + dest);
        }
    }
    public static void WithHaltsOrWithout(String src, String dest) {
        System.out.println("Choose option:\n1. Direct Flights Only\n2. Flights With Layovers");

        Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();

        switch (choice) {
            case 1:
                // Direct flights only
                boolean directFound = false;
                for (Flight flight : flightsGraph.getOrDefault(src, new ArrayList<>())) {
                    if (flight.destination.equals(dest)) {
                        System.out.println("Direct flight found: Airline: " + flight.airline + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                        directFound = true;
                    }
                }
                if (!directFound) {
                    System.out.println("No direct flight available from " + src + " to " + dest);
                }
                break;

            case 2:
                // Flights with layovers only
                System.out.println("Flights with layovers:");
                findAllFlightsWithLayoversOnly(src, dest);
                break;

            default:
                System.out.println("Invalid option!");
        }
    }

    public static void findAllFlightsWithLayoversOnly(String src, String dest) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.cost));
        List<Node> flightsList = new ArrayList<>();
        Map<String, Integer> minCost = new HashMap<>();
        pq.add(new Node(src, 0, 0, new ArrayList<>()));
        minCost.put(src, 0);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String currentCity = currentNode.city;

            if (currentCity.equals(dest) && currentNode.path.size() > 1) {
                System.out.println("Cost to " + dest + " from " + src + " is " + currentNode.cost + " with time " + currentNode.time + " minutes.");
                System.out.println("Flights to take:");
                String startCity = src;
                for (Flight flight : currentNode.path) {
                    System.out.println("Airline: " + flight.airline + ", From: " + startCity + ", To: " + flight.destination + ", Cost: " + flight.cost + ", Time: " + flight.time + " minutes.");
                    startCity = flight.destination;
                }
                System.out.println();
                flightsList.add(currentNode);
                continue;
            }

            for (Flight flight : flightsGraph.getOrDefault(currentCity, new ArrayList<>())) {
                int newCost = currentNode.cost + flight.cost;
                if (newCost < minCost.getOrDefault(flight.destination, Integer.MAX_VALUE)) {
                    minCost.put(flight.destination, newCost);
                    List<Flight> newPath = new ArrayList<>(currentNode.path);
                    newPath.add(flight);
                    pq.add(new Node(flight.destination, newCost, currentNode.time + flight.time, newPath));
                }
            }
        }

        if (flightsList.isEmpty()) {
            System.out.println("No routes with layovers available from " + src + " to " + dest);
        }
    }




    public static void main(String[] args) {
        System.out.println(" We appreciate your patience while we are fetching real-time weather data to ensure your journey safe. ");
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
        addFlight("Pune", "Mumbai", "Indigo", 20000, 20,"6E808");

        addFlight("Hyderabad", "Delhi", "Spice Jet", 7000, 150,"SG806");
        addFlight("Hyderabad", "Bangalore", "Air India", 3000, 45,"AI811");
        addFlight("Hyderabad", "Chennai", "Indigo", 2000, 45,"6E809");

        addFlight("Bangalore", "Bhubaneswar", "Spice Jet", 7000, 75,"SG807");
        addFlight("Bangalore", "Mumbai", "Air India", 7000, 90,"AI812");
        addFlight("Bangalore", "Delhi", "Indigo", 5000, 120,"6E810");

        Scanner sc = new Scanner(System.in);
        System.out.println(badWeatherCities);
        System.out.println("Welcome to AirEase - Your Ultimate Flight Logistics Companion!");
        System.out.println("Effortlessly manage flights, track routes, and ensure a smooth journey for passengers and cargo alike. From real-time updates to optimized routing, AirEase is here to make airline management simpler and more efficient.");
        System.out.println("Fasten your seatbelt, and let’s get started!");
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
        System.out.println("Enter 1 to execute admin functionality and 2 to execute user functionality. ");
        int adminOrUser = sc.nextInt();
        switch (adminOrUser) {
            case 1: // WHAT HERE????


            case 2:
                System.out.println("Choose an option: \n1. Minimum cost\n2. All possible routes sorted by cost\n3. Minimum time\n4. All possible routes sorted by time\n5. Flights with/without layovers");
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
                        WithHaltsOrWithout(source, destination);
                        break;
                    default:
                        System.out.println("Invalid option!");
                }

                // choose src dest airline/ flightno and then book
                // show matrix
                // accept info
                // confitm
                System.out.println("Do you want to proceed to booking now? Enter Y for yes or N for no");
                String proceedToBooking = sc.nextLine();
                if(proceedToBooking.equalsIgnoreCase("Y")){
                    User.booking();
                }
                else if(proceedToBooking.equalsIgnoreCase("N")){
                    return;
                }
                else{
                    System.out.println("Wrong value entered!");
                }

        }

    }
}
class User{
    static String name;
    static long mobileNo;
    static int age;
    static String emailId;
    public static void user_details(){//taking details of customer
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter the first name of customer");
        name = sc.nextLine();
        System.out.println("Enter the age");
        age=sc.nextInt();
        System.out.println("Enter the mobileNo");
        mobileNo=sc.nextLong();
        System.out.println("Enter the email Id");
        emailId=sc.next();
        if(emailId.contains("@")) {
            System.out.println("Valid E mail Id");
        }
        else{
            System.out.println("Invalid Email Id");
        }
        //USE OF WHILE LOOP TILL THE USER ENTERS VALID EMAIL
        while(!emailId.contains("@")){
            System.out.println("Enter the valid E mail (It must contain @)");
            emailId=sc.next();
        }
        if(emailId.contains("@")){
            System.out.println("Thank You!");
        }
    }
    public static void booking(){
        Scanner sc=new Scanner(System.in);
        System.out.println("For Booking, please enter the following details: ");
        System.out.println("Enter the city FROM which you want to start your journey. ");
        String source = sc.nextLine();
        System.out.println("Enter the city TO which you want to end your journey. ");
        String destination = sc.nextLine();
        System.out.println("Enter the desired AIRLINE. ");
        String airline = sc.nextLine();
        if(FlightLogisticsSystem.flightsGraph.containsKey(source)){
            for(Map.Entry<String,List<Flight>> set : FlightLogisticsSystem.flightsGraph.entrySet()){
                if(set.getValue().contains(destination) && set.getValue().contains(airline)){
                    System.out.println("Flight from source to destination of desired airline is available. ");
                }
                else{
                    System.out.println("Flight from source to destination of desired airline is not available. ");
                }
            }
        }
        char seatMatrix[][] = new char[9][15];
        for(char row[] : seatMatrix){
            Arrays.fill(row, 'A'); // A means Available
        }
        for(int i=0; i<seatMatrix.length; i++) {
            seatMatrix[i][2] = ' ';
            seatMatrix[i][6] = ' ';
        }
        for(int i=0; i<seatMatrix[0].length; i++) {
            seatMatrix[7][i] = ' ';
        }
        System.out.println("Towards the top is Business Class. Below the space is Economy Class. ");
        for(int i=0; i<seatMatrix.length;i++){
            for(int j=0; j<seatMatrix[0].length; j++){
                System.out.print(seatMatrix[i][j]+" ");
            }
            System.out.println();
        }
        System.out.println("Enter desired row and column no. A stands for available and B stands for Booked.");

        System.out.println("Enter no of passengers. ");
        int passengers = sc.nextInt();
        while(passengers!= 0) {
            System.out.println("Enter the row no. ");
            int rowNo = sc.nextInt();
            System.out.println("Enter the col no. ");
            int colNo = sc.nextInt();
            if (seatMatrix[rowNo][colNo] != 'B') {
                seatMatrix[rowNo][colNo] = 'B';
                passengers--;
            }
            else{
                System.out.println("That seat is already booked!");
            }
        }
        System.out.println("Seats booked!");
        User.user_details();
    }


}