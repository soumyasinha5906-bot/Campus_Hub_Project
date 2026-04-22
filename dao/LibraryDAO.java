package dao;

import config.DatabaseConfig;
import model.LibraryResource;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibraryDAO {
    
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();
    
    // Resource CRUD operations
    public int createResource(LibraryResource resource) throws SQLException {
        String sql = "INSERT INTO library_resources (title, author, isbn, category, total_copies, available_copies) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, resource.getTitle());
            stmt.setString(2, resource.getAuthor());
            stmt.setString(3, resource.getIsbn());
            stmt.setString(4, resource.getCategory());
            stmt.setInt(5, resource.getTotalCopies());
            stmt.setInt(6, resource.getAvailableCopies());
            
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
    
    public Optional<LibraryResource> findResourceById(int resourceId) throws SQLException {
        String sql = "SELECT * FROM library_resources WHERE resource_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, resourceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public List<LibraryResource> findAllResources() throws SQLException {
        String sql = "SELECT * FROM library_resources ORDER BY title";
        List<LibraryResource> resources = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        }
        return resources;
    }
    
    public List<LibraryResource> searchResources(String keyword) throws SQLException {
        String sql = "SELECT * FROM library_resources WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ?";
        List<LibraryResource> resources = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResultSetToResource(rs));
                }
            }
        }
        return resources;
    }
    
    public List<LibraryResource> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM library_resources WHERE category = ? ORDER BY title";
        List<LibraryResource> resources = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResultSetToResource(rs));
                }
            }
        }
        return resources;
    }
    
    public List<LibraryResource> findAvailableResources() throws SQLException {
        String sql = "SELECT * FROM library_resources WHERE available_copies > 0 ORDER BY title";
        List<LibraryResource> resources = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        }
        return resources;
    }
    
    // Borrowing operations
    public int borrowResource(int userId, int resourceId, int loanDays) throws SQLException {
        Connection conn = null;
        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Check availability
            String checkSql = "SELECT available_copies FROM library_resources WHERE resource_id = ? FOR UPDATE";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, resourceId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next() || rs.getInt("available_copies") <= 0) {
                        conn.rollback();
                        return -1; // Not available
                    }
                }
            }
            
            // Decrement available copies
            String updateSql = "UPDATE library_resources SET available_copies = available_copies - 1 WHERE resource_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, resourceId);
                updateStmt.executeUpdate();
            }
            
            // Create borrowing record
            String borrowSql = "INSERT INTO borrowings (user_id, resource_id, due_date, status) VALUES (?, ?, ?, 'BORROWED')";
            try (PreparedStatement borrowStmt = conn.prepareStatement(borrowSql, Statement.RETURN_GENERATED_KEYS)) {
                borrowStmt.setInt(1, userId);
                borrowStmt.setInt(2, resourceId);
                borrowStmt.setDate(3, Date.valueOf(LocalDate.now().plusDays(loanDays)));
                
                borrowStmt.executeUpdate();
                
                try (ResultSet generatedKeys = borrowStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        conn.commit();
                        return generatedKeys.getInt(1);
                    }
                }
            }
            
            conn.rollback();
            return -1;
            
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }
    
    public boolean returnResource(int borrowingId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Get resource ID from borrowing
            String getSql = "SELECT resource_id FROM borrowings WHERE borrowing_id = ? AND status = 'BORROWED'";
            int resourceId;
            try (PreparedStatement getStmt = conn.prepareStatement(getSql)) {
                getStmt.setInt(1, borrowingId);
                try (ResultSet rs = getStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    resourceId = rs.getInt("resource_id");
                }
            }
            
            // Update borrowing status
            String updateBorrowingSql = "UPDATE borrowings SET status = 'RETURNED', returned_at = CURRENT_TIMESTAMP WHERE borrowing_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateBorrowingSql)) {
                updateStmt.setInt(1, borrowingId);
                updateStmt.executeUpdate();
            }
            
            // Increment available copies
            String updateResourceSql = "UPDATE library_resources SET available_copies = available_copies + 1 WHERE resource_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateResourceSql)) {
                updateStmt.setInt(1, resourceId);
                updateStmt.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
        }
    }
    
    public List<LibraryResource> getBorrowedByUser(int userId) throws SQLException {
        String sql = "SELECT lr.* FROM library_resources lr " +
                     "JOIN borrowings b ON lr.resource_id = b.resource_id " +
                     "WHERE b.user_id = ? AND b.status = 'BORROWED'";
        List<LibraryResource> resources = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResultSetToResource(rs));
                }
            }
        }
        return resources;
    }
    
    public void updateOverdueStatus() throws SQLException {
        String sql = "UPDATE borrowings SET status = 'OVERDUE' " +
                     "WHERE status = 'BORROWED' AND due_date < CURRENT_DATE";
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    private LibraryResource mapResultSetToResource(ResultSet rs) throws SQLException {
        LibraryResource resource = new LibraryResource();
        resource.setResourceId(rs.getInt("resource_id"));
        resource.setTitle(rs.getString("title"));
        resource.setAuthor(rs.getString("author"));
        resource.setIsbn(rs.getString("isbn"));
        resource.setCategory(rs.getString("category"));
        resource.setTotalCopies(rs.getInt("total_copies"));
        resource.setAvailableCopies(rs.getInt("available_copies"));
        return resource;
    }
}
