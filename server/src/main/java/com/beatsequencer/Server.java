package com.beatsequencer;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        Database.initialize();

        Javalin app = Javalin.create().start(7001);
        log.info("Beat Sequencer server started on port 7000");

        // GET all patterns
        app.get("/patterns", ctx -> {
            List<Map<String, Object>> patterns = new ArrayList<>();
            try (Connection conn = Database.connect();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM patterns")) {
                while (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", rs.getInt("id"));
                    p.put("name", rs.getString("name"));
                    p.put("tempo", rs.getInt("tempo"));
                    patterns.add(p);
                }
            }
            log.info("GET /patterns - returned {} patterns", patterns.size());
            ctx.json(patterns);
        });

        // GET single pattern
        app.get("/patterns/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            try (Connection conn = Database.connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM patterns WHERE id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", rs.getInt("id"));
                    p.put("name", rs.getString("name"));
                    p.put("tempo", rs.getInt("tempo"));
                    log.info("GET /patterns/{} - found", id);
                    ctx.json(p);
                } else {
                    log.warn("GET /patterns/{} - not found", id);
                    ctx.status(404).result("Pattern not found");
                }
            }
        });

        // POST create pattern
        app.post("/patterns", ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String name = (String) body.get("name");
            int tempo = body.containsKey("tempo") ? (int) body.get("tempo") : 120;
            try (Connection conn = Database.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO patterns (name, tempo) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setInt(2, tempo);
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                int newId = keys.next() ? keys.getInt(1) : -1;
                log.info("POST /patterns - created pattern '{}' with id {}", name, newId);
                ctx.status(201).json(Map.of("id", newId, "name", name, "tempo", tempo));
            }
        });

        // PUT update pattern
        app.put("/patterns/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String name = (String) body.get("name");
            int tempo = body.containsKey("tempo") ? (int) body.get("tempo") : 120;
            try (Connection conn = Database.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE patterns SET name = ?, tempo = ? WHERE id = ?")) {
                stmt.setString(1, name);
                stmt.setInt(2, tempo);
                stmt.setInt(3, id);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    log.info("PUT /patterns/{} - updated", id);
                    ctx.json(Map.of("id", id, "name", name, "tempo", tempo));
                } else {
                    log.warn("PUT /patterns/{} - not found", id);
                    ctx.status(404).result("Pattern not found");
                }
            }
        });

        // DELETE pattern
        app.delete("/patterns/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            try (Connection conn = Database.connect();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM patterns WHERE id = ?")) {
                stmt.setInt(1, id);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    log.info("DELETE /patterns/{} - deleted", id);
                    ctx.status(204);
                } else {
                    log.warn("DELETE /patterns/{} - not found", id);
                    ctx.status(404).result("Pattern not found");
                }
            }
        });

        // GET beats for a pattern
        app.get("/patterns/{id}/beats", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            List<Map<String, Object>> beats = new ArrayList<>();
            try (Connection conn = Database.connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM beats WHERE pattern_id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", rs.getInt("id"));
                    b.put("pattern_id", rs.getInt("pattern_id"));
                    b.put("instrument_id", rs.getInt("instrument_id"));
                    b.put("step", rs.getInt("step"));
                    b.put("active", rs.getInt("active") == 1);
                    beats.add(b);
                }
            }
            log.info("GET /patterns/{}/beats - returned {} beats", id, beats.size());
            ctx.json(beats);
        });

        // PUT update beats for a pattern
        app.put("/patterns/{id}/beats", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            List<Map<String, Object>> beats = ctx.bodyAsClass(List.class);
            try (Connection conn = Database.connect()) {
                conn.setAutoCommit(false);
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO beats (pattern_id, instrument_id, step, active) VALUES (?, ?, ?, ?)");
                for (Map<String, Object> beat : beats) {
                    stmt.setInt(1, id);
                    stmt.setInt(2, (int) beat.get("instrument_id"));
                    stmt.setInt(3, (int) beat.get("step"));
                    stmt.setInt(4, (boolean) beat.get("active") ? 1 : 0);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                log.info("PUT /patterns/{}/beats - updated {} beats", id, beats.size());
                ctx.status(200).result("Beats updated");
            }
        });
    }
}
