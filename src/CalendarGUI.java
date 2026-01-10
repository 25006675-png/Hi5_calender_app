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
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.animation.AnimationTimer;

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
    
    // UI Containers
    private HBox homeToolbar;
    private VBox homeCenterLayout;
    
    // Notification History
    private List<String> notificationHistory = new ArrayList<>();
    private Set<String> notifiedEventIds = new HashSet<>();
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
        
        // Global App Bar (Always at top)
        root.setTop(createGlobalAppBar());

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Center: Home View by default
        calendarGrid = new GridPane();
        calendarGrid.getStyleClass().add("calendar-grid");
        
        eventListView = new ListView<>();
        
        // Initialize Home Toolbar
        createHomeToolbar();
        
        homeCenterLayout = new VBox();
        

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
                // Check events for the long range to catch widely set reminders
                List<Event> allEvents = searcher.searchByDateRange(now.minusMinutes(60), now.plusYears(1)); 
                List<Reminder> reminders = ReminderFileReader.loadReminders("data/reminder.csv");
                
                if (reminders != null && !reminders.isEmpty()) {
                    List<ReminderService.ReminderNotification> newReminders = ReminderService.getReminders(allEvents, reminders);
                    
                    if (!newReminders.isEmpty()) {
                        javafx.application.Platform.runLater(() -> processNotifications(newReminders));
                    }
                }
            } catch (Throwable e) {
                System.err.println("Scheduler Error: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void processNotifications(List<ReminderService.ReminderNotification> notifications) {
         boolean alertNeeded = false;
         StringBuilder elementString = new StringBuilder();
         String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

         for (ReminderService.ReminderNotification n : notifications) {
             String key = n.getEvent().getEventId() + "_" + n.getEvent().getStartDateTime();
             if (!notifiedEventIds.contains(key)) {
                 notifiedEventIds.add(key);
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


    private HBox createGlobalAppBar() {
        HBox globalBar = new HBox();
        globalBar.getStyleClass().add("global-app-bar");
        globalBar.setAlignment(Pos.CENTER);
        globalBar.setPadding(new Insets(10));
        // Changed color to #2c3e50's complementary or darker shade -> #1a252f (Darker) or #3498db (Accent)
        // User wants it DIFFERENT from left bar (#2c3e50).
        // Let's us #34495e (Light Asphalt) or #1abc9c
        globalBar.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");

        // Left: Logo
        Label logoLabel = new Label("Calendar App");
        logoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Center: Live Clock
        Label clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        // Force English Locale
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy | h:mm a", Locale.ENGLISH);
        
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                clockLabel.setText(LocalDateTime.now().format(timeFormatter));
            }
        };
        timer.start();
        
        // Right: User Profile (REMOVED as requested)
        // Label profileIcon = new Label("👤"); 

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        globalBar.getChildren().addAll(logoLabel, leftSpacer, clockLabel, rightSpacer);
        return globalBar;
    }

    private void createHomeToolbar() {
        homeToolbar = new HBox(10); // Adding spacing
        homeToolbar.getStyleClass().add("top-bar");
        homeToolbar.setAlignment(Pos.CENTER_LEFT);
        homeToolbar.setPadding(new Insets(10)); // Add padding

        // Navigation Buttons
        Button prevBtn = new Button("<");
        prevBtn.setStyle("-fx-background-radius: 5px;"); // Rounded
        prevBtn.setOnAction(e -> previousMonth());
        
        Button nextBtn = new Button(">");
        nextBtn.setStyle("-fx-background-radius: 5px;");
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
        viewSwitcher.setStyle("-fx-background-radius: 5px;");
        viewSwitcher.setOnAction(e -> {
            updateTitleLabel();
            drawCalendar();
        });
        
        // Title
        titleLabel = new Label();
        titleLabel.getStyleClass().add("title-label");
        // Ensure bold and size from style.css actually works or override
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        updateTitleLabel();

        // Search Bar (Logic removed as requested, just UI)
        searchBar = new TextField();
        searchBar.setPromptText("Search keywords...");
        searchBar.setPrefWidth(200);
        searchBar.setStyle("-fx-background-radius: 15px; -fx-padding: 5 10 5 10;"); // Rounded pill
        searchBar.textProperty().addListener((obs, oldVal, newVal) -> drawCalendar());

        Button advancedSearchBtn = new Button("Advanced Search");
        advancedSearchBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-underline: true;");
        advancedSearchBtn.setOnAction(e -> new SearchScene(searcher, this).show());

        // Spacer to push Create button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create Event Button
        Button createEventBtn = new Button("+ Create Event");
        createEventBtn.getStyleClass().add("create-event-btn");
        createEventBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5px; -fx-font-weight: bold;");
        EventDialog creatingDialog = new EventDialog(fileManager, this::drawCalendar);

        createEventBtn.setOnAction(e -> creatingDialog.create());

        // Layout assembly
        HBox leftGrp = new HBox(5, prevBtn, titleLabel, nextBtn, viewSwitcher);
        leftGrp.setAlignment(Pos.CENTER_LEFT);
        
        homeToolbar.getChildren().addAll(
            leftGrp,
            new Region() {{ setMinWidth(20); }},
            searchBar,
            advancedSearchBtn,
            spacer, 
            createEventBtn
        );
    }

    private void drawSettingsView() {
        VBox settingsContainer = new VBox(20);
        settingsContainer.setPadding(new Insets(20));
        settingsContainer.setAlignment(Pos.TOP_LEFT);
        
        // Toolbar for settings
        HBox toolbar = new HBox();
        Label title = new Label("SETTINGS");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        toolbar.getChildren().add(title);
        
        // Content
        Label content = new Label("Application preferences and account configurations will appear here.");
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        settingsContainer.getChildren().addAll(toolbar, new Separator(), content);
        
        root.setCenter(settingsContainer);
    }
    
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;"); 

        // Helper to create styled sidebar buttons
        java.util.function.Function<String, Button> createBtn = (text) -> {
            Button btn = new Button(text);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(10, 20, 10, 20));
            String baseStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;";
            btn.setStyle(baseStyle);
            return btn;
        };
        
        List<Button> navButtons = new ArrayList<>();

        Button homeBtn = createBtn.apply("Home");
        Button analysisBtn = createBtn.apply("Analysis");
        Button remindersBtn = createBtn.apply("Reminders");

        navButtons.add(homeBtn);
        navButtons.add(analysisBtn);
        navButtons.add(remindersBtn);

        javafx.event.EventHandler<javafx.event.ActionEvent> setAsActive = e -> {
            Button source = (Button) e.getSource();
            for(Button b : navButtons) {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
            }
            source.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px;");
        };

        homeBtn.setOnAction(e -> {
            setAsActive.handle(e);
            drawCalendar();
        });

        analysisBtn.setOnAction(e -> {
             setAsActive.handle(e);
             drawAnalysisView();
        });
        
        remindersBtn.setOnAction(e -> {
            setAsActive.handle(e);
            drawNotificationView();
        });

        // Other buttons
        Button settingsBtn = new Button("Settings");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setOnAction(e -> drawSettingsView());

        Button backupBtn = new Button("Export Backup");
        backupBtn.setMaxWidth(Double.MAX_VALUE);
        backupBtn.setOnAction(e -> handleBackup());
        
        Button restoreBtn = new Button("Import Restore");
        restoreBtn.setMaxWidth(Double.MAX_VALUE);
        restoreBtn.setOnAction(e -> handleRestore());

        Button mergeBtn = new Button("Merge/Import CSV");
        mergeBtn.setMaxWidth(Double.MAX_VALUE);
        mergeBtn.setOnAction(e -> handleMerge());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(
            new Region() {{ setMinHeight(20); }},
            homeBtn, analysisBtn, remindersBtn, 
            new Separator() {{ setPadding(new Insets(10,0,10,0)); }}, 
            backupBtn, restoreBtn, mergeBtn, 
            spacer, 
            settingsBtn
        );
        return sidebar;
    }

    private void drawAnalysisView() {
        EventStatistic stats = new EventStatistic(fileManager, recurrenceManager);
        root.setCenter(stats.getView());
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

        // Ensure Home View is Active
        // If center is not homeCenterLayout? 
        if (root.getCenter() != homeCenterLayout) {
             root.setCenter(homeCenterLayout);
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
        
        // Set Center of Home Layout, NOT root
        homeCenterLayout.getChildren().setAll(homeToolbar, calendarGrid);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);

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
        
        // Ensure Center is updated (redundant if done at top, but safe)
        homeCenterLayout.getChildren().setAll(homeToolbar, calendarGrid);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
    }

    // Logic inspired by ViewCalendar.showCalendarWeek
    private void drawWeekView() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        calendarGrid.getColumnConstraints().clear();
        
        homeCenterLayout.getChildren().setAll(homeToolbar, calendarGrid);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);

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
        // Set Center of Home Layout
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);
        
        addEventsForDateToList(currentDate);
    }

    private void drawListWeekView() {
        eventListView.getItems().clear();
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
        for (int i = 0; i < 7; i++) {
            addEventsForDateToList(startOfWeek.plusDays(i));
        }
    }

    private void drawListMonthView() {
        eventListView.getItems().clear();
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);

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
                
                // Color Logic based on Category
                String colorHex = "#bdc3c7"; // Default Grey
                switch (event.getCategory()) {
                    case "Work" -> colorHex = "#e74c3c";
                    case "Personal" -> colorHex = "#9b59b6";
                    case "Study" -> colorHex = "#3498db";
                    case "Holiday" -> colorHex = "#2ecc71";
                    case "Other" -> colorHex = "#f1c40f";
                    case "General" -> colorHex = "#bdc3c7";
                }
                
                eventLabel.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-padding: 2; -fx-font-size: 10px; -fx-background-radius: 3; -fx-font-weight: bold;");
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
