package terminal;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.regex.Pattern;

public class EventCreator {

    // This is the main method Main.java will call
    public Event createEvent(Scanner scanner, int lastEventId) {
        System.out.println("\n=== CREATE NEW EVENT ===");
        
        // 1. Auto-Generate ID
        int newId = lastEventId + 1;
        System.out.println("Generating Event ID: " + newId);

        // 2. Get Basic Info
        System.out.print("Enter Event Title: ");
        String title = scanner.nextLine();

        System.out.print("Enter Description: ");
        String description = scanner.nextLine();

        // 3. Get Start Date & Time (With validation loop)
        String startDate = promptForDate(scanner, "Start Date");
        String startTime = promptForTime(scanner, "Start Time");
        
        // 4. Get End Date & Time
        String endDate = promptForDate(scanner, "End Date");
        String endTime = promptForTime(scanner, "End Time");

        // 5. Combine into ISO string format (YYYY-MM-DDTHH:MM:SS)
        String startString = startDate + "T" + startTime + ":00";
        String endString = endDate + "T" + endTime + ":00";

        // 6. Convert String to LocalDateTime (REQUIRED by Teammate's Event.java)
        // This is the critical fix to match your teammate's code
        LocalDateTime startObj = LocalDateTime.parse(startString);
        LocalDateTime endObj = LocalDateTime.parse(endString);

        // 7. Create and Return the Object
        // Now passing LocalDateTime objects instead of Strings
        Event newEvent = new Event(newId, title, description, startObj, endObj);
        
        System.out.println("✅ Event object created successfully!");
        return newEvent;
    }

    // --- HELPER METHODS (To keep code clean) ---

    // Forces user to enter date in YYYY-MM-DD format
    private String promptForDate(Scanner scanner, String label) {
        String dateInput;
        // Regex pattern for YYYY-MM-DD
        String datePattern = "\\d{4}-\\d{2}-\\d{2}"; 
        
        while (true) {
            System.out.print("Enter " + label + " (YYYY-MM-DD): ");
            dateInput = scanner.nextLine().trim();
            
            if (Pattern.matches(datePattern, dateInput)) {
                return dateInput; // It's valid, break the loop
            } else {
                System.out.println("❌ Invalid format. Please use Year-Month-Day (e.g., 2025-10-30)");
            }
        }
    }

    // Forces user to enter time in HH:MM format
    private String promptForTime(Scanner scanner, String label) {
        String timeInput;
        // Regex pattern for HH:MM (24-hour format)
        String timePattern = "\\d{2}:\\d{2}";
        
        while (true) {
            System.out.print("Enter " + label + " (HH:MM in 24hr): ");
            timeInput = scanner.nextLine().trim();
            
            if (Pattern.matches(timePattern, timeInput)) {
                return timeInput;
            } else {
                System.out.println("❌ Invalid format. Please use HH:MM (e.g., 14:30)");
            }
        }
    }
    
    // --- TESTING SECTION (DELETE BEFORE FINAL SUBMISSION) ---
    /*public static void main(String[] args) {
        // This allows you to run this file alone to test if your inputs work!
        Scanner testScanner = new Scanner(System.in);
        terminal.EventCreator testCreator = new terminal.EventCreator();
        
        System.out.println("--- TESTING MODE: terminal.EventCreator.java ---");
        
        // Mocking the process with a dummy ID of 0
        try {
            Event e = testCreator.createEvent(testScanner, 0);
            
            System.out.println("\n--- TEST RESULT ---");
            System.out.println("Created ID: " + e.getEventId());
            System.out.println("Title: " + e.getTitle());
            System.out.println("Start: " + e.getStartDateTime());
            System.out.println("End: " + e.getEndDateTime());
        } catch (Exception ex) {
            System.out.println("❌ Test Failed: " + ex.getMessage());
            System.out.println("Make sure Event.java is in the same folder!");
        }
    } */
} 