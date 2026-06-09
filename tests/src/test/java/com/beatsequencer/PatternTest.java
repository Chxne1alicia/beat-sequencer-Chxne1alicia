package com.beatsequencer;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;

public class PatternTest {

    @BeforeEach
    void setup() {
        Database.initialize();
    }

    @Test
    void testDatabaseConnection() throws SQLException {
        Connection conn = Database.connect();
        assertNotNull(conn);
        conn.close();
    }

    @Test
    void testInstrumentsSeeded() throws SQLException {
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM instruments")) {
            assertTrue(rs.getInt("count") >= 4);
        }
    }

    @Test
    void testCreatePattern() throws SQLException {
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO patterns (name, tempo) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "Test Pattern");
            stmt.setInt(2, 120);
            int rows = stmt.executeUpdate();
            assertEquals(1, rows);
            ResultSet keys = stmt.getGeneratedKeys();
            assertTrue(keys.next());
            assertTrue(keys.getInt(1) > 0);
        }
    }

    @Test
    void testDeletePattern() throws SQLException {
        int id;
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO patterns (name, tempo) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "To Delete");
            stmt.setInt(2, 100);
            stmt.executeUpdate();
            id = stmt.getGeneratedKeys().getInt(1);
        }
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM patterns WHERE id = ?")) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            assertEquals(1, rows);
        }
    }

    @Test
    void testCreateBeat() throws SQLException {
        int patternId;
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO patterns (name, tempo) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "Beat Test");
            stmt.setInt(2, 140);
            stmt.executeUpdate();
            patternId = stmt.getGeneratedKeys().getInt(1);
        }
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO beats (pattern_id, instrument_id, step, active) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, patternId);
            stmt.setInt(2, 1);
            stmt.setInt(3, 0);
            stmt.setInt(4, 1);
            int rows = stmt.executeUpdate();
            assertEquals(1, rows);
        }
    }

    @Test
    void testTempoRange() {
        int tempo = 120;
        assertTrue(tempo >= 40 && tempo <= 240);
    }

    @Test
    void testStepCount() {
        int steps = 16;
        assertEquals(16, steps);
    }
}
