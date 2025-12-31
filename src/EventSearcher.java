import javax.swing.event.ListDataEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventSearcher{
    private final FileManager fileManager;
    private final RecurrenceManager recurrenceManager;

    EventSearcher(FileManager fm, RecurrenceManager rm){
        this.fileManager = fm;
        this.recurrenceManager = rm;

    }

    // BASIC Search: by date range.

    public List<Event> searchByDateRange(LocalDateTime start, LocalDateTime end){
        List<Event> results = new ArrayList<>();
        List<Event> baseEvents = fileManager.loadEvents();
        Map<Integer, RecurrenceRule> rules = fileManager.loadRecurrentRules();

        for (Event base : baseEvents){
            int eventID = base.getEventId();
            if (rules.containsKey(eventID)){
                results.addAll(recurrenceManager.generateOccurrences(base, rules.get(eventID), start, end));

            } else{
                // baseEvents with no recurrence
                if ((! base.getStartDateTime().isBefore(start)) && (! base.getEndDateTime().isAfter(end))){
                    results.add(base);
                }
            }
        }
        //sort by date (ascending)
        // Classname::methodName -> method reference
        // go to class Event and use getStartDateTime method
        results.sort(Comparator.comparing(Event::getStartDateTime));
        return results;
    }

    public List<Event> advanceFilter(List<Event> events, String keyword, String caterogy, String location){
        return events.stream().filter(e -> {
            boolean matchesKeyword = (keyword == null || keyword.trim().isEmpty()
                    || e.getTitle().toLowerCase().contains(keyword.toLowerCase())
                    || e.getDescription().toLowerCase().contains(keyword.toLowerCase()));

            boolean matchesCategory = (caterogy == null
                    || caterogy.equalsIgnoreCase("All")
                    || caterogy.toLowerCase().equalsIgnoreCase(caterogy));

            boolean matchesLocation = (location == null
                    || location.trim().isEmpty()
                    || location.toLowerCase().contains(location.toLowerCase())
                    );
            return matchesKeyword && matchesCategory && matchesLocation;
        }).toList();
    }
}