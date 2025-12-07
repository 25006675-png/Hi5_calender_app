import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Blueprint for recurrence.csv
 * Does not contain calculation logic
 */

public class RecurrenceRule {
    private final int eventId; // link to base event
    private final String recurrentInterval;  // e.g. "1d", "1w", "1m"
    private final int recurrentTimes;  // e.g. 3
    private final LocalDateTime recurrentEndDate;

//  constructor: will be called by FileManager when loading recurrent.csv
    public RecurrenceRule(int eventId, String recurrentInterval, int recurrentTimes, LocalDateTime recurrentEndDate){
        this.eventId = eventId;
        this.recurrentInterval = recurrentInterval;
        this.recurrentTimes = recurrentTimes;
        this.recurrentEndDate = recurrentEndDate;
    }

//    getters
    public int getEventId(){
        return eventId;
    }

    public String getRecurrentInterval(){
        return recurrentInterval;
    }

    public int getRecurrentTimes(){
        return recurrentTimes;
    }

    public LocalDateTime getRecurrentEndDate(){
        return recurrentEndDate;
    }

}
