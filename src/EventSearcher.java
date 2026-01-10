import javax.swing.event.ListDataEvent;
import java.time.LocalDateTime;
import java.util.*;
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
                // baseEvents with no recurrence - CHECK OVERLAP instead of containment
                if (base.getStartDateTime().isBefore(end) && base.getEndDateTime().isAfter(start)) {
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

    public List<Event> advanceFilter(List<Event> events, String keyword, String category, String location, String attendees){

        String lowKeyword = keyword == null ? keyword = "" : keyword.toLowerCase().trim();
        String lowLocation = location == null ? location = "" : location.toLowerCase().trim();
        String lowAttendees = attendees == null ? attendees = "" : attendees.toLowerCase().trim();

        return events.stream().filter(e -> {
            boolean matchesKeyword = (lowKeyword.isEmpty()
                    || e.getTitle().toLowerCase().contains(lowKeyword)
                    || e.getDescription().toLowerCase().contains(lowKeyword)
                    || e.getCategory().toLowerCase().contains(lowKeyword)
                    || e.getLocation().toLowerCase().contains(lowKeyword)
                    || e.getAttendees().toLowerCase().contains(lowKeyword)

            );


            boolean matchesCategory = (category == null
                    || category.equalsIgnoreCase("General")
                    || e.getCategory().toLowerCase().equalsIgnoreCase(category));

            boolean matchesLocation = (lowLocation.isEmpty()
                    || e.getLocation().toLowerCase().contains(lowLocation)
                    );
            boolean matchesAttendees = (lowAttendees.isEmpty()
                    || e.getAttendees().toLowerCase().contains(lowAttendees)
            );
            return matchesKeyword && matchesCategory && matchesLocation && matchesAttendees;
        }).toList();
    }
}