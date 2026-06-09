package com.beatsequencer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static final String DB_URL = "jdbc:sqlite:server/beatsequencer.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS instruments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    midi_note INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS patterns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    tempo INTEGER NOT NULL DEFAULT 120
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS beats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pattern_id INTEGER NOT NULL,
                    instrument_id INTEGER NOT NULL,
                    step INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (pattern_id) REFERENCES patterns(id),
                    FOREIGN KEY (instrument_id) REFERENCES instruments(id)
                )
            """);
            // seed default instruments
            stmt.execute("""
                INSERT OR IGNORE INTO instruments (id, name, midi_note) VALUES
                (1, 'Kick', 36),
                (2, 'Snare', 38),
                (3, 'Hi-Hat', 42),
                (4, 'Tom', 45)
            """);
            log.info("Database initialized successfully");
        } catch (SQLException e) {
            log.error("Failed to initialize database: {}", e.getMessage());
        }
    }
}
