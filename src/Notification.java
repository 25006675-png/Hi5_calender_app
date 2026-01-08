import java.util.List;
import java.io.File;
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        List<Event> events = EventFileReader.loadEvents("event.csv");
        List<Reminder> reminders = ReminderFileReader.loadReminders("reminder.csv");
        ReminderService.checkReminders(events, reminders);
        
        
        System.out.println("Reminder file exists? " + new File("reminder.csv").exists());
    }
    
}