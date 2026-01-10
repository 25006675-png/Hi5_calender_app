package calendar_app;

import java.time.LocalDate;
import java.time.LocalTime;

public class ConflictDetectorTest {

    public static void main(String[] args) {

        Event event1 = new Event(
                "Team Meeting",
                LocalDate.of(2025, 1, 10),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        Event event2 = new Event(
                "Client Call",
                LocalDate.of(2025, 1, 10),
                LocalTime.of(10, 30),
                LocalTime.of(12, 0)
        );

        if (ConflictDetector.hasConflict(event1, event2)) {
            System.out.println("Conflict detected!");
        } else {
            System.out.println("No conflict.");
        }
    }
}
