package calendar_app;

import java.time.LocalDate;

public class RecurringEventtest {

    public static void main(String[] args) {

        RecurringEvent event = new RecurringEvent(
                "Team Meeting",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 7),
                "DAILY"
        );

        event.printOccurrences();
    }
}
