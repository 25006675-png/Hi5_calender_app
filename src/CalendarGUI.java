import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
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
        
        Button printBtn = new Button("🖨");
        printBtn.setStyle("-fx-background-radius: 5px; -fx-font-size: 13px; -fx-padding: 4 8 4 8;");
        printBtn.setTooltip(new Tooltip("Print view to console"));
        printBtn.setOnAction(e -> {
            String view = viewSwitcher.getValue();
            if (view.contains("Calendar") && view.contains("Month")) {
                CalendarPrinter.printMonthCalendar(visibleEvents, currentYearMonth);
            } else if (view.contains("Calendar") && view.contains("Week")) {
                LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
                CalendarPrinter.printWeekCalendar(visibleEvents, startOfWeek);
            } else {
                LocalDate start, end;
                 if (view.contains("Week")) {
                     LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
                     start = startOfWeek;
                     end = startOfWeek.plusDays(6);
                } else if (view.contains("Month")) {
                     start = currentYearMonth.atDay(1);
                     end = currentYearMonth.atEndOfMonth();
                } else { // Day
                     start = currentDate;
                     end = currentDate;
                }
                CalendarPrinter.printList(visibleEvents, start, end, view);
            }
        });
        
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

        // Universal Date Picker for Quick Jumping
        DatePicker quickJumpPicker = new DatePicker();
        quickJumpPicker.setPrefWidth(25); 
        // Style it to look like a small icon button or minimal field
        quickJumpPicker.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-pref-width: 30px;");
        // Hack: Make the text field inside invisible so only the calendar icon is main interaction point, or just keep it small
        quickJumpPicker.getEditor().setVisible(false);
        quickJumpPicker.getEditor().setManaged(false);
        
        quickJumpPicker.setOnAction(e -> {
            LocalDate selected = quickJumpPicker.getValue();
            if (selected != null) {
                currentDate = selected;
                currentYearMonth = YearMonth.from(selected);
                updateTitleLabel();
                drawCalendar();
                // Reset to null so it can be picked again if needed? Or keep consistent.
            }
        });

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
        EventDialog creatingDialog = new EventDialog(fileManager, recurrenceManager, this::drawCalendar);

        createEventBtn.setOnAction(e -> creatingDialog.create());

        // Layout assembly
        HBox leftGrp = new HBox(5, prevBtn, titleLabel, quickJumpPicker, nextBtn, printBtn, viewSwitcher);
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
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        
        // Header
        Label header = new Label("Data Management");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Data Management Content Box
        VBox dataBox = new VBox(15);
        dataBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");
        
        // Backup & Restore Row
        HBox backupRow = new HBox(20);
        backupRow.setAlignment(Pos.CENTER_LEFT);
        Label backupLbl = new Label("Backup & Restore:");
        backupLbl.setMinWidth(140);
        backupLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 14px;");
        
        Button exportBtn = new Button("Export Data");
        exportBtn.setStyle("-fx-base: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        exportBtn.setOnAction(e -> handleBackup());
        
        Button importBtn = new Button("Import Data");
        importBtn.setStyle("-fx-base: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        importBtn.setOnAction(e -> handleRestore());
        
        backupRow.getChildren().addAll(backupLbl, exportBtn, importBtn);
        
        // External Files Row
        HBox mergeRow = new HBox(20);
        mergeRow.setAlignment(Pos.CENTER_LEFT);
        Label mergeLbl = new Label("External Files:");
        mergeLbl.setMinWidth(140);
        mergeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 14px;");
        
        Button mergeBtn = new Button("Merge CSV");
        mergeBtn.setStyle("-fx-base: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;"); 
        mergeBtn.setOnAction(e -> handleMerge());
        
        mergeRow.getChildren().addAll(mergeLbl, mergeBtn);
        
        dataBox.getChildren().addAll(backupRow, new Separator(), mergeRow);
        
        content.getChildren().addAll(header, dataBox);
        
        root.setCenter(content);
    }
    
    private VBox createSidebar() {
        VBox sidebar = new VBox(5); // Small spacing
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;"); 
        
        // Header Padding
        Region topPad = new Region();
        topPad.setMinHeight(20);
        sidebar.getChildren().add(topPad);

        // Helper to create styled sidebar buttons
        java.util.function.Function<String, Button> createBtn = (text) -> {
            Button btn = new Button(text);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(12, 20, 12, 20)); // Taller buttons
            String baseStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;";
            btn.setStyle(baseStyle);
            return btn;
        };
        
        List<Button> navButtons = new ArrayList<>();

        Button homeBtn = createBtn.apply("🏠  Home");
        Button analysisBtn = createBtn.apply("📊  Analysis");
        Button remindersBtn = createBtn.apply("🔔  Reminders");
        Button settingsBtn = createBtn.apply("⚙  Settings");

        navButtons.add(homeBtn);
        navButtons.add(analysisBtn);
        navButtons.add(remindersBtn);
        navButtons.add(settingsBtn);

        javafx.event.EventHandler<javafx.event.ActionEvent> setAsActive = e -> {
            Button source = (Button) e.getSource();
            for(Button b : navButtons) {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
            }
            // Active Style
            source.setStyle("-fx-background-color: #34495e; -fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #f1c40f; -fx-border-width: 0 0 0 4;");
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

        settingsBtn.setOnAction(e -> {
            setAsActive.handle(e);
            drawSettingsView();
        });

        // Add all to sidebar
        sidebar.getChildren().addAll(
            homeBtn, 
            analysisBtn, 
            remindersBtn, 
            settingsBtn
        );
        
        // Flexible spacer at bottom
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        sidebar.getChildren().add(bottomSpacer);
        
        // About / Version at bottom
        Label version = new Label("v1.0.0");
        version.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-padding: 10;");
        version.setMaxWidth(Double.MAX_VALUE);
        version.setAlignment(Pos.CENTER);
        sidebar.getChildren().add(version);

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
            
            int weekNum = currentDate.get(java.time.temporal.WeekFields.of(java.util.Locale.US).weekOfWeekBasedYear());
            int year = currentDate.get(java.time.temporal.WeekFields.of(java.util.Locale.US).weekBasedYear());
            
            titleLabel.setText("Week " + weekNum + ", " + year + " (" + 
                startOfWeek.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH)) + " - " + 
                endOfWeek.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH)) + ")");
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

    private void configureListView() {
        eventListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    
                    if (item.startsWith("HEADER:")) {
                        // Date Header
                        String text = item.substring(7);
                        boolean isToday = false;
                        
                        if (text.startsWith("TODAY|")) {
                            isToday = true;
                            text = text.substring(6);
                        }
                        
                        Label lbl = new Label(text);
                        String style = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 0 2 0;";
                        
                        if (isToday) {
                            style += " -fx-text-fill: #3498db; -fx-background-color: #eaf2f8; -fx-background-radius: 5; -fx-padding: 5 10 5 10;";
                            lbl.setText("Today, " + text);
                        } else {
                            style += " -fx-text-fill: #2c3e50;";
                        }
                        
                        lbl.setStyle(style);
                        VBox box = new VBox(lbl, new Separator());
                        box.setPadding(new Insets(10, 5, 0, 5));
                        setGraphic(box);
                        setStyle("-fx-background-color: transparent;");
                    } else if (item.startsWith("POINTER:")) {
                        // Event Item
                        // Format: POINTER:Time|Category|Title|Desc|ID
                        String[] parts = item.substring(8).split("\\|");
                        if (parts.length >= 4) {
                             String time = parts[0];
                             String cat = parts[1];
                             String title = parts[2];
                             String desc = parts[3];
                             
                             HBox row = new HBox(15);
                             row.setAlignment(Pos.CENTER_LEFT);
                             row.setPadding(new Insets(8, 10, 8, 10));
                             row.setStyle("-fx-background-color: white; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
                             
                             // Time Box
                             VBox timeBox = new VBox();
                             timeBox.setAlignment(Pos.CENTER);
                             timeBox.setMinWidth(60);
                             Label timeLbl = new Label(time);
                             timeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
                             timeBox.getChildren().add(timeLbl);
                             
                             // Category Indicator
                             String colorHex = "#bdc3c7";
                            switch (cat) {
                                case "Work" -> colorHex = "#e74c3c";
                                case "Personal" -> colorHex = "#9b59b6";
                                case "Study" -> colorHex = "#3498db";
                                case "Holiday" -> colorHex = "#2ecc71";
                                case "Other" -> colorHex = "#f1c40f";
                            }
                             Region indicator = new Region();
                             indicator.setMinWidth(4);
                             indicator.setMinHeight(25);
                             indicator.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 2px;");

                             // Content
                             VBox content = new VBox(2);
                             Label titleLbl = new Label(title);
                             titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                             Label descLbl = new Label(desc);
                             descLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                             content.getChildren().addAll(titleLbl, descLbl);
                             
                             row.getChildren().addAll(timeBox, indicator, content);
                             setGraphic(row);
                             setStyle("-fx-background-color: transparent; -fx-padding: 2 10 2 10;");
                             
                        } else {
                            setText(item);
                        }
                    } else {
                        // "No events" or spacers
                        Label lbl = new Label(item);
                        lbl.setStyle("-fx-text-fill: #95a5a6; -fx-padding: 5 15 5 15; -fx-font-style: italic;");
                        setGraphic(lbl);
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });
    }

    // Logic inspired by ViewCalendar.showListView
    private void drawListDayView() {
        eventListView.getItems().clear();
        configureListView();
        // Set Center of Home Layout
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);
        
        addEventsForDateToList(currentDate);
    }

    private void drawListWeekView() {
        eventListView.getItems().clear();
        configureListView();
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
        for (int i = 0; i < 7; i++) {
            addEventsForDateToList(startOfWeek.plusDays(i));
        }
    }

    private void drawListMonthView() {
        eventListView.getItems().clear();
        configureListView();
        homeCenterLayout.getChildren().setAll(homeToolbar, eventListView);
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        int length = currentYearMonth.lengthOfMonth();
        for (int i = 1; i <= length; i++) {
            addEventsForDateToList(currentYearMonth.atDay(i));
        }
    }

    private void addEventsForDateToList(LocalDate date) {
        boolean hasEvents = false;
        
        DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
        // Special format marker "HEADER:"
        String headerPrefix = "HEADER:";
        if (date.equals(LocalDate.now())) {
            headerPrefix = "HEADER:TODAY|";
        }
        eventListView.getItems().add(headerPrefix + date.format(headerFmt));
        
        for (Event event : visibleEvents) {
            if (event.getStartDateTime().toLocalDate().equals(date)) {
                // POINTER:Time|Category|Title|Desc|ID
                String packed = String.format("POINTER:%s|%s|%s|%s|%s",
                    event.getStartDateTime().toLocalTime().toString(),
                    event.getCategory(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getEventId()
                );
                eventListView.getItems().add(packed);
                hasEvents = true;
            }
        }
        
        if (!hasEvents) {
            eventListView.getItems().add("No scheduled events");
        }
        eventListView.getItems().add(""); // Empty line for spacing
    }

    private VBox createDayCell(LocalDate date, boolean showDayNumber, boolean isCurrentMonth) {
        VBox cell = new VBox();
        boolean isToday = date.equals(LocalDate.now());

        // style: dimmer background if not current month;
        String bgStyle;
        String borderStyle;
        
        if (isToday) {
            // Highlight for today
            bgStyle = "-fx-background-color: #f0f8ff;"; // Alice Blue
            borderStyle = "-fx-border-color: #3498db; -fx-border-width: 2;";
        } else if (isCurrentMonth) { 
            bgStyle = "-fx-background-color: white;";
            borderStyle = "-fx-border-color: #eeeeee;";
        } else { 
            bgStyle = "-fx-background-color: #f9f9f9;"; 
            borderStyle = "-fx-border-color: #eeeeee;";
        }

        cell.getStyleClass().add("calendar-cell");
        cell.setStyle(borderStyle + " -fx-padding: 5;" + bgStyle);
        cell.setFillWidth(true);

        // click cell background to create event
        cell.setOnMouseClicked(e ->{
            // pass the date of this cell so dialog opens with date where the cell is clicked
            System.out.println("Creating event for date: " + date);
            EventDialog creatingDialog = new EventDialog(fileManager, recurrenceManager, this::drawCalendar);
            creatingDialog.create(date);
        });

        if (showDayNumber) {
            BorderPane header = new BorderPane();
            
            Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
            
            if (isToday) {
                Label todayLbl = new Label("Today");
                todayLbl.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px;");
                header.setLeft(todayLbl);
                
                dayNumber.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            } else if (!isCurrentMonth){
                dayNumber.setStyle("-fx-text-fill: #aaaaaa;");
                //dimmer text for overflow days
            }
            
            header.setRight(dayNumber);
            cell.getChildren().add(header);
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
            EventDialog eventManager = new EventDialog(fileManager, recurrenceManager, this::drawCalendar);
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
