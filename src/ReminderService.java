import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderService {

    public static class ReminderNotification {
        private Event event;
        private String message;

        public ReminderNotification(Event event, String message) {
            this.event = event;
            this.message = message;
        }

        public Event getEvent() { return event; }
        public String getMessage() { return message; }
    }

    // Returns a list of reminder objects instead of showing an alert directly
    public static List<ReminderNotification> getReminders(List<Event> events, List<Reminder> reminders) {
        LocalDateTime now = LocalDateTime.now();
        List<ReminderNotification> notifications = new ArrayList<>();

        for (Event event : events) {
            for (Reminder reminder : reminders) {

                if (event.getEventId() == reminder.getEventId()) {

                    long minutesUntilEvent =
                            Duration.between(now, event.getStartDateTime()).toMinutes();

                    // Allow 0 for "starting now" events
                    if (minutesUntilEvent >= 0 &&
                        minutesUntilEvent <= reminder.getRemindBeforeMinutes()) {
                        
                        String timeString;
                        if (minutesUntilEvent >= 60) {
                            long hours = minutesUntilEvent / 60;
                            long mins = minutesUntilEvent % 60;
                            if (mins == 0) {
                                timeString = String.format("%d %s", hours, hours == 1 ? "hour" : "hours");
                            } else {
                                timeString = String.format("%d %s %d minutes", hours, hours == 1 ? "hour" : "hours", mins);
                            }
                        } else {
                            timeString = String.format("%d minutes", minutesUntilEvent);
                        }
                        
                        String msg = String.format("🔔 %s starts in %s.", event.getTitle(), timeString);
                        notifications.add(new ReminderNotification(event, msg));
                    }
                }
            }
        }
        return notifications;
    }
    
    // Deprecated: kept to avoid immediate compilation errors if called elsewhere, 
    // but CalendarGUI will be updated to use getReminders.
    public static void checkReminders(List<Event> events, List<Reminder> reminders) {
        // This will be replaced in GUI by getReminders + History Log
    }
}