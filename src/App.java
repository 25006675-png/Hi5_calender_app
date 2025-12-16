import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class App {

    public static void main(String[] args) {
        FileManager fileManager = new FileManager();
        List<Event> events = fileManager.loadEvents();
        Scanner sc = new Scanner(System.in);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (true) {
            System.out.println("\n=== CALENDAR MENU ===");
            System.out.println("1. List View - Day");
            System.out.println("2. List View - Week");
            System.out.println("3. List View - Month");
            System.out.println("4. Calendar View - Week");
            System.out.println("5. Calendar View - Month");
            System.out.println("6. Exit");
            System.out.println("7. Create Event");
            System.out.print("Enter your choice: ");

            int choice;
            try {
                String input = sc.nextLine();
                if (input.trim().isEmpty()) continue;
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            if (choice == 6) {
                System.out.println("Exiting program. Bye!");
                break;
            }

            switch (choice) {
                case 1: // List View - Day
                    System.out.print("Enter date (yyyy-MM-dd): ");
                    try {
                        LocalDate day = LocalDate.parse(sc.nextLine(), dateFormatter);
                        System.out.println("=== LIST VIEW: DAY ===");
                        ViewCalendar.showListView(events, day, day);
                    } catch (Exception e) {
                        System.out.println("Invalid date format.");
                    }
                    break;

                case 2: // List View - Week
                    System.out.print("Enter start date of week (yyyy-MM-dd): ");
                    try {
                        LocalDate weekStart = LocalDate.parse(sc.nextLine(), dateFormatter);
                        System.out.println("=== LIST VIEW: WEEK ===");
                        ViewCalendar.showListView(events, weekStart, weekStart.plusDays(6));
                    } catch (Exception e) {
                        System.out.println("Invalid date format.");
                    }
                    break;

                case 3: // List View - Month
                    System.out.print("Enter month and year (MM yyyy): ");
                    try {
                        String[] monthYear = sc.nextLine().split(" ");
                        int month = Integer.parseInt(monthYear[0]);
                        int year = Integer.parseInt(monthYear[1]);
                        LocalDate monthStart = LocalDate.of(year, month, 1);
                        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                        System.out.println("=== LIST VIEW: MONTH ===");
                        ViewCalendar.showListView(events, monthStart, monthEnd);
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                    }
                    break;

                case 4: // Calendar View - Week
                    System.out.print("Enter start date of week (yyyy-MM-dd): ");
                    try {
                        LocalDate calWeekStart = LocalDate.parse(sc.nextLine(), dateFormatter);
                        System.out.println("=== CALENDAR VIEW: WEEK ===");
                        ViewCalendar.showCalendarWeek(events, calWeekStart);
                    } catch (Exception e) {
                        System.out.println("Invalid date format.");
                    }
                    break;

                case 5: // Calendar View - Month
                    System.out.print("Enter month and year (MM yyyy): ");
                    try {
                        String[] calMonthYear = sc.nextLine().split(" ");
                        int calMonth = Integer.parseInt(calMonthYear[0]);
                        int calYear = Integer.parseInt(calMonthYear[1]);
                        System.out.println("=== CALENDAR VIEW: MONTH ===");
                        ViewCalendar.showCalendarMonth(events, calMonth, calYear);
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                    }
                    break;

                case 7: // Create Event
                    EventCreator creator = new EventCreator();
                    int lastId = 0;
                    for (Event e : events) {
                        if (e.getEventId() > lastId) {
                            lastId = e.getEventId();
                        }
                    }
                    Event newEvent = creator.createEvent(sc, lastId);
                    events.add(newEvent);
                    fileManager.saveEvents(events);
                    break;

                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }

        sc.close();
    }
}