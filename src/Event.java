import java.time.LocalDateTime;

public class Event{
    private int eventId;
    private String title;
    private String description;
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
    //#added by wy
    public void setEventId(int eventId){ //used in AppendModeBackup() method in EventManager.java (for restoreEvents() method in BackupManager.java)
        this.eventId = eventId;
    }
    public void setTitle(String title){ //used in updateEventTitle() method in EventEditor.java
        this.title = title;
    }
    public void setDescription(String description){
        this.description = description;
    }
    public void setStartDateTime(LocalDateTime startDateTime){
        this.startDateTime = startDateTime;
    }
    public void setEndDateTime(LocalDateTime endDateTime){
        this.endDateTime = endDateTime;
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

    //# added by wy
    // Convert event in arraylist to CSV string for saving (used in saveEvent() method in EventManager.java) 
    public String toCsvString() {
        return this.eventId + "," + this.title + "," + this.description + ","+ this.startDateTime + "," + this.endDateTime + ","+ this.location + ","+ this.category;}
    

    /**
     * Constructor used by FileManager to create an Event object 
     * from a line of CSV data (String array).
     */
    public Event(String[] parts) {
        
        this.eventId = Integer.parseInt(parts[0]); 

        this.title = parts[1];
        this.description = parts[2];

        this.startDateTime = LocalDateTime.parse(parts[3]);
        this.endDateTime = LocalDateTime.parse(parts[4]);

        // Initialize optional fields to avoid nulls
        if (parts.length > 5) {
            this.location = parts[5];
        } else {
            this.location = "Unspecified";
        }
        
        if (parts.length > 6) {
            this.category = parts[6];
        } else {
            this.category = "Unspecified";
        }
    }

}