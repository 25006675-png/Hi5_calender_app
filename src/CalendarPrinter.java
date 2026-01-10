import java.util.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;

public class CalendarPrinter {

    // ---------------- List View (Generic for Day/Week/Month) ----------------
    public static void printList(List<Event> events, LocalDate start, LocalDate end, String title) {
        System.out.println("\n=== " + title + " ===");
        LocalDate current = start;
        while (!current.isAfter(end)) {
            String dayNum = String.format("%02d", current.getDayOfMonth());
            String dayName = current.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            
            System.out.print(dayName + " " + dayNum + ": ");
            
            List<String> eventStrings = new ArrayList<>();
            for (Event e : events) {
                // Check if event occurs on this day
                // Handle recurrences already expanded in 'events' or single events
                // The 'events' list passed here should ideally be the searched/expanded list for the range.
                if (isEventOnDate(e, current)) {
                    eventStrings.add(e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ")");
                }
            }
            
            if (eventStrings.isEmpty()) {
                System.out.print("No events");
            } else {
                System.out.print(String.join(", ", eventStrings));
            }
            System.out.println();
            current = current.plusDays(1);
        }
    }

    // ---------------- Calendar Month View ----------------
    public static void printMonthCalendar(List<Event> events, YearMonth yearMonth) {
        String monthName = yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        System.out.println("\n" + monthName + " " + yearMonth.getYear());
        
        System.out.println("Su  Mo  Tu  We  Th  Fr  Sa");

        int cellWidth = 4;
        LocalDate firstDay = yearMonth.atDay(1);
        int firstWeekDay = firstDay.getDayOfWeek().getValue() % 7; // Sunday = 0

        // Indent first row
        for (int i = 0; i < firstWeekDay; i++) {
            System.out.print(String.format("%-" + cellWidth + "s", ""));
        }

        int length = yearMonth.lengthOfMonth();
        for (int day = 1; day <= length; day++) {
            LocalDate current = yearMonth.atDay(day);
            boolean hasEvent = events.stream().anyMatch(e -> isEventOnDate(e, current));
            
            String dayStr = String.format("%-2d%s", day, hasEvent ? "*" : " ");
            System.out.print(String.format("%-" + cellWidth + "s", dayStr));

            // New line on Saturday (which is value 6, but we check 'firstWeekDay' logic or just day of week)
            if (current.getDayOfWeek().getValue() == 6 || (current.getDayOfWeek().getValue() == 7 && false)) { // 6 is Sat in ISO? No.
                // Java: Mon=1 ... Sun=7.
                // We want Sunday header... 
                // Using standard java DayOfWeek: Mon(1)..Sun(7).
                // My header is Su Mo ... Sa.
                // If header starts at Sun, then Sat is the end of row.
                // Sat is 6.
            }
            // Simpler: Just rely on counter
            int currentPos = (firstWeekDay + day - 1) % 7;
            if (currentPos == 6) {
                System.out.println();
            }
        }
        System.out.println(); // Final newline

        // List events clearly below
        System.out.println("* = day with events");
        for (Event e : events) {
            LocalDate ed = e.getStartDateTime().toLocalDate();
            // Filter strictly for this month (though passed list might be larger)
             if (ed.getYear() == yearMonth.getYear() && ed.getMonth() == yearMonth.getMonth()) {
                 System.out.println("* " + ed.getDayOfMonth() + ": " + e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ")");
             }
        }
    }

    // ---------------- Calendar Week View ----------------
    public static void printWeekCalendar(List<Event> events, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        System.out.println("\nWEEK VIEW (" + weekStart + " to " + weekEnd + ")");

        int colWidth = 5; 
        String[] dayNames = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (String d : dayNames) System.out.print(String.format("%-" + colWidth + "s", d));
        System.out.println();

        for (int i = 0; i < 7; i++) {
            LocalDate current = weekStart.plusDays(i);
            boolean hasEvent = events.stream().anyMatch(e -> isEventOnDate(e, current));
            String dayStr = String.format("%-2d%s", current.getDayOfMonth(), hasEvent ? "*" : " ");
            System.out.print(String.format("%-" + colWidth + "s", dayStr));
        }
        System.out.println("\n\n* = day with events");

        // List details
        for (Event e : events) {
            LocalDate ed = e.getStartDateTime().toLocalDate();
            if (!ed.isBefore(weekStart) && !ed.isAfter(weekEnd)) {
                System.out.println("* " + ed.getDayOfMonth() + ": " + e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ")");
            }
        }
    }

    private static boolean isEventOnDate(Event e, LocalDate date) {
        LocalDate start = e.getStartDateTime().toLocalDate();
        LocalDate end = e.getEndDateTime().toLocalDate();
        // Simple case: starts on date.
        // Complex case: spans date.
        // User's previous code mostly checked start date. I will stick to start date or intersection.
        // For simplicity and matching user's likely mental model of "Today's events", we check overlap.
        return !start.isAfter(date) && !end.isBefore(date);
    }
}
