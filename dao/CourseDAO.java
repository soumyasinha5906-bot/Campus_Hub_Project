package dao;

import config.DatabaseConfig;
import model.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseDAO {
    
    private final DatabaseConfig dbConfig = DatabaseConfig.getInstance();
    
    public int create(Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_code, course_name, description, credits, department, faculty_id, max_capacity) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, course.getCourseCode());
            stmt.setString(2, course.getCourseName());
            stmt.setString(3, course.getDescription());
            stmt.setInt(4, course.getCredits());
            stmt.setString(5, course.getDepartment());
            stmt.setInt(6, course.getFacultyId());
            stmt.setInt(7, course.getMaxCapacity());
            
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
    
    public Optional<Course> findById(int courseId) throws SQLException {
        String sql = "SELECT * FROM courses WHERE course_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCourse(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public Optional<Course> findByCode(String courseCode) throws SQLException {
        String sql = "SELECT * FROM courses WHERE course_code = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, courseCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCourse(rs));
                }
            }
        }
        return Optional.empty();
    }
    
    public List<Course> findAll() throws SQLException {
        String sql = "SELECT * FROM courses ORDER BY course_code";
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        }
        return courses;
    }
    
    public List<Course> findByDepartment(String department) throws SQLException {
        String sql = "SELECT * FROM courses WHERE department = ? ORDER BY course_code";
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, department);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
        }
        return courses;
    }
    
    public List<Course> findByFaculty(int facultyId) throws SQLException {
        String sql = "SELECT * FROM courses WHERE faculty_id = ? ORDER BY course_code";
        List<Course> courses = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, facultyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
        }
        return courses;
    }
    
    public int getEnrollmentCount(int courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE course_id = ? AND status = 'ACTIVE'";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    public boolean update(Course course) throws SQLException {
        String sql = "UPDATE courses SET course_code = ?, course_name = ?, description = ?, " +
                     "credits = ?, department = ?, faculty_id = ?, max_capacity = ? WHERE course_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, course.getCourseCode());
            stmt.setString(2, course.getCourseName());
            stmt.setString(3, course.getDescription());
            stmt.setInt(4, course.getCredits());
            stmt.setString(5, course.getDepartment());
            stmt.setInt(6, course.getFacultyId());
            stmt.setInt(7, course.getMaxCapacity());
            stmt.setInt(8, course.getCourseId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean delete(int courseId) throws SQLException {
        String sql = "DELETE FROM courses WHERE course_id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courseId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setCourseName(rs.getString("course_name"));
        course.setDescription(rs.getString("description"));
        course.setCredits(rs.getInt("credits"));
        course.setDepartment(rs.getString("department"));
        course.setFacultyId(rs.getInt("faculty_id"));
        course.setMaxCapacity(rs.getInt("max_capacity"));
        return course;
    }
}
