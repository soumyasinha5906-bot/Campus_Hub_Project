package dao;

import config.DatabaseConfig;
import model.Announcement;
import model.Announcement.TargetAudience;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnnouncementDAO {

    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();

    // ------------------------------------------------------------------ create

    public int create(Announcement announcement) throws SQLException {
        String sql = "INSERT INTO announcements (title, content, author_id, target_audience, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getContent());
            stmt.setInt(3, announcement.getAuthorId());
            stmt.setString(4, announcement.getTargetAudience().name());

            if (announcement.getExpiresAt() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(announcement.getExpiresAt()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
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

    // ----------------------------------------------------------------- find by id

    public Optional<Announcement> findById(int announcementId) throws SQLException {
        String sql = "SELECT * FROM announcements WHERE announcement_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, announcementId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAnnouncement(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ----------------------------------------------------------------- find all

    public List<Announcement> findAll() throws SQLException {
        String sql = "SELECT * FROM announcements ORDER BY published_at DESC";
        List<Announcement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                announcements.add(mapResultSetToAnnouncement(rs));
            }
        }
        return announcements;
    }

    // ----------------------------------------------------------------- find active
    // Uses the composite index (target_audience, published_at) + idx_expires.
    // Returns announcements that have not yet expired, for a given audience.

    public List<Announcement> findActiveByAudience(TargetAudience audience) throws SQLException {
        String sql = "SELECT * FROM announcements " +
                     "WHERE (target_audience = ? OR target_audience = 'ALL') " +
                     "  AND (expires_at IS NULL OR expires_at > NOW()) " +
                     "ORDER BY published_at DESC";

        List<Announcement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, audience.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    announcements.add(mapResultSetToAnnouncement(rs));
                }
            }
        }
        return announcements;
    }

    // ----------------------------------------------------------------- find all active (any audience)

    public List<Announcement> findAllActive() throws SQLException {
        String sql = "SELECT * FROM announcements " +
                     "WHERE expires_at IS NULL OR expires_at > NOW() " +
                     "ORDER BY published_at DESC";

        List<Announcement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                announcements.add(mapResultSetToAnnouncement(rs));
            }
        }
        return announcements;
    }

    // ----------------------------------------------------------------- find by author

    public List<Announcement> findByAuthor(int authorId) throws SQLException {
        String sql = "SELECT * FROM announcements WHERE author_id = ? ORDER BY published_at DESC";
        List<Announcement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, authorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    announcements.add(mapResultSetToAnnouncement(rs));
                }
            }
        }
        return announcements;
    }

    // ----------------------------------------------------------------- find expired

    public List<Announcement> findExpired() throws SQLException {
        String sql = "SELECT * FROM announcements WHERE expires_at IS NOT NULL AND expires_at <= NOW() " +
                     "ORDER BY expires_at DESC";
        List<Announcement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                announcements.add(mapResultSetToAnnouncement(rs));
            }
        }
        return announcements;
    }

    // ------------------------------------------------------------------ update

    public boolean update(Announcement announcement) throws SQLException {
        String sql = "UPDATE announcements SET title = ?, content = ?, target_audience = ?, expires_at = ? " +
                     "WHERE announcement_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getContent());
            stmt.setString(3, announcement.getTargetAudience().name());

            if (announcement.getExpiresAt() != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(announcement.getExpiresAt()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }

            stmt.setInt(5, announcement.getAnnouncementId());

            return stmt.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------ delete

    public boolean delete(int announcementId) throws SQLException {
        String sql = "DELETE FROM announcements WHERE announcement_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, announcementId);
            return stmt.executeUpdate() > 0;
        }
    }

    // --------------------------------------------------------------- bulk expire
    // Soft-delete pattern: sets expires_at to NOW() rather than deleting rows.
    // Useful for admin "expire all old announcements" cleanup tasks.

    public int expireOlderThan(java.time.LocalDateTime cutoff) throws SQLException {
        String sql = "UPDATE announcements SET expires_at = NOW() " +
                     "WHERE (expires_at IS NULL OR expires_at > NOW()) AND published_at < ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));
            return stmt.executeUpdate();
        }
    }

    // --------------------------------------------------------------- count active

    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM announcements WHERE expires_at IS NULL OR expires_at > NOW()";

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // --------------------------------------------------------------- mapper

    private Announcement mapResultSetToAnnouncement(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.setAnnouncementId(rs.getInt("announcement_id"));
        a.setTitle(rs.getString("title"));
        a.setContent(rs.getString("content"));
        a.setAuthorId(rs.getInt("author_id"));
        a.setTargetAudience(TargetAudience.valueOf(rs.getString("target_audience")));
        a.setPublishedAt(rs.getTimestamp("published_at").toLocalDateTime());

        Timestamp expiresTs = rs.getTimestamp("expires_at");
        if (expiresTs != null) {
            a.setExpiresAt(expiresTs.toLocalDateTime());
        }
        return a;
    }
}
