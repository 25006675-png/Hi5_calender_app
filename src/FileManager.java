import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.zip.*;
/**
 *
 * @author User
 */
public class FileManager {
    
    protected static final String FOLDER_NAME = "data";
    protected static final String EVENT_FILE_PATH = FOLDER_NAME + File.separator + "event.csv";
    protected static final String RECURRENT_FILE_PATH = FOLDER_NAME + File.separator + "recurrent.csv";
    protected static final String EVENT_HEADER = "eventId,title,description,startDateTime,endDateTime,location,category,attendees";
    protected static final String RECURRENT_HEADER = "eventId, recurrentInterval, recurrentTimes, recurrentEndDate";

    protected static final String ADDITIONAL_FILE_PATH = FOLDER_NAME + File.separator + "additional.csv";
    protected static final String ADDITIONAL_HEADER = "eventId,location,category,attendees";
    private int maxEventId = 0;


    public FileManager(){
        ensureDataFolderExists();
    }


    // Load events from CSV file
    public List<Event> loadEvents() {

    // use LinkedHashMap to preserve the order in which they are saved
    Map<Integer, Event> joinMap = new LinkedHashMap<>();

    // 2. File Reading
    try (BufferedReader br = new BufferedReader(new FileReader(EVENT_FILE_PATH))) {
        br.readLine();
        String line;
        while((line=br.readLine())!=null){
            if(!(line.trim()).isEmpty()){
                Event e = new Event(line.split(","));

                joinMap.put(e.getEventId(), e);
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
    //read additional.csv
    try (BufferedReader br = new BufferedReader(new FileReader(ADDITIONAL_FILE_PATH))){
        br.readLine();
        String line;
        while((line = br.readLine())!= null){
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length > 0) {
                int id = Integer.parseInt(parts[0].trim());

                if (joinMap.containsKey(id)) {
                    Event eventToUpdate = joinMap.get(id);
                    eventToUpdate.setLocation(parts.length > 1 ? parts[1].trim() : "None");
                    eventToUpdate.setCategory(parts.length > 2 ? parts[2].trim() : "General");
                    eventToUpdate.setAttendees(parts.length > 3 ? parts[3].trim() : "None");
                }
            }
        }
    }catch (IOException e){
        System.out.println("no additional data found.");
    }
    return new ArrayList<>(joinMap.values());
    }

    public Map<Integer, RecurrenceRule> loadRecurrentRules(){
        Map<Integer, RecurrenceRule> rules = new HashMap<>();
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
                    rules.put(Id, new RecurrenceRule(Id, interval, times, endDate));
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
                pw.println(event.toCsvString()); //toCsvString() method in Event.java
            }
            System.out.println("Events successfully saved to " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing to " + file.getAbsolutePath() + ": " + e.getMessage());
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(ADDITIONAL_FILE_PATH))){
            pw.println(ADDITIONAL_HEADER);
            for (Event e: events){
                pw.println(e.toAdditionalCsv()); // method inside Event.java
            }
        }catch (IOException e){
            System.out.println("Fail to save to additional.csv");
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
             // load current event fresh
            List<Event> currentEvents = loadEvents();

            for (String line : backupEventLines) {

                if (line.contains("title")) continue;
                // use constructor
                Event backupEvent = new Event(line.split(","));

                if (backupEvent == null) continue;

                int oldId = backupEvent.getEventId();

                // If an equivalent event already exists, map oldId -> existingId and skip merging
                Event existing = null;
                for (Event e : currentEvents) {
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
                currentEvents.add(backupEvent);
                idMap.put(oldId, newId);
            }

            // Persist merged list (will write header too)
            saveEvents(currentEvents);

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

    public void appendRecurrences(List<String> backupRecurLines, Map<Integer, Integer> idMap) {

        // load current rules fresh
        Map<Integer, RecurrenceRule> currentRulesMap = loadRecurrentRules();
        List<RecurrenceRule> rulesToSave = new ArrayList<>(currentRulesMap.values());

        for (String line : backupRecurLines) {
            RecurrenceRule backupRule = RecurrenceRule.fromCsvR(line);

            if (backupRule != null) {
                int oldId = backupRule.getEventId();

                // Only proceed if the event exists in our current calendar (via the Map)
                if (idMap.containsKey(oldId)) {
                    int newEventId = idMap.get(oldId);

                    // FIX: Remove any existing rule for this event ID before adding the one from backup
                    // This prevents "Double Rules" for the same event.
                    rulesToSave.removeIf(existingRule -> existingRule.getEventId() == newEventId);

                    RecurrenceRule fixedRule = new RecurrenceRule(
                        newEventId,
                        backupRule.getRecurrentInterval(),
                        backupRule.getRecurrentTimes(),
                        backupRule.getRecurrentEndDate()
                    );

                    rulesToSave.add(fixedRule);
                }
            }
        }
        // Final Step: Write the cleaned, merged list back to the disk
        saveRecurrenceRule(rulesToSave);
    }

    public void appendAdditional(List<String> backupAdditionalLine, Map<Integer, Integer> idMap) {
        List<String> currentLines = new ArrayList<>();
        File additionalFile = new File(ADDITIONAL_FILE_PATH);

        if (additionalFile.exists()){
            try{
                currentLines = Files.readAllLines(additionalFile.toPath());
            }catch (IOException e){
                System.err.println("Error reading existing additional fields: " + e.getMessage());
            }
        }

        if (currentLines.isEmpty()){
            currentLines.add(ADDITIONAL_HEADER);
        }

        for(String line : backupAdditionalLine){
            String[] parts = line.split(",");
            try {
                int oldId = Integer.parseInt(parts[0].trim());

                if (idMap.containsKey(oldId)){
                    // reconstruct the line with new ID
                    // parts[1...n] are additional fields
                    int newId = idMap.get(oldId);
                    StringBuilder newLine = new StringBuilder(String.valueOf(newId));
                    for(int i = 1; i<parts.length; i++){
                        newLine.append(",").append(parts[i]);
                    }
                    currentLines.add(newLine.toString());

                }
            }catch (NumberFormatException e){
                // skip lines that aren't valid data (empty lines)
                continue;
            }
        }
    }
        // Find the next available event ID
        public int getNextAvailableEventId() {
            return ++maxEventId;
        }

}        