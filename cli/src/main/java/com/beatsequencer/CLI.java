package com.beatsequencer;

import java.net.URI;
import java.net.http.*;
import java.util.Scanner;

public class CLI {
    private static final String BASE_URL = "http://localhost:7001";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Beat Sequencer CLI ===");
        System.out.println("Commands: list, create, delete, get, quit");

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "list" -> listPatterns();
                case "create" -> {
                    System.out.print("Pattern name: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Tempo (BPM): ");
                    int tempo = Integer.parseInt(scanner.nextLine().trim());
                    createPattern(name, tempo);
                }
                case "delete" -> {
                    System.out.print("Pattern ID to delete: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    deletePattern(id);
                }
                case "get" -> {
                    System.out.print("Pattern ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    getPattern(id);
                }
                case "quit" -> {
                    System.out.println("Bye!");
                    return;
                }
                default -> System.out.println("Unknown command. Try: list, create, delete, get, quit");
            }
        }
    }

    static void listPatterns() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Patterns: " + res.body());
    }

    static void createPattern(String name, int tempo) throws Exception {
        String json = "{\"name\":\"" + name + "\",\"tempo\":" + tempo + "}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Created: " + res.body());
    }

    static void deletePattern(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + id))
            .DELETE().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println(res.statusCode() == 204 ? "Deleted!" : "Not found.");
    }

    static void getPattern(int id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + id))
            .GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Pattern: " + res.body());
    }
}
