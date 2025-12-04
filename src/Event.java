import java.time.LocalDateTime;

public class Event{
    private final int eventId;
    private final String title;
    private final String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // additional fields
    private String location;
    private String category;

    // constructors: same name but java can distinguish them through parameters

    /**
     * Constructor for BASE event
     * Called only by FileManager when loading event.csv
     * blueprint (baseEvent) for generating recurring occurrence instances
     */

    public Event(int eventId, String title, String description, LocalDateTime startDateTime, LocalDateTime endDateTime){
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.location = "Unspecified";
        this.category = "Unspecified";
    }

    /**
     * Constructor for RECURRING event
     * Called ONLY by RecurrenceManager when generating recurring occurrence instances
     * within a search range. (if not given by users, set the range to prevent infinite loop)
     * It copies the details from the baseEvent
     *
     * @param baseEvent the event template loaded by FileManager
     * @param newStart start date of a specific recurred instance
     * @param newEnd end date of a specific recurred instance
     */
    public Event(Event baseEvent, LocalDateTime newStart, LocalDateTime newEnd){
        this.eventId = baseEvent.eventId;
        this.title = baseEvent.title;
        this.description = baseEvent.description;
        this.startDateTime = newStart;
        this.endDateTime = newEnd;
        // additional fields
        this.location = baseEvent.location;
        this.category = baseEvent.category;
    }

    // setters (used by FileManager when saving event.csv)
    public void setLocation(String location){
        this.location = location;
    }
    public void setCategory(String category){
        this.category = category;
    }
    // getters
    public int getEventId(){
        return eventId;
    }
    public String getTitle(){
        return title;
    }
    public String getDescription(){
        return description;
    }
    public LocalDateTime getStartDateTime(){
        return startDateTime;
    }
    public LocalDateTime getEndDateTime(){
        return endDateTime;
    }

    public String getLocation(){
        return location;
    }
    public String getCategory(){
        return category;
    }



}