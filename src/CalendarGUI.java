import com.sun.javafx.event.EventHandlerManager;
import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.*;

public class CalendarGUI extends Application {

    private YearMonth currentYearMonth;
    private LocalDate currentDate; // For Week/Day focus

    private FileManager fileManager;
    private RecurrenceManager recurrenceManager;
    private EventSearcher searcher;

    private List<Event> visibleEvents = new ArrayList<>();
//    allEvents changed to visibleEvents (update of searcher which already loadEvents)

    private GridPane calendarGrid;
    private ListView<String> eventListView;
    private Label titleLabel;
    private TextField searchBar;
    private ComboBox<String> viewSwitcher;
    private BorderPane root;
    private BackupManager backupManager;
    
    // Notification History
    private List<String> notificationHistory = new ArrayList<>();
    private Set<Integer> notifiedEventIds = new HashSet<>();
    private java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) {


        // Initialize state
        currentYearMonth = YearMonth.now();
        currentDate = LocalDate.now();
        
        // Load Real Data
        try {
            fileManager = new FileManager();
            recurrenceManager = new RecurrenceManager();
            searcher = new EventSearcher(fileManager, recurrenceManager);
            backupManager = new BackupManager(fileManager);
            // allEvents only for event creation

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Data Load Error");
            alert.setHeaderText("Could not load events");
            alert.setContentText("There was an issue loading your events from the file.\n" + e.getMessage());
            alert.showAndWait();
        }

        root = new BorderPane();
        
        // Top Bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Center: Initialized in drawCalendar
        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");
        root.setCenter(calendarGrid); // Ensure it's added to the scene graph
        
        eventListView = new ListView<>();

        Scene scene = new Scene(root, 1100, 700);
        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load style.css: " + e.getMessage());
        }
        
        primaryStage.setTitle("Calendar App");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        drawCalendar(); // Draw immediately so its not blank
            
        drawCalendar(); // Draw immediately so its not blank
        
        // Start background polling for reminders
        startReminderService();
    }

    private void startReminderService() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Determine if we need to reload files? 
                // For safety vs corruption, maybe just reload reminders?
                // Events might be edited while app is open.
                // fileManager.loadEvents() is safe-ish.
                // Use searcher to include today's recurrent instances
                LocalDateTime now = LocalDateTime.now();
                // Check events for the next 30 days to catch most upcoming reminders (including recurrences)
                List<Event> allEvents = searcher.searchByDateRange(now.minusMinutes(5), now.plusDays(30)); 
                List<Reminder> reminders = ReminderFileReader.loadReminders("data/reminder.csv");
                
                if (reminders != null && !reminders.isEmpty()) {
                    List<ReminderService.ReminderNotification> newReminders = ReminderService.getReminders(allEvents, reminders);
                    
                    if (!newReminders.isEmpty()) {
                        javafx.application.Platform.runLater(() -> processNotifications(newReminders));
                    }
                }
            } catch (Exception e) {
                System.out.println("Scheduler Error: " + e.getMessage());
            }
        }, 0, 1, java.util.concurrent.TimeUnit.MINUTES);
    }

    private void processNotifications(List<ReminderService.ReminderNotification> notifications) {
         boolean alertNeeded = false;
         StringBuilder elementString = new StringBuilder();
         String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

         for (ReminderService.ReminderNotification n : notifications) {
             if (!notifiedEventIds.contains(n.getEvent().getEventId())) {
                 notifiedEventIds.add(n.getEvent().getEventId());
                 notificationHistory.add("[" + timestamp + "] " + n.getMessage());
                 elementString.append(n.getMessage()).append("\n");
                 alertNeeded = true;
             }
         }

         if (alertNeeded) {
              Alert alert = new Alert(Alert.AlertType.INFORMATION);
              alert.setTitle("Reminders");
              alert.setHeaderText("You have upcoming events!");
              alert.setContentText(elementString.toString());
              alert.show(); 
         }
    }

    @Override
    public void stop() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        super.stop();
    }

    private void refreshVisibleEvents(){
        LocalDateTime start ;
        LocalDateTime end;
        String view = viewSwitcher.getValue();

        if (view.contains("Week")){
            LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
            start = startOfWeek.atStartOfDay();
            end = startOfWeek.plusDays(7).atStartOfDay();
        } else if (view.contains("Day")){
            start = currentDate.atStartOfDay();
            end = currentDate.plusDays(1).atStartOfDay();

        //default view: month
        } else {
            LocalDate firstDayOfMonth = currentYearMonth.atDay(1);

            // If Oct 1 is Mon (1), we want prev Sunday -> minus 1 day.
            // If Oct 1 is Sun (7), we start ON that day -> minus 0 days.
            int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue();
            int offset = (dayOfWeekValue == 7) ? 0 : dayOfWeekValue;

            LocalDate gridStart = firstDayOfMonth.minusDays(offset);

            // start date + 42 days for a standard 6-rows 7-columns grid
            LocalDate gridEnd = gridStart.plusDays(42);

            start = gridStart.atStartOfDay();
            end = gridEnd.atStartOfDay();
        }
        visibleEvents = searcher.searchByDateRange(start, end);

        if (searchBar != null && ! searchBar.getText().isEmpty()){
            visibleEvents = searcher.advanceFilter(visibleEvents, searchBar.getText(), "General", "", "");
        }

    }


    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Navigation Buttons
        Button prevBtn = new Button("<");
        prevBtn.setOnAction(e -> previousMonth());
        
        Button nextBtn = new Button(">");
        nextBtn.setOnAction(e -> nextMonth());
        
        // View Switcher
        viewSwitcher = new ComboBox<>();
        viewSwitcher.getItems().addAll(
            "Calendar (Month)", 
            "Calendar (Week)", 
            "List (Day)", 
            "List (Week)", 
            "List (Month)"
        );
        viewSwitcher.setValue("Calendar (Month)");
        viewSwitcher.setOnAction(e -> {
            updateTitleLabel();
            drawCalendar();
        });
        
        // Title
        titleLabel = new Label();
        titleLabel.getStyleClass().add("title-label");
        updateTitleLabel();

        // Search Bar (Logic removed as requested, just UI)
        searchBar = new TextField();
        searchBar.setPromptText("Search keywords...");
        searchBar.setPrefWidth(200);
        searchBar.textProperty().addListener((obs, oldVal, newVal) -> drawCalendar());

        Button advancedSearchBtn = new Button("Advanced Search");
        advancedSearchBtn.setOnAction(e -> new SearchScene(searcher, this).show());

        // Spacer to push Create button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create Event Button
        Button createEventBtn = new Button("Create Event");
        createEventBtn.getStyleClass().add("create-event-btn");
        EventDialog creatingDialog = new EventDialog(fileManager, this::drawCalendar);

        createEventBtn.setOnAction(e -> creatingDialog.create());

        topBar.getChildren().addAll(
            prevBtn, nextBtn, 
            new Region() {{ setMinWidth(10); }}, // small spacer
            titleLabel, 
            new Region() {{ setMinWidth(20); }}, // spacer
            viewSwitcher, 
            new Region() {{ setMinWidth(20); }}, // spacer
                searchBar,
            advancedSearchBtn,
            spacer, 
            createEventBtn
        );
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);
        
        Label navLabel = new Label("MY CALENDARS");
        navLabel.getStyleClass().add("sidebar-header");
        
        Button homeBtn = new Button("Home");
        homeBtn.setOnAction(e -> drawCalendar());

        Button workBtn = new Button("Work");
        Button personalBtn = new Button("Personal");

        Button backupBtn = new Button("Export Backup");
        backupBtn.setOnAction(e -> handleBackup());

        Button restoreBtn = new Button("Import Restore");
        restoreBtn.setOnAction(e -> handleRestore());
            
        Button settingsBtn = new Button("Settings");

        Button mergeBtn = new Button("Merge/Import CSV");
        mergeBtn.setOnAction(e -> handleMerge());

        Button remindersBtn = new Button("Reminders");
        remindersBtn.setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: black;");
        remindersBtn.setOnAction(e -> drawNotificationView());
        
        // Push settings to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(navLabel, homeBtn, workBtn, personalBtn, new Separator(), backupBtn, restoreBtn, mergeBtn, remindersBtn, spacer, settingsBtn);
        return sidebar;
    }



    private void drawNotificationView() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        
        Label header = new Label("Notification History");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        ListView<String> historyList = new ListView<>();
        historyList.getItems().addAll(notificationHistory);
        
        if (notificationHistory.isEmpty()) {
            historyList.getItems().add("No notifications yet.");
        }
        
        VBox.setVgrow(historyList, Priority.ALWAYS);
        
        Button clearBtn = new Button("Clear History");
        clearBtn.setOnAction(e -> {
            notificationHistory.clear();
            drawNotificationView();
        });

        content.getChildren().addAll(header, historyList, clearBtn);
        root.setCenter(content);
    }

    private void previousMonth() {
        String view = viewSwitcher.getValue();
        if (view.contains("Week")) {
            currentDate = currentDate.minusWeeks(1);
        } else if (view.contains("Day")) {
            currentDate = currentDate.minusDays(1);
        } else {
            currentYearMonth = currentYearMonth.minusMonths(1);
            currentDate = currentDate.minusMonths(1);
        }
        updateTitleLabel();
        drawCalendar();
    }

    private void nextMonth() {
        String view = viewSwitcher.getValue();
        if (view.contains("Week")) {
            currentDate = currentDate.plusWeeks(1);
        } else if (view.contains("Day")) {
            currentDate = currentDate.plusDays(1);
        } else {
            currentYearMonth = currentYearMonth.plusMonths(1);
            currentDate = currentDate.plusMonths(1);
        }
        updateTitleLabel();
        drawCalendar();
    }
    
    private void updateTitleLabel() {
        if (viewSwitcher.getValue() == null) return;
        
        String view = viewSwitcher.getValue();
        if (view.contains("Week")) {
            LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            titleLabel.setText(startOfWeek.toString() + " to " + endOfWeek.toString());
        } else if (view.contains("Day")) {
            titleLabel.setText(currentDate.toString());
        } else {
            titleLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());
        }
    }

    private void drawCalendar() {
        if (searcher == null) return; // Safety check if data load failed
        
        try {
            refreshVisibleEvents();
        } catch (Exception e) {
             System.out.println("Error refreshing events: " + e.getMessage());
             e.printStackTrace();
             return;
        }

        String view = viewSwitcher.getValue();

        switch (view) {
            case "Calendar (Month)" -> drawMonthView();
            case "Calendar (Week)" -> drawWeekView();
            case "List (Day)" -> drawListDayView();
            case "List (Week)" -> drawListWeekView();
            case "List (Month)" -> drawListMonthView();
        }
    }

    // Logic inspired by ViewCalendar.showCalendarMonth
    private void drawMonthView() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        calendarGrid.getColumnConstraints().clear();
        root.setCenter(calendarGrid); 

        // Configure columns
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }
        // Configure rows
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30);
        calendarGrid.getRowConstraints().add(headerRow);
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(row);
        }

        // Add Day Headers
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        // show the previous month if first day of month is not Sunday
        int offset = (dayOfWeek == 7) ? 0 : dayOfWeek;
        LocalDate gridIterDate = firstDayOfMonth.minusDays(offset);

        for(int row = 1; row <= 6; row++){
            for(int col = 0; col < 7; col++){
                boolean isCurrentMonth = gridIterDate.getMonth().equals(firstDayOfMonth.getMonth());
                VBox cell = createDayCell(gridIterDate, true, isCurrentMonth);
                calendarGrid.add(cell, col, row);
                // col , row are coordinates
                gridIterDate = gridIterDate.plusDays(1);
            }
        }

    }

    // Logic inspired by ViewCalendar.showCalendarWeek
    private void drawWeekView() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        calendarGrid.getColumnConstraints().clear();
        root.setCenter(calendarGrid); 

        // Configure columns
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(col);
        }
        // Configure rows: Header + 1 big row
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30);
        calendarGrid.getRowConstraints().add(headerRow);
        
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        calendarGrid.getRowConstraints().add(contentRow);

        // Calculate start of week (Sunday)
        LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            // Header
            Label dayLabel = new Label(days[i] + " " + startOfWeek.plusDays(i).getDayOfMonth());
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);

            // Content
            VBox cell = createDayCell(startOfWeek.plusDays(i), false, true);
            calendarGrid.add(cell, i, 1);
        }
    }

    // Logic inspired by ViewCalendar.showListView
    private void drawListDayView() {
        eventListView.getItems().clear();
        root.setCenter(eventListView);
        
        addEventsForDateToList(currentDate);
    }

    private void drawListWeekView() {
        eventListView.getItems().clear();
        root.setCenter(eventListView);

        LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
        for (int i = 0; i < 7; i++) {
            addEventsForDateToList(startOfWeek.plusDays(i));
        }
    }

    private void drawListMonthView() {
        eventListView.getItems().clear();
        root.setCenter(eventListView);

        int length = currentYearMonth.lengthOfMonth();
        for (int i = 1; i <= length; i++) {
            addEventsForDateToList(currentYearMonth.atDay(i));
        }
    }

    private void addEventsForDateToList(LocalDate date) {
        boolean hasEvents = false;
        // Header for the day
        eventListView.getItems().add("=== " + date.toString() + " (" + date.getDayOfWeek() + ") ===");
        
        for (Event event : visibleEvents) {
            if (event.getStartDateTime().toLocalDate().equals(date)) {
                String entry = String.format("   %s - %s: %s", 
                    event.getStartDateTime().toLocalTime(), 
                    event.getTitle(), 
                    event.getDescription());
                eventListView.getItems().add(entry);
                hasEvents = true;
            }
        }
        
        if (!hasEvents) {
            eventListView.getItems().add("   No events");
        }
        eventListView.getItems().add(""); // Empty line for spacing
    }

    private VBox createDayCell(LocalDate date, boolean showDayNumber, boolean isCurrentMonth) {
        VBox cell = new VBox();
        // style: dimmer background if not current month;
        String bgStyle;
        if (isCurrentMonth) { bgStyle = "-fx-background-color: white;";}
        else { bgStyle = "-fx-background-color: #f9f9f9;"; }

        cell.getStyleClass().add("calendar-cell");
        cell.setStyle("-fx-border-color: #eeeeee; -fx-padding: 5;" + bgStyle);
        cell.setFillWidth(true);

        // click cell background to create event
        cell.setOnMouseClicked(e ->{
            // pass the date of this cell so dialog opens with date where the cell is clicked
            System.out.println("Creating event for date: " + date);
            EventDialog creatingDialog = new EventDialog(fileManager, this::drawCalendar);
            creatingDialog.create(date);
        });

        if (showDayNumber) {
            Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
            dayNumber.setMaxWidth(Double.MAX_VALUE);
            dayNumber.setAlignment(Pos.TOP_RIGHT);
            if (!isCurrentMonth){
                dayNumber.setStyle("-fx-text-fill: #aaaaaa;");
                //dimmer text for overflow days
            }
            cell.getChildren().add(dayNumber);
        }

        for (Event event : visibleEvents) {
            if (event.getStartDateTime().toLocalDate().equals(date)) {
                Label eventLabel = new Label(event.getTitle());
                eventLabel.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 2; -fx-font-size: 10px; -fx-background-radius: 3;");
                eventLabel.setMaxWidth(Double.MAX_VALUE);

                eventLabel.setOnMouseClicked(clickedEvent -> {
                    clickedEvent.consume();
                    // consume() prevent same Event in same VBox is chosen together
                    handleEventInteraction(event);
                });

                cell.getChildren().add(eventLabel);

            }

        }
        return cell;
    }

    public void handleEventInteraction(Event event){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Event details");
        alert.setHeaderText(event.getTitle());
        alert.setContentText("Description: " + event.getDescription());

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);
        ButtonType editBtn = new ButtonType("Details");
        ButtonType closeBtn = new ButtonType("Close");

        alert.getButtonTypes().setAll(deleteBtn, editBtn, closeBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent()){
            EventDialog eventManager = new EventDialog(fileManager, this::drawCalendar);
            if (result.get() == deleteBtn){
                eventManager.delete(event);

            } else if (result.get() == editBtn){
                eventManager.edit(event);
            }
        }
    }

        private void handleBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Single-File Backup");
        fileChooser.setInitialFileName("calendar_backup.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());

        if (file != null) {
            boolean success = backupManager.BackupEvents(file.getAbsolutePath());
            if (success) {
                showInfo("Backup Successful", "Backup saved to: " + file.getName());
            } else {
                showError("Backup Failed", "Could not export data.");
            }
        }
    }
        private void handleRestore() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Restore from Backup (Overwrite Current)");
        
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            // 'false' means AppendMode = off (Replace Mode)
            boolean success = backupManager.restoreEvents(file.getAbsolutePath(), false);
            
            if (success) {
                drawCalendar(); // Refresh the UI
                showInfo("Restore Successful", "Your calendar has been reset to the backup state.");
            }
        }
    }

        private void handleMerge() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Merge Backup with Current Calendar");
        
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            // 'true' means AppendMode = on (Smart Merge)
            boolean success = backupManager.restoreEvents(file.getAbsolutePath(), true);
            
            if (success) {
                drawCalendar(); // Refresh the UI
                showInfo("Merge Successful", "New events added and duplicates updated.");
            }
        }
    }

    private void showInfo(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }


}
