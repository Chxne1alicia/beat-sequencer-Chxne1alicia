package com.beatsequencer;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class TUI {
    private static final String BASE_URL = "http://localhost:7001";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        Screen screen = factory.createScreen();
        screen.startScreen();

        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

        showMainMenu(gui);

        screen.stopScreen();
    }

    static void showMainMenu(MultiWindowTextGUI gui) throws Exception {
        while (true) {
            String[] options = {"List Patterns", "Create Pattern", "Delete Pattern", "Exit"};
            ListSelectDialog<String> dialog = new ListSelectDialogBuilder<String>()
                .setTitle("Beat Sequencer")
                .setDescription("Select an action:")
                .addListItems(options)
                .build();

            String choice = dialog.showDialog(gui);
            if (choice == null || choice.equals("Exit")) break;

            switch (choice) {
                case "List Patterns" -> showPatterns(gui);
                case "Create Pattern" -> createPattern(gui);
                case "Delete Pattern" -> deletePattern(gui);
            }
        }
    }

    static void showPatterns(MultiWindowTextGUI gui) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        MessageDialog.showMessageDialog(gui, "Patterns", res.body());
    }

    static void createPattern(MultiWindowTextGUI gui) throws Exception {
        String name = new TextInputDialogBuilder()
            .setTitle("Create Pattern")
            .setDescription("Enter pattern name:")
            .build()
            .showDialog(gui);
        if (name == null) return;

        String tempo = new TextInputDialogBuilder()
            .setTitle("Create Pattern")
            .setDescription("Enter tempo (BPM):")
            .setInitialContent("120")
            .build()
            .showDialog(gui);
        if (tempo == null) return;

        String json = "{\"name\":\"" + name + "\",\"tempo\":" + tempo + "}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        MessageDialog.showMessageDialog(gui, "Created!", res.body());
    }

    static void deletePattern(MultiWindowTextGUI gui) throws Exception {
        String id = new TextInputDialogBuilder()
            .setTitle("Delete Pattern")
            .setDescription("Enter pattern ID to delete:")
            .build()
            .showDialog(gui);
        if (id == null) return;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/patterns/" + id))
            .DELETE().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        String msg = res.statusCode() == 204 ? "Deleted!" : "Pattern not found.";
        MessageDialog.showMessageDialog(gui, "Result", msg);
    }
}
