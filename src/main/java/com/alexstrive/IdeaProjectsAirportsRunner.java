package com.alexstrive;

import com.alexstrive.domain.Airline;
import com.alexstrive.domain.Airport;
import com.alexstrive.domain.Ticket;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.GraphWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class IdeaProjectsAirportsRunner {

    private static final String FILENAME = "tickets.json";

    private static final Logger logger = LoggerFactory.getLogger(IdeaProjectsAirportsRunner.class);

    public static void main(String... args) {
        try {
            logger.info("App has been started");

            var graph = initializeGraph();
            var origin = new Airport("VVO");
            var destination = new Airport("TLV");

            var dijkstra = new DijkstraShortestPath(graph);
            var shortestPath = dijkstra.getPath(origin, destination);
            var weight = shortestPath.getWeight();
            var shortestPathValue = Double.valueOf(weight).longValue();
            var shortestPathDuration = Duration.ofMinutes(shortestPathValue);
            logger.info(String.format("[1] Shortest airline duration between VVO and TLV is %s hours %s minutes",
                    shortestPathDuration.toHoursPart(),
                    shortestPathDuration.toMinutesPart()));
            logger.info(String.format("    Shortest path is (in minutes) %s", String.join(" -> ", shortestPath.getVertexList().toString())));


            logger.info("");

            logger.info("    All possible paths from VVO to TLV: ");

            var allPaths = new AllDirectedPaths(graph).getAllPaths(origin, destination, false, 1_000);


            var prices = new ArrayList<Integer>();
            for (Object pathObj : allPaths) {
                var path = (GraphWalk<Airport, Airline>) pathObj;
                var totalPathSequence = airportsToSequence(path.getVertexList());
                var totalCostPerPath = airlinesToSum(path.getEdgeList());
                prices.add(totalCostPerPath);
                logger.info(String.format("    %s (price: %s RUB)", totalPathSequence, totalCostPerPath));
            }
            prices.sort(Comparator.comparingInt(o -> o));
            var medianIndex = prices.size() % 2 == 0 ? prices.size() / 2 : prices.size() / 2 + 1;
            var average = prices.stream().mapToInt(Integer::valueOf).sum() / prices.size();
            var median = prices.get(medianIndex);
            logger.info(String.format("Average is %s, Median is %s", average, median));
            logger.info(String.format("[2] Difference between average and median is %s ", Math.abs(average - median)));


        } catch (StreamReadException e) {
            throw new RuntimeException(e);
        } catch (DatabindException e) {
            logger.error("Unable to cast ticket lists file in executing directory.");
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error(String.format("Unable to read %s file in executing directory. Put %s in executing directory.", FILENAME, FILENAME));
            throw new RuntimeException(e);
        } finally {
            logger.info("App execution has been completed");
        }
    }

    public static Graph<Airport, Airline> initializeGraph() throws IOException {
        var objectMapper = new ObjectMapper();
        logger.info("ObjectMapper has been created");
        objectMapper.findAndRegisterModules();
        logger.info("Extra Jackson modules are registered");

        var inputFile = new File(FILENAME);
        var jsonNode = objectMapper.readTree(inputFile);
        logger.info(String.format("File %s is deserialized successfully into general JSON node", FILENAME));

        var ticketsNode = jsonNode.get("tickets");
        if (ticketsNode == null) {
            throw new RuntimeException("Unable to find `tickets` member in root json object");
        }

        List<Ticket> tickets = objectMapper.readValue(jsonNode.get("tickets").traverse(), new TypeReference<List<Ticket>>() {
        });

        logger.info("Building Airport graph...");
        Graph<Airport, Airline> graph = new DirectedWeightedMultigraph<>(Airline.class);

        for (var ticket : tickets) {
            var originAirport = new Airport(ticket.getOrigin());
            graph.addVertex(originAirport);
            var destinationAirport = new Airport(ticket.getDestination());
            graph.addVertex(destinationAirport);
            var duration = Duration.between(ticket.getDepartureDateTime(), ticket.getArrivalDateTime());
            var price = ticket.getPrice();
            var airline = new Airline(duration, price);
            graph.addEdge(originAirport, destinationAirport, airline);
            graph.setEdgeWeight(airline, airline.duration().toMinutes());
        }

        logger.info("Airport graph is complete");

        return graph;
    }

    public static String airportsToSequence(List<Airport> airports) {
        return String.join(" -> ", airports.stream().map(Airport::code).toList());
    }

    public static int airlinesToSum(List<Airline> airlines) {
        return airlines.stream().map(Airline::price).mapToInt(Integer::valueOf).sum();
    }
}
