import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author User
 */
public class FileManager {
    
    private static final String FOLDER_NAME = "data";
    private static final String EVENT_FILE_PATH = FOLDER_NAME + File.separator + "event.csv";
    private static final String EVENT_HEADER = "eventId,title,description,startDateTime,endDateTime,location,category";
    
    // Load events from CSV file    
    public List<Event> loadEvents() {
    List<Event> events = new ArrayList<>();
    
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
                events.add(new Event(line.split(",")));
            }
        }
    
    }
    // 3. Handle a missing file (Self-Healing file creation)
    catch (FileNotFoundException e){
        createEmptyEventFile();// method below
        return loadEvents();
    }
    catch (IOException e) {
        System.err.println("Error reading file: " + e.getMessage());}

    return events;
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
        
        private void createEmptyEventFile() {
            
            try(PrintWriter pw = new PrintWriter(new FileWriter(EVENT_FILE_PATH))){
                
                pw.println(EVENT_HEADER);
                System.out.println("Created new, empty event file at: "+EVENT_FILE_PATH);
                
            }catch (IOException e){
                System.err.println("Unable to create a new event file: "+e.getMessage());
            }
        }

        // !the methods below are needed if we want to implement Append Mode Backup feature (in BackupManager.java)!
        
        public boolean AppendModeBackup(Path backupFilePath) {
            
            // 1. Load the current live events list
            List<Event> liveEvents = loadEvents(); 
            
            // 2. Load the events from the external backup file
            List<Event> backupEvents = loadEventsFromPath(backupFilePath); // method below

            if (backupEvents.isEmpty()) {
                System.out.println("No events found in the backup file. Nothing to merge.");
                return true; // Considered successful
            }
            
            // 3. Find the next unique ID in the current live events list
            int nextId = getNextAvailableEventId(liveEvents); // method below
            
            // 4. Merge: Loop through backup events, re-ID them, and add to live list
            for (Event event : backupEvents) {
                event.setEventId(nextId++); 
                liveEvents.add(event);
            }
            
            // 5. Save the combined list back to the live event.csv file
            saveEvents(liveEvents);
            
            System.out.println("✅ " + backupEvents.size() + " events successfully appended.");
            return true;
        }

        //this method is similiar to loadEvents() but loads from a custom path
        public List<Event> loadEventsFromPath(Path customPath) {
            List<Event> events = new ArrayList<>();
            
            try (BufferedReader br = new BufferedReader(new FileReader(customPath.toFile()))) {
                
                br.readLine(); // Skip header
                String line;
                while((line=br.readLine())!=null){
                    if(!(line.trim()).isEmpty()){
                        events.add(new Event(line.split(",")));
                    }
                }
            } catch (FileNotFoundException e) {
                // This is expected if the backup file doesn't exist yet
                System.out.println("No backup file found at the specified path.");
            } catch (IOException e) {
                System.err.println("Error reading backup file at " + customPath.toString() + ": " + e.getMessage());
            }
            return events;
        }

        // Find the next available event ID
        private int getNextAvailableEventId(List<Event> events) {
            int maxId = 0;
            for (Event event : events) {
                if (event.getEventId() > maxId) {
                    maxId = event.getEventId();
                }
            }
            return maxId + 1;
        }

        // NOTE: This method requires the two helper methods defined previously:
        // 1. public List<Event> loadEventsFromPath(Path customPath)
        // 2. private int getNextAvailableEventId(List<Event> events) 
        // The Event class must also have a public setter: setEventId(int id).
}        