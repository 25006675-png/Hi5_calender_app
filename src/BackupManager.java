import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.NoSuchFileException;

public class BackupManager {

    private static final String EVENT_FILE_PATH = FileManager.EVENT_FILE_PATH;
    private static final String RECURRENCE_FILE_PATH = FileManager.RECURRENT_FILE_PATH;
    private static final String ADDITIONAL_FILE_PATH = FileManager.ADDITIONAL_FILE_PATH;
    //backup event file and recurrence file into single file
    private static final String EVENT_MARKER = "---EVENTS---";
    private static final String RECURRENCE_MARKER = "---RECURRENCES---";
    private static final String ADDITIONAL_MARKER = "---ADDITIONAL---";

    private FileManager fm;

    public BackupManager(FileManager fm) {
        this.fm = fm;
    }

    public boolean BackupEvents(String backupFilePath) {
        // backupFilePath should now be the full path + filename, e.g., "D:/my_backup.txt"
        Path targetFile = Paths.get(backupFilePath);

        try {
            List<String> combinedLines = new ArrayList<>();

            // 1. Read and add Event data
            combinedLines.add(EVENT_MARKER);
            combinedLines.addAll(Files.readAllLines(Paths.get(EVENT_FILE_PATH)));

            // 2. Read and add Recurrence data
            combinedLines.add(RECURRENCE_MARKER);
            combinedLines.addAll(Files.readAllLines(Paths.get(RECURRENCE_FILE_PATH)));

            combinedLines.add(ADDITIONAL_MARKER);
            combinedLines.addAll(Files.readAllLines(Paths.get(ADDITIONAL_FILE_PATH)));
            // 3. Save everything into the ONE single file
            Files.write(targetFile, combinedLines);
            
            System.out.println("✅ Single backup file created at: " + targetFile.toAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("❌ Backup failed: " + e.getMessage());
            return false;
        }
    }

    //restore backup (considered Append Mode)
    public boolean restoreEvents(String backupFilePath, boolean appendMode) {
    Path backupPath = Paths.get(backupFilePath);
    
    try {
        List<String> allLines = Files.readAllLines(backupPath);
        List<String> backupEventLines = new ArrayList<>();
        List<String> backupRecurLines = new ArrayList<>();
        List<String> backupAdditionalLines = new ArrayList<>();
        
        // 1. Separate the data using your markers
        boolean readingEvents = false;
        boolean readingRecurrences = false;
        boolean readingAdditional = false;

        for (String line : allLines) {
            switch (line) {
                case EVENT_MARKER -> {
                    readingEvents = true;
                    readingRecurrences = false;
                    readingAdditional = false;
                    continue;
                }
                case RECURRENCE_MARKER -> {
                    readingEvents = false;
                    readingRecurrences = true;
                    readingAdditional = false;
                    continue;
                }
                case ADDITIONAL_MARKER -> {
                    readingEvents = false;
                    readingRecurrences = false;
                    readingAdditional = true;
                    continue;
                }
            }
            // Skip empty lines
            if (line == null || line.trim().isEmpty()) continue;

            // When reading event section, skip CSV header if present
            if (readingEvents) {
                if (line.contains("title") || line.contains("description")) continue;
                backupEventLines.add(line);
            }
            // When reading recurrence section, skip CSV header if present
            else if (readingRecurrences) {
                if (line.contains("recurrentInterval") || line.contains("recurrentTimes")) continue;
                backupRecurLines.add(line);
            }
            else if (readingAdditional){
                if (line.contains("category") || line.contains("location")) continue;;
                backupAdditionalLines.add(line);
            }
        }

        // 2. The process differs based on mode
        if (appendMode) {
        // 1. Process Events first and get the 'Translation Key'
        Map<Integer, Integer> idMap = fm.mergeAndSaveBackup(backupEventLines);

        // 2. Give that key to the RecurrenceManager to fix the recurrences
        fm.appendRecurrences(backupRecurLines, idMap);
        fm.appendAdditional(backupAdditionalLines, idMap);
        fm.loadEvents(); 
        fm.loadRecurrentRules();
        System.out.println("✅ Backup appended successfully!");
    }
        else {
            // REPLACE MODE: Parse backup lines into objects and use FileManager save helpers
            List<Event> eventsToSave = new ArrayList<>();
            for (String el : backupEventLines) {
                // use constructor
                Event e = new Event(el.split(","));
                if (e != null) eventsToSave.add(e);
            }

            List<RecurrenceRule> rulesToSave = new ArrayList<>();
            for (String rl : backupRecurLines) {
                RecurrenceRule r = RecurrenceRule.fromCsvR(rl);
                if (r != null) rulesToSave.add(r);
            }

            // Save via FileManager so headers are included and internal lists updated
            fm.saveEvents(eventsToSave);
            fm.saveRecurrenceRule(rulesToSave);

            // Reload managers so they see the new data immediately
            fm.loadEvents();
            fm.loadRecurrentRules();

            System.out.println("✅ Replaced all data with backup.");
        }
        return true;

    } catch (IOException e) {
        System.err.println("❌ Restore failed: " + e.getMessage());
        return false;
    }
}

    
    
    
    
}