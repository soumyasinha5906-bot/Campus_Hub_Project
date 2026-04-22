package service;

import model.User;
import model.User.Role;
import dao.UserDAO;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

public class UserService {
    private UserDAO userDAO = new UserDAO();
    
    // For now, we will use a temporary in-memory list since UserDAO is empty.
    // TODO: Connect these to your UserDAO methods once they are written.
    private List<User> mockDatabase = new ArrayList<>();

    public User register(String email, String password, String firstName, String lastName, Role role) throws SQLException {
        // Here you would normally hash the password before saving
        User user = new User(email, password, firstName, lastName, role);
        user.setUserId(mockDatabase.size() + 1);
        
        // userDAO.addUser(user);
        mockDatabase.add(user);
        return user;
    }

    public Optional<User> authenticate(String email, String password) throws SQLException {
        // userDAO.getUserByEmailAndPassword(email, password);
        return mockDatabase.stream()
            .filter(u -> u.getEmail().equals(email) && u.getPasswordHash().equals(password))
            .findFirst();
    }

    public List<User> getStudents() throws SQLException {
        // userDAO.getAllStudents();
        return mockDatabase.stream()
            .filter(u -> u.getRole() == Role.STUDENT)
            .toList();
    }
}
