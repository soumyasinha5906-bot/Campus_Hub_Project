import model.User;
import model.User.Role;
import model.Course;
import model.Enrollment;
import service.UserService;
import service.CourseService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class Main {
    
    public static void main(String[] args) {
        UserService userService = new UserService();
        CourseService courseService = new CourseService();
        
        try {
            // Register a new student
            User student = userService.register(
                "john.doe@university.edu",
                "SecurePass123",
                "John",
                "Doe",
                Role.STUDENT
            );
            System.out.println("Registered student: " + student.getFullName());
            
            // Register faculty
            User faculty = userService.register(
                "prof.smith@university.edu",
                "FacultyPass456",
                "Jane",
                "Smith",
                Role.FACULTY
            );
            System.out.println("Registered faculty: " + faculty.getFullName());
            
            // Create a course
            Course course = courseService.createCourse(
                "CS101",
                "Introduction to Computer Science",
                3,
                "Computer Science",
                faculty.getUserId()
            );
            System.out.println("Created course: " + course.getCourseName());
            
            // Enroll student
            Enrollment enrollment = courseService.enrollStudent(
                student.getUserId(),
                course.getCourseId()
            );
            System.out.println("Student enrolled in course with ID: " + enrollment.getEnrollmentId());
            
            // Authenticate user
            Optional<User> authenticated = userService.authenticate(
                "john.doe@university.edu",
                "SecurePass123"
            );
            
            if (authenticated.isPresent()) {
                System.out.println("Authentication successful for: " + authenticated.get().getEmail());
            }
            
            // Get all students
            List<User> students = userService.getStudents();
            System.out.println("Total students: " + students.size());
            
            // Check available seats
            int availableSeats = courseService.getAvailableSeats(course.getCourseId());
            System.out.println("Available seats in " + course.getCourseCode() + ": " + availableSeats);
            
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(Main.class.getName())
                .log(java.util.logging.Level.SEVERE, "Database error", e);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
        }
    }
}
