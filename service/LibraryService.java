package service;

import dao.LibraryDAO;
import model.Borrowing;
import model.LibraryResource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LibraryService {

    private final LibraryDAO libraryDAO = new LibraryDAO();

    // ================================================================ Resources

    /**
     * Adds a new library resource. ISBN must be unique if provided.
     */
    public LibraryResource addResource(String title, String author, String isbn,
                                       String category, int totalCopies) throws SQLException {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (totalCopies < 1) {
            throw new IllegalArgumentException("Total copies must be at least 1");
        }

        LibraryResource resource = new LibraryResource(title, author, isbn, category);
        resource.setTotalCopies(totalCopies);
        resource.setAvailableCopies(totalCopies);   // all copies available on creation

        int resourceId = libraryDAO.createResource(resource);
        if (resourceId > 0) {
            resource.setResourceId(resourceId);
            return resource;
        }

        throw new SQLException("Failed to create library resource");
    }

    public Optional<LibraryResource> getResourceById(int resourceId) throws SQLException {
        return libraryDAO.findResourceById(resourceId);
    }

    public List<LibraryResource> getAllResources() throws SQLException {
        return libraryDAO.findAllResources();
    }

    public List<LibraryResource> getAvailableResources() throws SQLException {
        return libraryDAO.findAvailableResources();
    }

    public List<LibraryResource> getResourcesByCategory(String category) throws SQLException {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        return libraryDAO.findByCategory(category);
    }

    /**
     * Searches across title, author, and ISBN.
     * Falls back to full-text search if the DAO supports it; currently
     * uses the LIKE-based implementation in LibraryDAO.
     */
    public List<LibraryResource> searchResources(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword cannot be empty");
        }
        return libraryDAO.searchResources(keyword.trim());
    }

    // ================================================================ Borrowing

    /**
     * Lends a resource to a user for {@code loanDays} days.
     * Checks that the user does not already have an active borrow of the same
     * resource before delegating to the transactional DAO method.
     *
     * @return the new borrowing ID, or throws if unavailable / already borrowed
     */
    public int borrowResource(int userId, int resourceId, int loanDays) throws SQLException {

        if (loanDays < 1) {
            throw new IllegalArgumentException("Loan period must be at least 1 day");
        }

        // Verify the resource exists
        Optional<LibraryResource> resourceOpt = libraryDAO.findResourceById(resourceId);
        if (resourceOpt.isEmpty()) {
            throw new IllegalArgumentException("Resource not found");
        }

        // Prevent duplicate active borrows for the same user + resource
        List<LibraryResource> currentlyBorrowed = libraryDAO.getBorrowedByUser(userId);
        boolean alreadyBorrowed = currentlyBorrowed.stream()
                .anyMatch(r -> r.getResourceId() == resourceId);

        if (alreadyBorrowed) {
            throw new IllegalArgumentException("User already has an active borrow for this resource");
        }

        if (!resourceOpt.get().isAvailable()) {
            throw new IllegalStateException("No copies available for borrowing");
        }

        int borrowingId = libraryDAO.borrowResource(userId, resourceId, loanDays);
        if (borrowingId < 0) {
            throw new IllegalStateException("Resource is not available (taken by another request)");
        }
        return borrowingId;
    }

    /**
     * Returns a previously borrowed resource.
     * Marks the borrowing as RETURNED and increments available copies atomically.
     */
    public boolean returnResource(int borrowingId) throws SQLException {
        return libraryDAO.returnResource(borrowingId);
    }

    /**
     * Lists all resources currently borrowed (status = BORROWED) by a given user.
     */
    public List<LibraryResource> getResourcesBorrowedByUser(int userId) throws SQLException {
        return libraryDAO.getBorrowedByUser(userId);
    }

    /**
     * Scheduled task entry-point: marks all borrowings whose due_date has passed
     * as OVERDUE. Should be called by a scheduler (e.g. daily cron).
     */
    public void runOverdueCheck() throws SQLException {
        libraryDAO.updateOverdueStatus();
    }

    // ================================================================ Availability helpers

    /**
     * Returns true if at least one copy of the given resource is available.
     */
    public boolean isAvailable(int resourceId) throws SQLException {
        return libraryDAO.findResourceById(resourceId)
                .map(LibraryResource::isAvailable)
                .orElse(false);
    }

    /**
     * Returns the number of available copies, or 0 if the resource is not found.
     */
    public int getAvailableCopies(int resourceId) throws SQLException {
        return libraryDAO.findResourceById(resourceId)
                .map(LibraryResource::getAvailableCopies)
                .orElse(0);
    }
}
