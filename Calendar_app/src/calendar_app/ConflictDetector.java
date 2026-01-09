package calendar_app;

public class ConflictDetector {

    public static boolean hasConflict(Event e1, Event e2) {

        // Different dates → no conflict
        if (!e1.date.equals(e2.date)) {
            return false;
        }

        // Time overlap check
        return e1.startTime.isBefore(e2.endTime) &&
               e2.startTime.isBefore(e1.endTime);
    }
}
