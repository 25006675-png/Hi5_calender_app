package assignment2;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
public class ReminderFileReader {
    public static List<Reminder> loadReminders(String filePath) {
        List<Reminder> reminders = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Skip empty lines or header line
                if (line.isEmpty() || line.startsWith("eventId")) {
                    continue;
                }

                String[] data = line.split(",");

                // Trim each field to avoid errors
                int eventId = Integer.parseInt(data[0].trim());
                int minutes = Integer.parseInt(data[1].trim());

                reminders.add(new Reminder(eventId, minutes));
            }

        } catch (Exception e) {
            System.out.println("Error reading reminder file.");
            e.printStackTrace(); // Show exact cause
        }

        return reminders;
    }
}