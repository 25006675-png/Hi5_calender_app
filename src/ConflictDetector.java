import java.time.LocalDateTime;
import java.util.List;

public class ConflictDetector {

    private final EventSearcher searcher;
    private final RecurrenceManager recurrenceManager;

    public ConflictDetector(FileManager fileManager, RecurrenceManager recurrenceManager) {
        this.recurrenceManager = recurrenceManager;
        this.searcher = new EventSearcher(fileManager, recurrenceManager);
    }

    /**
     * Checks if the given event (recurrence or single) conflicts with existing events.
     * @param candidate The event to check.
     * @param rule The recurrence rule (can be null for non-recurrent).
     * @return true if a conflict exists.
     */
    public boolean check(Event candidate, RecurrenceRule rule) {
        // 1. If non-recurrent, just check the single range
        if (rule == null || rule.getRecurrentInterval().equals("Do not repeat")) {
            return checkRange(candidate.getStartDateTime(), candidate.getEndDateTime(), candidate.getEventId());
        }

        // 2. If recurrent, check instances for up to 1 year or rule limit
        // We limit to 1 year or ~100 instances to avoid performance freeze on "Daily forever"
        LocalDateTime searchStart = candidate.getStartDateTime();
        LocalDateTime searchEnd = searchStart.plusYears(1); 
        
        List<Event> instances = recurrenceManager.generateOccurrences(candidate, rule, searchStart, searchEnd);
        
        for (Event inst : instances) {
            if (checkRange(inst.getStartDateTime(), inst.getEndDateTime(), candidate.getEventId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkRange(LocalDateTime start, LocalDateTime end, int ignoreId) {
        // Search for potential collisions in this time window
        List<Event> found = searcher.searchByDateRange(start, end);
        
        for (Event e : found) {
            // Ignore self (for edit mode)
            if (e.getEventId() == ignoreId) continue;

            // Check overlap: (StartA < EndB) and (StartB < EndA)
            if (start.isBefore(e.getEndDateTime()) && e.getStartDateTime().isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
