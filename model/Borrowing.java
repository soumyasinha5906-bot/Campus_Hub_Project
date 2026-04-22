package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Borrowing {

    private int borrowingId;
    private int userId;
    private int resourceId;
    private LocalDateTime borrowedAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;   // null until returned
    private Status status;

    public enum Status {
        BORROWED, RETURNED, OVERDUE
    }

    public Borrowing() {}

    public Borrowing(int userId, int resourceId, LocalDate dueDate) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.dueDate = dueDate;
        this.status = Status.BORROWED;
    }

    // Getters and Setters
    public int getBorrowingId() { return borrowingId; }
    public void setBorrowingId(int borrowingId) { this.borrowingId = borrowingId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(LocalDateTime borrowedAt) { this.borrowedAt = borrowedAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    /**
     * Convenience check — true if the book has not been returned
     * and the due date is in the past.
     */
    public boolean isOverdue() {
        return status == Status.BORROWED && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    /**
     * Number of days remaining before the due date.
     * Returns a negative number if already overdue.
     */
    public long daysRemaining() {
        if (dueDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }
}
