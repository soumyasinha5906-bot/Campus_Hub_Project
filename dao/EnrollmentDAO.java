package dao;

import config.DatabaseConfig;
import model.Enrollment;
import model.Enrollment.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnrollmentDAO {
    
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();
    
    public int create(Enrollment enrollment) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, course_id, status) VALUES (?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, enrollment.getStudentId());
            stmt.setInt(2, enrollment.getCourseId());
            stmt.setString(3, enrollment.getStatus().name());
            
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
    
    public Optional<Enrollment> findById(int enrollmentId) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE enrollment_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, enrollmentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public Optional<Enrollment> findByStudentAndCourse(int studentId, int courseId) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE student_id = ? AND course_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public List<Enrollment> findByStudent(int studentId) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE student_id = ? ORDER BY enrolled_at DESC";
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
        }
        return enrollments;
    }
    
    public List<Enrollment> findByCourse(int courseId) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE course_id = ? AND status = 'ACTIVE'";
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
        }
        return enrollments;
    }
    
    public boolean updateGrade(int enrollmentId, String grade) throws SQLException {
        String sql = "UPDATE enrollments SET grade = ? WHERE enrollment_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, grade);
            stmt.setInt(2, enrollmentId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateStatus(int enrollmentId, Status status) throws SQLException {
        String sql = "UPDATE enrollments SET status = ? WHERE enrollment_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, enrollmentId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean drop(int studentId, int courseId) throws SQLException {
        String sql = "UPDATE enrollments SET status = 'DROPPED' WHERE student_id = ? AND course_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean delete(int enrollmentId) throws SQLException {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, enrollmentId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    private Enrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(rs.getInt("enrollment_id"));
        enrollment.setStudentId(rs.getInt("student_id"));
        enrollment.setCourseId(rs.getInt("course_id"));
        enrollment.setEnrolledAt(rs.getTimestamp("enrolled_at").toLocalDateTime());
        enrollment.setGrade(rs.getString("grade"));
        enrollment.setStatus(Status.valueOf(rs.getString("status")));
        return enrollment;
    }
}
