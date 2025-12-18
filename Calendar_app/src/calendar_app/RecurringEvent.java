package calendar_app;

import java.time.LocalDate;

public class RecurringEvent {

    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String frequency; // DAILY or WEEKLY

    public RecurringEvent(String title, LocalDate startDate,
                          LocalDate endDate, String frequency) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = frequency;
    }

    public void printOccurrences() {
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            System.out.println(title + " on " + current);

            if (frequency.equalsIgnoreCase("DAILY")) {
                current = current.plusDays(1);
            } else if (frequency.equalsIgnoreCase("WEEKLY")) {
                current = current.plusWeeks(1);
            } else {
                break;
            }
        }
    }
}
