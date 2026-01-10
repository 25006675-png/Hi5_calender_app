package calendar_app;

import java.time.LocalDate;
import java.time.LocalTime;

public class ConflictDetector {

    public static boolean hasConflict(
            LocalDate date1, LocalTime start1, LocalTime end1,
            LocalDate date2, LocalTime start2, LocalTime end2) {

        // Different dates → no conflict
        if (!date1.equals(date2)) {
            return false;
        }

        // Time overlap check
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
