import java.util.List;

public class EventEditor {

    private final FileManager fm; 
    private List<Event> events;

    public EventEditor(FileManager fm) {
        this.fm = fm;

        this.events = fm.loadEvents(); 
    }

    //delete event by eventId
    public boolean deleteEvent(int eventId) {
    
        for (int i = events.size() - 1; i >= 0; i--) { 
            
            Event currentEvent = events.get(i);
            
            if (currentEvent.getEventId()==eventId) {
                
                events.remove(i);
                
                this.fm.saveEvents(events);
                
                return true; }
        }
        return false; 
    }
    
    //update event title by eventId
    public boolean updateEventTitle(int eventId, String newTitle) {
    
    for (Event event: events) {
        
        if (event.getEventId() == eventId) {
            
            event.setTitle(newTitle);
            
            this.fm.saveEvents(this.events);
            
            return true; }
        }
    return false; 
    }
}
