import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class EventFileReader {
    public static List<Event> loadEvents(String filePath) {
        List<Event> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");

                int id = Integer.parseInt(data[0]);
                String title = data[1];
                LocalDateTime start = LocalDateTime.parse(data[3]);

                events.add(new Event(id, title, start));
            }
        } catch (Exception e) {
            System.out.println("Error reading event.csv");
        }

        return events;
    }
}