import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReminderManager {
    private static final String REMINDER_FILE = "data/reminder.csv"; // Ensure correct path
    private static final String HEADER = "eventId, minutesBefore";

    public ReminderManager() {
        // Ensure file exists
        File file = new File(REMINDER_FILE);
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
                try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                    pw.println(HEADER);
                }
            } catch (IOException e) {
                System.out.println("Error creating reminder.csv");
            }
        }
    }

    public void saveReminder(int eventId, int minutesBefore) {
        if (minutesBefore <= 0) return; // Don't save if "None" selected

        List<Reminder> reminders = ReminderFileReader.loadReminders(REMINDER_FILE);
        
        // Remove existing reminder for this event if it exists (update logic)
        reminders.removeIf(r -> r.getEventId() == eventId);
        
        // Add new reminder
        reminders.add(new Reminder(eventId, minutesBefore));

        saveAll(reminders);
    }
    
    // Used when deleting an event
    public void deleteReminder(int eventId) {
        List<Reminder> reminders = ReminderFileReader.loadReminders(REMINDER_FILE);
        boolean removed = reminders.removeIf(r -> r.getEventId() == eventId);
        if (removed) {
            saveAll(reminders);
        }
    }

    private void saveAll(List<Reminder> reminders) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(REMINDER_FILE))) {
            pw.println(HEADER);
            for (Reminder r : reminders) {
                pw.println(r.getEventId() + "," + r.getRemindBeforeMinutes());
            }
        } catch (IOException e) {
            System.out.println("Error saving reminders: " + e.getMessage());
        }
    }
    
    public int getReminderMinutes(int eventId) {
        List<Reminder> reminders = ReminderFileReader.loadReminders(REMINDER_FILE);
        for (Reminder r : reminders) {
            if (r.getEventId() == eventId) {
                return r.getRemindBeforeMinutes();
            }
        }
        return 0; // 0 means no reminder
    }
}
