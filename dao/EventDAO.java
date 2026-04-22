package dao;

import config.DatabaseConfig;
import model.Event;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventDAO {
    
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();
    
    public int create(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, event_date, location, organizer_id, max_attendees) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(event.getEventDate()));
            stmt.setString(4, event.getLocation());
            stmt.setInt(5, event.getOrganizerId());
            if (event.getMaxAttendees() != null) {
                stmt.setInt(6, event.getMaxAttendees());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
    
    public Optional<Event> findById(int eventId) throws SQLException {
        String sql = "SELECT * FROM events WHERE event_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvent(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public List<Event> findAll() throws SQLException {
        String sql = "SELECT * FROM events ORDER BY event_date";
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> findUpcoming() throws SQLException {
        String sql = "SELECT * FROM events WHERE event_date > NOW() ORDER BY event_date";
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> findByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT * FROM events WHERE event_date BETWEEN ? AND ? ORDER BY event_date";
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        }
        return events;
    }
    
    public List<Event> findByOrganizer(int organizerId) throws SQLException {
        String sql = "SELECT * FROM events WHERE organizer_id = ? ORDER BY event_date DESC";
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, organizerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        }
        return events;
    }
    
    public boolean update(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, event_date = ?, " +
                     "location = ?, max_attendees = ? WHERE event_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(event.getEventDate()));
            stmt.setString(4, event.getLocation());
            if (event.getMaxAttendees() != null) {
                stmt.setInt(5, event.getMaxAttendees());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setInt(6, event.getEventId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean delete(int eventId) throws SQLException {
        String sql = "DELETE FROM events WHERE event_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, eventId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getInt("event_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
        event.setLocation(rs.getString("location"));
        event.setOrganizerId(rs.getInt("organizer_id"));
        int maxAttendees = rs.getInt("max_attendees");
        event.setMaxAttendees(rs.wasNull() ? null : maxAttendees);
        event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return event;
    }
}
