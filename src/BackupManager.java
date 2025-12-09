import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.NoSuchFileException;

public class BackupManager {

    private static final String EVENT_FILE_PATH = "data" + File.separator + "event.csv";
    private static final String EVENT_FILE_NAME = "event.csv";
    private static final String RECURRENCE_FILE_PATH = "data" + File.separator + "recurrent.csv";
    private static final String RECURRENCE_FILE_NAME = "recurrent.csv";
    
    
    private final FileManager fm;     
    public BackupManager(FileManager fm) {
    this.fm = fm; }


    //for Backup, sourcePath = datafile, targetPath = user's backup file
    //for Restore, sourcePath = user's backup file, targetPath = datafile
    private void copyFile(Path sourcePath, Path targetPath) throws IOException {
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean BackupEvents(String backupPath) {
        Path backupDir = Paths.get(backupPath);

        try {
            // Ensure the backup directory exists before copying
            Files.createDirectories(backupDir);

            // 1. Copy event.csv (Source: data/event.csv | Target: backupPath/event.csv)
            Path eventSource = Paths.get(EVENT_FILE_PATH);
            Path eventTarget = backupDir.resolve(EVENT_FILE_NAME);
            copyFile(eventSource, eventTarget);

            // 2. Copy recurrent.csv (Source: data/recurrent.csv | Target: backupPath/recurrent.csv)
            Path recurSource = Paths.get(RECURRENCE_FILE_PATH);
            Path recurTarget = backupDir.resolve(RECURRENCE_FILE_NAME);
            copyFile(recurSource, recurTarget);
            
            System.out.println("Backup has been completed to: " + backupDir.toAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
            return false;
        }
    }

    public boolean restoreEvents(String backupPath, boolean appendMode) {
    Path backupDir = Paths.get(backupPath);
    
    if (!Files.isDirectory(backupDir)) {
        System.err.println("Restore failed: Backup directory not found or is not a directory.");
        return false;
    }

    try {

        Path eventBackupPath = backupDir.resolve(EVENT_FILE_NAME);
        Path recurBackupPath = backupDir.resolve(RECURRENCE_FILE_NAME);

        // --- A. APPEND MODE (Calls FileManager for complex data merge) ---
        if (appendMode) {

            boolean eventSuccess = this.fm.AppendModeBackup(eventBackupPath);//method in EventManager.java
            
            if (eventSuccess) {
                 // After merging events, simply replace the recurrence file since merging recurrences is too complex.
                 Path recurTarget = Paths.get(RECURRENCE_FILE_PATH);
                 copyFile(recurBackupPath, recurTarget); 
                 System.out.println("Restore completed. Events appended and recurrence list replaced.");
                 return true;
            } else {
                 return false;
            }

        } 
        
        // --- B. REPLACE MODE (Simple File Copy) ---
        else {
            // 1. Replace event.csv
            Path eventTarget = Paths.get(EVENT_FILE_PATH);
            copyFile(eventBackupPath, eventTarget);

            // 2. Replace recurrent.csv
            Path recurTarget = Paths.get(RECURRENCE_FILE_PATH);
            copyFile(recurBackupPath, recurTarget); 

            System.out.println("Restore completed. Live data files replaced from backup in " + backupDir.toAbsolutePath());
            return true;
        }

    } catch (NoSuchFileException e) {
        System.err.println("Restore failed: Missing required file in backup. Ensure " + e.getFile() + " exists in the backup folder.");
        return false;
    } catch (IOException e) {
        System.err.println("estore failed: An error occurred during file copy/merge: " + e.getMessage());
        return false;
    }
}
    
    /*if we don't want to use append mode for restoring, the restoreEvents() method would be like this
    
    public boolean restoreEvents(String backupPath) {
    Path backupDir = Paths.get(backupPath);

    // Check if the input path is a valid directory
    if (!Files.isDirectory(backupDir)) {
        System.err.println("Restore failed: Backup directory not found or is not a directory.");
        return false;}

    try {

        // 1. Define Backup Source Paths

        Path eventBackupPath = backupDir.resolve(EVENT_FILE_NAME);
        Path recurBackupPath = backupDir.resolve(RECURRENCE_FILE_NAME);

        // 2. Perform Copy Operations

        Path eventTarget = Paths.get(EVENT_FILE_PATH);
        copyFile(eventBackupPath, eventTarget); // Source is eventBackupPath

        // Replace recurrent.csv
        Path recurTarget = Paths.get(RECURRENCE_FILE_PATH);
        copyFile(recurBackupPath, recurTarget); // Source is recurBackupPath
    
        System.out.println("Restore completed. Live data files replaced from backup in " + backupDir.toAbsolutePath().toString());
        return true;

    } catch (NoSuchFileException e) {
        System.err.println("Restore failed: Missing required file in backup. Ensure " + e.getFile() + " exists in the backup folder.");
        return false;
    } catch (IOException e) {
        System.err.println("Restore failed: An error occurred during file copy/merge: " + e.getMessage());
        return false;
    }
}
    
    */
    
}
