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

         public Map<Integer, Integer> mergeAndSaveBackup(List<String> backupLines) {
    Map<Integer, Integer> idMap = new HashMap<>();
    
    // We start IDs from the highest current ID + 1
    int nextId = getNextAvailableEventId();

    for (String line : backupLines) {
        if (line.trim().isEmpty()) continue;
        
        Event backupEvent = Event.fromCsvToEvent(line);
        int oldId = backupEvent.getEventId();

        // --- SMART CHECK: Look for an existing identical event ---
        Event existingMatch = null;
        for (Event existing : this.events) {
            if (isSameEvent(existing, backupEvent)) {
                existingMatch = existing;
                break;
            }
        }

        if (existingMatch != null) {
            // It's a duplicate! Don't add it.
            // Map the backup's oldId to the existing event's current ID
            idMap.put(oldId, existingMatch.getEventId());
            System.out.println("Skipping duplicate event: " + backupEvent.getTitle());
        } else {
            // It's a new unique event. Add it.
            int newId = nextId++;
            backupEvent.setEventId(newId);
            idMap.put(oldId, newId);
            this.events.add(backupEvent);
        }
    }
    
    saveEvents(this.events); // Save the final merged list
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

        //allow append mode restore recurrence
        public void appendRecurrences(List<String> backupRecurLines, java.util.Map<Integer, Integer> idMap) {
            for (String line : backupRecurLines) {

                RecurrenceRule backupRule = RecurrenceRule.fromCsvR(line);
                
                if (backupRule != null) {
                    int oldId = backupRule.getEventId();

                    // Check if this recurrence belongs to an event we just restored
                    if (idMap.containsKey(oldId)) {
                        int newEventId = idMap.get(oldId);
                        
                        // Since fields are FINAL, we create a NEW object with the NEW ID
                        // but copy all other values from the backupRule
                        RecurrenceRule fixedRule = new RecurrenceRule(
                            newEventId, 
                            backupRule.getRecurrentInterval(), 
                            backupRule.getRecurrentTimes(), 
                            backupRule.getRecurrentEndDate()
                        );
                        // Add to our main list
                        this.recurrentlist.add(fixedRule);
                    }
                }
            }
            // After appending all, save to CSV file
            saveRecurrenceRule(this.recurrentlist);
        }

        // Find the next available event ID
        public int getNextAvailableEventId() {
            return ++maxEventId;
        }


        public void exportData(String destinationZipPath) throws IOException {
            // This creates a single ZIP file containing both your CSVs
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destinationZipPath))) {
                addToZip(EVENT_FILE_PATH, "event.csv", zos);
                addToZip(RECURRENT_FILE_PATH, "recurrent.csv", zos);
            }
        }

        private void addToZip(String filePath, String fileName, ZipOutputStream zos) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) return; // Don't zip if file doesn't exist yet
            
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }

        public void importData(String sourceZipPath) throws IOException {
            // This opens the ZIP and replaces your current event.csv and recurrent.csv
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceZipPath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path targetPath = Paths.get(FOLDER_NAME, entry.getName());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    zis.closeEntry();
                }
            }
            // Refresh the memory after importing files
            this.events.clear();
            loadEvents();
            loadRecurrentRules();
        }
}        