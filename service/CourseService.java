package service;

public class CourseService {
    private dao.CourseDAO courseDAO = new dao.CourseDAO();
    private dao.EnrollmentDAO enrollmentDAO = new dao.EnrollmentDAO();

    // Temporary mock storage until DAOs are implemented
    private java.util.List<model.Course> mockCourses = new java.util.ArrayList<>();
    private java.util.List<model.Enrollment> mockEnrollments = new java.util.ArrayList<>();

    public model.Course createCourse(String courseCode, String courseName, int credits, String department, int facultyId) throws java.sql.SQLException {
        model.Course course = new model.Course(courseCode, courseName, credits, department);
        course.setFacultyId(facultyId);
        course.setCourseId(mockCourses.size() + 1);
        mockCourses.add(course);
        return course;
    }

    public model.Enrollment enrollStudent(int studentId, int courseId) throws java.sql.SQLException {
        model.Enrollment enrollment = new model.Enrollment(studentId, courseId);
        enrollment.setEnrollmentId(mockEnrollments.size() + 1);
        mockEnrollments.add(enrollment);
        return enrollment;
    }

    public int getAvailableSeats(int courseId) throws java.sql.SQLException {
        model.Course course = mockCourses.stream()
            .filter(c -> c.getCourseId() == courseId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        long enrolledCount = mockEnrollments.stream()
            .filter(e -> e.getCourseId() == courseId)
            .count();
            
        return course.getMaxCapacity() - (int) enrolledCount;
    }
}
