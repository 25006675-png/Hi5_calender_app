import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
public class ReminderService {
    public static void checkReminders(List<Event> events, List<Reminder> reminders) {
        LocalDateTime now = LocalDateTime.now();

        for (Event event : events) {
            for (Reminder reminder : reminders) {

                if (event.getEventId() == reminder.getEventId()) {

                    long minutesUntilEvent =
                            Duration.between(now, event.getStartDateTime()).toMinutes();

                    if (minutesUntilEvent > 0 &&
                        minutesUntilEvent <= reminder.getRemindBeforeMinutes()) {

                        System.out.println("🔔 Reminder:");
                        System.out.println(event.getTitle()
                                + " starts in " + minutesUntilEvent + " minutes.");
                    }
                }
            }
        }
    }
}