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

    private static final String EVENT_FILE_PATH = "data" + File.separator + "event.csv";
    private static final String RECURRENCE_FILE_PATH = "data" + File.separator + "recurrent.csv";

    //backup event file and recurrence file into single file
    private static final String EVENT_MARKER = "---EVENTS---";
    private static final String RECURRENCE_MARKER = "---RECURRENCES---";

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
        
        // 1. Separate the data using your markers
        boolean readingEvents = false;
        boolean readingRecurrences = false;
        for (String line : allLines) {
            if (line.equals(EVENT_MARKER)) { readingEvents = true; readingRecurrences = false; continue; }
            if (line.equals(RECURRENCE_MARKER)) { readingEvents = false; readingRecurrences = true; continue; }
            
            if (readingEvents) backupEventLines.add(line);
            else if (readingRecurrences) backupRecurLines.add(line);
        }

        // 2. The process differs based on mode
        if (appendMode) {
        // 1. Process Events first and get the 'Translation Key'
        Map<Integer, Integer> idMap = fm.mergeAndSaveBackup(backupEventLines);

        // 2. Give that key to the RecurrenceManager to fix the recurrences
        fm.appendRecurrences(backupRecurLines, idMap);

        System.out.println("✅ Backup appended successfully!");
    }
        else {
            // REPLACE MODE: Overwrite files directly (Clean Slate)
            Files.write(Paths.get(EVENT_FILE_PATH), backupEventLines);
            Files.write(Paths.get(RECURRENCE_FILE_PATH), backupRecurLines);
            
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