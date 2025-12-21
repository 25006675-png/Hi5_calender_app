import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarGUI extends Application {

    private YearMonth currentYearMonth;
    private LocalDate currentDate; // For Week/Day focus
    private List<Event> allEvents;
    private GridPane calendarGrid;
    private ListView<String> eventListView;
    private Label titleLabel;
    private TextField searchBar;
    private ComboBox<String> viewSwitcher;
    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        // Initialize state
        currentYearMonth = YearMonth.now();
        currentDate = LocalDate.now();
        
        // Load Real Data
        try {
            FileManager fileManager = new FileManager();
            allEvents = fileManager.loadEvents();
        } catch (Exception e) {
            allEvents = new ArrayList<>(); // Fallback to empty list
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
        
        eventListView = new ListView<>();

        Scene scene = new Scene(root, 1100, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load style.css: " + e.getMessage());
        }
        
        primaryStage.setTitle("Calendar App");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        drawCalendar();
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
        searchBar.setPromptText("Search events...");
        searchBar.setPrefWidth(200);
        // searchBar.textProperty().addListener((obs, oldVal, newVal) -> drawCalendar());

        // Spacer to push Create button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create Event Button
        Button createEventBtn = new Button("Create Event");
        createEventBtn.getStyleClass().add("create-event-btn");
        createEventBtn.setOnAction(e -> showCreateEventDialog());

        topBar.getChildren().addAll(
            prevBtn, nextBtn, 
            new Region() {{ setMinWidth(10); }}, // small spacer
            titleLabel, 
            new Region() {{ setMinWidth(20); }}, // spacer
            viewSwitcher, 
            new Region() {{ setMinWidth(20); }}, // spacer
            searchBar, 
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
        Button workBtn = new Button("Work");
        Button personalBtn = new Button("Personal");
        Button settingsBtn = new Button("Settings");
        
        // Push settings to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(navLabel, homeBtn, workBtn, personalBtn, spacer, settingsBtn);
        return sidebar;
    }

    private void showCreateEventDialog() {
        // This mimics the logic from EventCreator.java but in a GUI Dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Event");
        dialog.setHeaderText("Enter event details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Fields matching EventCreator.java
        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");
        
        TextField descField = new TextField();
        descField.setPromptText("Description");

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        Spinner<Integer> startHour = new Spinner<>(0, 23, 9);
        Spinner<Integer> startMin = new Spinner<>(0, 59, 0);
        startHour.setPrefWidth(60);
        startMin.setPrefWidth(60);
        HBox startTimeBox = new HBox(5, startHour, new Label(":"), startMin);
        startTimeBox.setAlignment(Pos.CENTER_LEFT);

        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        Spinner<Integer> endHour = new Spinner<>(0, 23, 10);
        Spinner<Integer> endMin = new Spinner<>(0, 59, 0);
        endHour.setPrefWidth(60);
        endMin.setPrefWidth(60);
        HBox endTimeBox = new HBox(5, endHour, new Label(":"), endMin);
        endTimeBox.setAlignment(Pos.CENTER_LEFT);

        // Layout
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0, 2, 1); 
        
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1, 2, 1);

        grid.add(new Label("Start:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(startTimeBox, 2, 2);

        grid.add(new Label("End:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(endTimeBox, 2, 3);
        
        dialog.getDialogPane().setContent(grid);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                // Logic from EventCreator.java: Create Event Object
                try {
                    LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), 
                        java.time.LocalTime.of(startHour.getValue(), startMin.getValue()));
                    LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), 
                        java.time.LocalTime.of(endHour.getValue(), endMin.getValue()));
                    
                    int newId = allEvents.size() + 1; // Simple ID generation
                    Event newEvent = new Event(newId, titleField.getText(), descField.getText(), start, end);
                    
                    allEvents.add(newEvent);
                    
                    // Save to file
                    FileManager fileManager = new FileManager();
                    System.out.println("Saving " + allEvents.size() + " events...");
                    fileManager.saveEvents(allEvents);
                    
                    System.out.println("Event Created and Saved: " + newEvent.getTitle());
                    drawCalendar(); // Refresh view
                } catch (Exception e) {
                    System.out.println("Error creating event: " + e.getMessage());
                }
                return createButtonType;
            }
            return null;
        });

        dialog.showAndWait();
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
        String view = viewSwitcher.getValue();
        
        if (view.equals("Calendar (Month)")) {
            drawMonthView();
        } else if (view.equals("Calendar (Week)")) {
            drawWeekView();
        } else if (view.equals("List (Day)")) {
            drawListDayView();
        } else if (view.equals("List (Week)")) {
            drawListWeekView();
        } else if (view.equals("List (Month)")) {
            drawListMonthView();
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
        int column = (dayOfWeek == 7) ? 0 : dayOfWeek;
        int row = 1;

        int lengthOfMonth = currentYearMonth.lengthOfMonth();

        for (int day = 1; day <= lengthOfMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            VBox cell = createDayCell(date, true);
            calendarGrid.add(cell, column, row);

            column++;
            if (column > 6) {
                column = 0;
                row++;
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
            VBox cell = createDayCell(startOfWeek.plusDays(i), false);
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
        
        for (Event event : allEvents) {
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

    private VBox createDayCell(LocalDate date, boolean showDayNumber) {
        VBox cell = new VBox();
        cell.getStyleClass().add("calendar-cell");
        cell.setStyle("-fx-border-color: #ccc; -fx-padding: 5;");
        cell.setFillWidth(true);

        if (showDayNumber) {
            Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
            dayNumber.setMaxWidth(Double.MAX_VALUE);
            dayNumber.setAlignment(Pos.TOP_RIGHT);
            cell.getChildren().add(dayNumber);
        }

        for (Event event : allEvents) {
            if (event.getStartDateTime().toLocalDate().equals(date)) {
                Label eventLabel = new Label(event.getTitle());
                eventLabel.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 2; -fx-font-size: 10px; -fx-background-radius: 3;");
                eventLabel.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(eventLabel);
            }
        }
        return cell;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
