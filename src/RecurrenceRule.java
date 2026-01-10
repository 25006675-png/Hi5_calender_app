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

    public static RecurrenceRule fromCsvR(String line) {
    try {
        String[] parts = line.split(",");
        int eventId = Integer.parseInt(parts[0]);
        String interval = parts[1];
        int times = Integer.parseInt(parts[2]);
        
        LocalDateTime endDate = null;
        if (parts.length > 3) {
            String raw = parts[3].trim();
            if (!raw.equals("0") && !raw.equalsIgnoreCase("null") && !raw.isEmpty()) {
                endDate = LocalDateTime.parse(raw);
            }
        }

        return new RecurrenceRule(eventId, interval, times, endDate);
    } catch (Exception e) {
        return null; // Skip lines that are broken
    }
}

}


