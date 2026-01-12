import java.time.LocalDateTime;

/**
 * Represents a specific instance of a recurring event that has been deleted or modified.
 * If found in exceptions.csv, the RecurrenceManager will skip generating this instance.
 */
public class ExceptionRule {
    private final int eventId;
    private final LocalDateTime exceptionDate;
    private final String type; // e.g., "DELETE"

    public ExceptionRule(int eventId, LocalDateTime exceptionDate, String type) {
        this.eventId = eventId;
        this.exceptionDate = exceptionDate;
        this.type = type;
    }

    public int getEventId() { return eventId; }
    public LocalDateTime getExceptionDate() { return exceptionDate; }
    public String getType() { return type; }
}