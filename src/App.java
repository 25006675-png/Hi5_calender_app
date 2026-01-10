import java.util.Locale;

import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        Application.launch(CalendarGUI.class, args);
    }
}
