import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.*;

/**
 *
 * @author User
 */
public class FileManager {
    
    private static final String FOLDER_NAME = "data";
    private static final String EVENT_FILE_PATH = FOLDER_NAME + File.separator + "event.csv";
    private static final String RECURRENT_FILE_PATH = FOLDER_NAME + File.separator + "recurrent.csv";
    private static final String EVENT_HEADER = "eventId,title,description,startDateTime,endDateTime,location,category";
    private static final String RECURRENT_HEADER = "eventId, recurrentInterval, recurrentTimes, recurrentEndDate";

    private int maxEventId = 0;
    List<Event> events = new ArrayList<>();
    List<RecurrenceRule> recurrentlist = new ArrayList<>();

    // Load events from CSV file
    public List<Event> loadEvents() {
    
        this.events.clear();
    // 1. Check for data folder existence and exit if creation fails.
    if (!ensureDataFolderExists()) { //method below
        return events;
    }
    
    // 2. File Reading
    try (BufferedReader br = new BufferedReader(new FileReader(EVENT_FILE_PATH))) {
        
        br.readLine();
        
        String line;
        while((line=br.readLine())!=null){
            if(!(line.trim()).isEmpty()){
                Event e = new Event(line.split(","));
                events.add(e);
                // update maxEventId while loading the file
                if(e.getEventId() > maxEventId){
                    maxEventId = e.getEventId();
                }
            }
        }
    
    }
    // 3. Handle a missing file (Self-Healing file creation)
    catch (FileNotFoundException e){
        createEmptyFile(EVENT_FILE_PATH, EVENT_HEADER);// method below
        return loadEvents();
    }
    catch (IOException e) {
        System.err.println("Error reading file: " + e.getMessage());}

    return events;
    }

    public Map<Integer, RecurrenceRule> loadRecurrentRules(){
        Map<Integer, RecurrenceRule> rules = new HashMap<>();
        this.recurrentlist.clear();


        try (BufferedReader br = new BufferedReader(new FileReader(RECURRENT_FILE_PATH))) {
            br.readLine(); // read header (ignored)
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] data = line.split(",");
                    int Id = Integer.parseInt(data[0].trim());
                    String interval = data[1].trim();
                    int times = Integer.parseInt(data[2].trim());
                    LocalDateTime endDate = data[3].trim().equals("0") ? null : LocalDateTime.parse(data[3]);

                    RecurrenceRule newRule = new RecurrenceRule(Id, interval, times, endDate);

                    rules.put(Id, newRule); 
                    this.recurrentlist.add(newRule);
                }
            }
        }catch (FileNotFoundException e){
            createEmptyFile(RECURRENT_FILE_PATH, RECURRENT_HEADER);
            return loadRecurrentRules();

        }catch (IOException e){
            System.err.println("Error loading recurrent.csv: " + e.getMessage());
        }

        return rules;
    }

    // Save events to CSV file
    public void saveEvents(List<Event> events) {
        File file = new File(EVENT_FILE_PATH);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            pw.println(EVENT_HEADER);

            for (Event event: events){
                pw.println(event.toCsvStringE()); //toCsvString() method in Event.java
            }
            System.out.println("Events successfully saved to " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing to " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }
    public void saveRecurrenceRule(List<RecurrenceRule> rules) {
        File file = new File(RECURRENT_FILE_PATH);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            pw.println(RECURRENT_HEADER);

            for (RecurrenceRule rule : rules) {
                String recurrentEndDate = (rule.getRecurrentEndDate() == null)
                        ? "0" : rule.getRecurrentEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

                String line = (String.format(("%d,%s,%d,%s"),
                        rule.getEventId(),
                        rule.getRecurrentInterval(),
                        rule.getRecurrentTimes(),
                        recurrentEndDate)
                );
                pw.println(line);
            }
            System.out.println("Events successfully saved to " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing to " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }


    private boolean ensureDataFolderExists() {
        
        Path datafolder = Paths.get(FOLDER_NAME);
        
        if(Files.notExists(datafolder)){
            try{
                Files.createDirectories(datafolder);
                System.out.println("Created missing directory: "+FOLDER_NAME);
            }
            catch(IOException e){
                System.err.println("Unable to create a new data folder: "+e.getMessage());
                return false;
            }
        }return true;
}
        // general file creation
        private void createEmptyFile(String path, String header) {

            try(PrintWriter pw = new PrintWriter(new FileWriter(path))){
                
                pw.println(header);
                System.out.println("Created new, empty event file at: "+ path);
                
            }catch (IOException e){
                System.err.println("Unable to create a new event file: "+e.getMessage());
            }
        }

         public Map<Integer, Integer> mergeAndSaveBackup(List<String> backupEventLines) {
            Map<Integer, Integer> idMap = new HashMap<>();

            // Ensure we have the current events loaded and maxEventId set
            loadEvents();

            for (String line : backupEventLines) {
                Event backupEvent = Event.fromCsvToEvent(line);
                if (backupEvent == null) continue;

                int oldId = backupEvent.getEventId();

                // If an equivalent event already exists, map oldId -> existingId and skip merging
                Event existing = null;
                for (Event e : this.events) {
                    if (isSameEvent(e, backupEvent)) { existing = e; break; }
                }

                if (existing != null) {
                    idMap.put(oldId, existing.getEventId());
                    continue; // don't add duplicate
                }

                // Otherwise assign a fresh ID and add the event
                int newId = getNextAvailableEventId();
                // Reuse the parsed object but assign it a new id
                backupEvent.setEventId(newId);
                this.events.add(backupEvent);
                idMap.put(oldId, newId);
            }

            // Persist merged list (will write header too)
            saveEvents(this.events);

            return idMap;
}

/**
 * Helper to determine if two events are "The Same".
 * You can add more checks (like location or description) if you want.
 */
private boolean isSameEvent(Event e1, Event e2) {
    return e1.getTitle().equalsIgnoreCase(e2.getTitle()) && 
           e1.getStartDateTime().equals(e2.getStartDateTime());
}

public void appendRecurrences(List<String> backupRecurLines, java.util.Map<Integer, Integer> idMap) {
    for (String line : backupRecurLines) {
        RecurrenceRule backupRule = RecurrenceRule.fromCsvR(line);
        
        if (backupRule != null) {
            int oldId = backupRule.getEventId();

            // Only proceed if the event exists in our current calendar (via the Map)
            if (idMap.containsKey(oldId)) {
                int newEventId = idMap.get(oldId);
                
                // FIX: Remove any existing rule for this event ID before adding the one from backup
                // This prevents "Double Rules" for the same event.
                this.recurrentlist.removeIf(existingRule -> existingRule.getEventId() == newEventId);

                RecurrenceRule fixedRule = new RecurrenceRule(
                    newEventId, 
                    backupRule.getRecurrentInterval(), 
                    backupRule.getRecurrentTimes(), 
                    backupRule.getRecurrentEndDate()
                );
                
                this.recurrentlist.add(fixedRule);
            }
        }
    }
    // Final Step: Write the cleaned, merged list back to the disk
    saveRecurrenceRule(this.recurrentlist);
}

        // Find the next available event ID
        public int getNextAvailableEventId() {
            return ++maxEventId;
        }

}        