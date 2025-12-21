import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class ViewCalendar {

    // ---------------- List View ----------------
    public static void showListView(List<Event> events, LocalDate start, LocalDate end) {
        LocalDate current = start;
        while (!current.isAfter(end)) {
            String dayNum = String.format("%02d", current.getDayOfMonth());
            System.out.print(current.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + dayNum + ": ");
            boolean hasEvent = false;
            for (Event e : events) {
                LocalDate eventDate = e.getStartDateTime().toLocalDate();
                if (eventDate.equals(current)) {
                    System.out.print(e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ") ");
                    hasEvent = true;
                }
            }
            if (!hasEvent) System.out.print("No events");
            System.out.println();
            current = current.plusDays(1);
        }
    }

    // ---------------- Calendar Week View ----------------
    public static void showCalendarWeek(List<Event> events, LocalDate weekStart) {
        System.out.println("\nWEEK VIEW (" + weekStart + " to " + weekStart.plusDays(6) + ")");

        int colWidth = 5; // fixed width for every column
        
        // Header row (days of week)
        String[] dayNames = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (String d : dayNames) {
            System.out.print(String.format("%-" + colWidth + "s", d));
        }
        System.out.println();

        // Print day numbers with *
        for (int i = 0; i < 7; i++) {
            LocalDate current = weekStart.plusDays(i);

            boolean hasEvent = false;
            for (Event e : events) {
                LocalDate eventDate = e.getStartDateTime().toLocalDate();
                if (eventDate.equals(current)) {
                    hasEvent = true;
                    break;
                }
            }

            String dayStr = String.format("%2d%s", current.getDayOfMonth(), hasEvent ? "*" : " ");
            System.out.print(String.format("%-" + colWidth + "s", dayStr));
        }

        System.out.println("\n\n* = day with events\n");

        // Event list below
        for (int i = 0; i < 7; i++) {
            LocalDate current = weekStart.plusDays(i);
            
            for (Event e : events) {
                LocalDate eventDate = e.getStartDateTime().toLocalDate();
                if (eventDate.equals(current)) {
                    System.out.println("* " + current.getDayOfMonth() + ": " +
                            e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ")");
                }
            }
        }

        System.out.println();
    }

    // ---------------- Calendar Month View ----------------
    public static void showCalendarMonth(List<Event> events, int month, int year) {
        System.out.println("\n" + Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year);

        int cellWidth = 4; // for month day numbers only
        System.out.println("Su  Mo  Tu  We  Th  Fr  Sa");

        LocalDate firstDay = LocalDate.of(year, month, 1);
        int firstWeekDay = firstDay.getDayOfWeek().getValue() % 7; // Sunday = 0

        // Print empty spaces for first week
        for (int i = 0; i < firstWeekDay; i++) System.out.print(String.format("%" + cellWidth + "s", ""));

        int length = firstDay.lengthOfMonth();

        for (int day = 1; day <= length; day++) {
            LocalDate current = LocalDate.of(year, month, day);
            boolean hasEvent = false;
            for (Event e : events) {
                LocalDate eventDate = e.getStartDateTime().toLocalDate();
                if (eventDate.equals(current)) {
                    hasEvent = true;
                    break;
                }
            }

            String dayStr = String.format("%2d", day) + (hasEvent ? "*" : " ");
            System.out.print(dayStr + " ");

            if (current.getDayOfWeek() == DayOfWeek.SATURDAY) System.out.println();
        }

        System.out.println("\n* = day with events\n");

        // List events under calendar
        for (Event e : events) {
            LocalDate eventDate = e.getStartDateTime().toLocalDate();
            if (eventDate.getMonthValue() == month && eventDate.getYear() == year) {
                System.out.println("* " + eventDate.getDayOfMonth() + ": " + e.getTitle() + " (" + e.getStartDateTime().toLocalTime() + ")");
            }
        }
    }
}