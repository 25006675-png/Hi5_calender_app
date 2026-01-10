import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        // Force English Locale for consistent UI (DatePickers, Alerts, WeekFields)
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        Application.launch(CalendarGUI.class, args);
    }
}

