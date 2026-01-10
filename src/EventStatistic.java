import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class EventStatistic {

    private final FileManager fileManager;
    private final RecurrenceManager recurrenceManager;
    
    // UI Components
    private BorderPane rootLayout;
    private Label dateDisplayLabel;
    private ToggleGroup scopeGroup;
    private PieChart categoryPieChart;
    private Label donutCenterLabel;
    private VBox breakdownList;
    private BarChart<String, Number> hourlyBarChart;
    private XYChart.Series<String, Number> hourlyDataSeries;

    // State
    private LocalDate selectedDate; // The anchor date (e.g., first day of month, or start of week)
    private String currentScope = "Month"; // Default

    public EventStatistic(FileManager fileManager, RecurrenceManager recurrenceManager) {
        this.fileManager = fileManager;
        this.recurrenceManager = recurrenceManager;
        this.selectedDate = LocalDate.now();
        
        initializeUI();
        refreshData();
    }

    public BorderPane getView() {
        return rootLayout;
    }

    private void initializeUI() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: white;");

        // 2. The Page-Specific Toolbar
        HBox toolbar = createToolbar();
        rootLayout.setTop(toolbar);

        // 3. Content Area (Bento Box Grid)
        GridPane dashboardGrid = createDashboardGrid();
        rootLayout.setCenter(dashboardGrid);
        
        // Initial title update
        updateDateLabel();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(20);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new javafx.geometry.Insets(15, 20, 15, 20));
        toolbar.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Left: Title
        Label title = new Label("ANALYTICS DASHBOARD");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #444;");

        // Center-Left: Date Navigator
        Button prevBtn = new Button("<");
        prevBtn.setStyle("-fx-background-radius: 15; -fx-min-width: 30;");
        prevBtn.setOnAction(e -> navigateDate(-1));

        dateDisplayLabel = new Label();
        dateDisplayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 150px; -fx-alignment: center;");

        Button nextBtn = new Button(">");
        nextBtn.setStyle("-fx-background-radius: 15; -fx-min-width: 30;");
        nextBtn.setOnAction(e -> navigateDate(1));

        HBox navigator = new HBox(5, prevBtn, dateDisplayLabel, nextBtn);
        navigator.setAlignment(Pos.CENTER);

        // Center-Right: Scope Switcher
        HBox scopeSwitcher = new HBox(0);
        scopeGroup = new ToggleGroup();

        ToggleButton weekBtn = createScopeButton("Week");
        ToggleButton monthBtn = createScopeButton("Month");
        ToggleButton yearBtn = createScopeButton("Year");

        monthBtn.setSelected(true); // Default
        scopeSwitcher.getChildren().addAll(weekBtn, monthBtn, yearBtn);

        // Right: Export Button
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("Export Report");
        exportBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #3498db; -fx-text-fill: #3498db; -fx-border-radius: 4;");
        exportBtn.setOnAction(e -> System.out.println("Exporting report... (Not implemented yet)"));

        toolbar.getChildren().addAll(title, navigator, scopeSwitcher, spacer, exportBtn);
        return toolbar;
    }

    private ToggleButton createScopeButton(String text) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(scopeGroup);
        btn.setUserData(text);
        
        // CSS for segmented control look
        btn.setStyle("-fx-background-radius: 0; -fx-border-color: #ccc; -fx-border-width: 1 1 1 0;");
        // Fix borders for first/last
        if (text.equals("Week")) btn.setStyle("-fx-background-radius: 4 0 0 4; -fx-border-color: #ccc;");
        if (text.equals("Year")) btn.setStyle("-fx-background-radius: 0 4 4 0; -fx-border-color: #ccc; -fx-border-width: 1;");

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle(btn.getStyle() + "-fx-background-color: #3498db; -fx-text-fill: white;");
                currentScope = text;
                updateDateLabel();
                refreshData();
            } else {
                // Reset style (simplified)
                String baseStyle = "-fx-background-radius: 0; -fx-border-color: #ccc; -fx-border-width: 1 1 1 0;";
                 if (text.equals("Week")) baseStyle = "-fx-background-radius: 4 0 0 4; -fx-border-color: #ccc;";
                 if (text.equals("Year")) baseStyle = "-fx-background-radius: 0 4 4 0; -fx-border-color: #ccc; -fx-border-width: 1;";
                btn.setStyle(baseStyle + "-fx-background-color: white; -fx-text-fill: black;");
            }
        });
        
        // Initial style
        if (text.equals("Month")) btn.setStyle(btn.getStyle() + "-fx-background-color: #3498db; -fx-text-fill: white;");
        else btn.setStyle(btn.getStyle() + "-fx-background-color: white; -fx-text-fill: black;");

        return btn;
    }

    private GridPane createDashboardGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.setHgap(20);
        grid.setVgap(20);

        // Column constraints: Left Area (Pie) takes remaining space, Right Area (Breakdown) fixed/min width
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Row constraints
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(55); // Top half
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(45); // Bottom half
        grid.getRowConstraints().addAll(row1, row2);


        // ZONE A: Donut Chart
        VBox donutContainer = createDonutChart();
        addCardStyle(donutContainer);
        grid.add(donutContainer, 0, 0);

        // ZONE B: Breakdown List
        VBox breakdownContainer = createBreakdownList();
        addCardStyle(breakdownContainer);
        grid.add(breakdownContainer, 1, 0);

        // ZONE C: Hourly Bar Chart
        VBox barChartContainer = createHourlyBarChart();
        addCardStyle(barChartContainer);
        grid.add(barChartContainer, 0, 1, 2, 1); // Spans 2 columns

        return grid;
    }

    private VBox createDonutChart() {
        // Title Label
        Label chartTitle = new Label("Time Distribution");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        
        categoryPieChart = new PieChart();
        categoryPieChart.setLabelsVisible(false);
        categoryPieChart.setLegendVisible(false); // Hide legend to center the pie
        // Removed setTitle to separate layout

        donutCenterLabel = new Label("Total\n0h");
        donutCenterLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-alignment: center;");
        donutCenterLabel.setMouseTransparent(true); 

        // Add semi-transparent circle background
        javafx.scene.shape.Circle centerBackground = new javafx.scene.shape.Circle(60); 
        centerBackground.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.85));
        centerBackground.setMouseTransparent(true);

        StackPane stack = new StackPane(categoryPieChart, centerBackground, donutCenterLabel);
        VBox.setVgrow(stack, Priority.ALWAYS);
        
        VBox container = new VBox(chartTitle, stack);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new javafx.geometry.Insets(15));
        
        return container;
    }

    private VBox createBreakdownList() {
        VBox container = new VBox(10);
        container.setPadding(new javafx.geometry.Insets(15));
        
        Label header = new Label("Category Breakdown");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Header Row for the Leaderboard
        GridPane listHeader = new GridPane();
        listHeader.setHgap(10);
        listHeader.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 5 0;");
        // Col Constraints
        ColumnConstraints c0 = new ColumnConstraints(40); // Rank
        ColumnConstraints c1 = new ColumnConstraints(25); // Indicator
        ColumnConstraints c2 = new ColumnConstraints(); // Name
        c2.setHgrow(Priority.ALWAYS);
        c2.setMinWidth(60); 
        ColumnConstraints cTrend = new ColumnConstraints(80); // Trend
        ColumnConstraints c3 = new ColumnConstraints(60); // Count
        ColumnConstraints c4 = new ColumnConstraints(100); // Duration
        c4.setHalignment(javafx.geometry.HPos.RIGHT); // Align time right
        listHeader.getColumnConstraints().addAll(c0, c1, c2, cTrend, c3, c4);
        
        Label l0 = new Label("Rank"); l0.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label l1 = new Label("");
        Label l2 = new Label("Category"); l2.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label lTrend = new Label("Trend"); lTrend.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label l3 = new Label("No. of Events"); l3.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label l4 = new Label("Total Time"); l4.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        
        listHeader.addRow(0, l0, l1, l2, lTrend, l3, l4);


        breakdownList = new VBox(8); // List content
        ScrollPane scroll = new ScrollPane(breakdownList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        container.getChildren().addAll(header, listHeader, scroll);
        return container;
    }

    private VBox createHourlyBarChart() {
        VBox container = new VBox();
        container.setPadding(new javafx.geometry.Insets(10));
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time Period");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Duration (Hours)");

        hourlyBarChart = new BarChart<>(xAxis, yAxis);
        hourlyBarChart.setTitle("Activity vs Time");
        hourlyBarChart.setLegendVisible(false);
        hourlyBarChart.setAnimated(false); // Disable for better update performance
        
        // Initialize with 0-23 hours
        hourlyDataSeries = new XYChart.Series<>();
        for (int i = 0; i < 24; i++) {
            hourlyDataSeries.getData().add(new XYChart.Data<>(String.format("%02d:00", i), 0));
        }
        hourlyBarChart.getData().add(hourlyDataSeries);
        
        VBox.setVgrow(hourlyBarChart, Priority.ALWAYS);
        container.getChildren().add(hourlyBarChart);
        return container;
    }

    private void addCardStyle(Region region) {
        region.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
    }

    // --- Logic & Data Processing ---

    private void navigateDate(int direction) {
        switch (currentScope) {
            case "Week" -> selectedDate = selectedDate.plusWeeks(direction);
            case "Month" -> selectedDate = selectedDate.plusMonths(direction);
            case "Year" -> selectedDate = selectedDate.plusYears(direction);
        }
        updateDateLabel();
        refreshData();
    }

    private void updateDateLabel() {
        DateTimeFormatter formatter;
        String text = "";
        
        switch (currentScope) {
            case "Week" -> {
                // Calculate week range
                LocalDate startOfWeek = selectedDate.with(WeekFields.of(Locale.ENGLISH).dayOfWeek(), 1);
                LocalDate endOfWeek = startOfWeek.plusDays(6);
                int weekNum = selectedDate.get(WeekFields.of(Locale.ENGLISH).weekOfWeekBasedYear());
                text = "Week " + weekNum + " (" + startOfWeek.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + " - " + endOfWeek.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + ")";
            }
            case "Month" -> {
                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            }
            case "Year" -> {
                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH));
            }
        }
        dateDisplayLabel.setText(text);
    }

    private void refreshData() {
        // 1. Determine Scope Dates (Current vs Previous for Trend)
        LocalDateTime startDateTime, endDateTime;
        LocalDateTime prevStartDateTime, prevEndDateTime;
        
        if (currentScope.equals("Week")) {
            LocalDate startOfWeek = selectedDate.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
            startDateTime = startOfWeek.atStartOfDay();
            endDateTime = startOfWeek.plusDays(7).atStartOfDay();
            
            prevStartDateTime = startDateTime.minusWeeks(1);
            prevEndDateTime = endDateTime.minusWeeks(1);
            
            // Bar Chart Config: Daily Activity
            hourlyBarChart.setTitle("Daily Activity (This Week)");
            ((CategoryAxis)hourlyBarChart.getXAxis()).setLabel("Day of Week");
            
        } else if (currentScope.equals("Month")) {
            startDateTime = selectedDate.withDayOfMonth(1).atStartOfDay();
            endDateTime = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()).plusDays(1).atStartOfDay();
            
            prevStartDateTime = startDateTime.minusMonths(1);
            prevEndDateTime = endDateTime.minusMonths(1);
            
            // Bar Chart Config: Daily Activity
            hourlyBarChart.setTitle("Daily Activity (" + selectedDate.getMonth() + ")");
             ((CategoryAxis)hourlyBarChart.getXAxis()).setLabel("Day of Month");

        } else { // Year
            startDateTime = selectedDate.withDayOfYear(1).atStartOfDay();
            endDateTime = selectedDate.withDayOfYear(selectedDate.lengthOfYear()).plusDays(1).atStartOfDay();
            
            prevStartDateTime = startDateTime.minusYears(1);
            prevEndDateTime = endDateTime.minusYears(1);
            
            // Bar Chart Config: Monthly Activity
            hourlyBarChart.setTitle("Monthly Activity (" + selectedDate.getYear() + ")");
            ((CategoryAxis)hourlyBarChart.getXAxis()).setLabel("Month");
        }

        // 2. Fetch Events
        EventSearcher searcher = new EventSearcher(fileManager, recurrenceManager);
        List<Event> events = searcher.searchByDateRange(startDateTime, endDateTime);
        List<Event> prevEvents = searcher.searchByDateRange(prevStartDateTime, prevEndDateTime);

        // 3. Process Data for Pie & Leaderboard
        Map<String, Long> categoryDuration = new HashMap<>(); 
        Map<String, Long> categoryCount = new HashMap<>(); // New: Count events per category
        Map<String, Long> prevCategoryDuration = new HashMap<>();
        long totalMinutes = 0;

        // Current Period
        for (Event e : events) {
            long minutes = java.time.Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
            categoryDuration.merge(e.getCategory(), minutes, Long::sum);
            categoryCount.merge(e.getCategory(), 1L, Long::sum); // Increment count
            totalMinutes += minutes;
        }
        // Previous Period (for trend)
        for(Event e : prevEvents) {
            long minutes = java.time.Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
            prevCategoryDuration.merge(e.getCategory(), minutes, Long::sum);
        }

        // 4. Update Pie Chart
        categoryPieChart.getData().clear();
        List<String> orderedCategories = Arrays.asList("General", "Work", "Personal", "Study", "Holiday", "Other");
        
        // Define consistent colors for Pie slices and List indicators
        Map<String, String> categoryColors = new HashMap<>();
        categoryColors.put("General", "#bdc3c7"); // Grey
        categoryColors.put("Work", "#e74c3c");    // Red
        categoryColors.put("Personal", "#9b59b6");// Purple
        categoryColors.put("Study", "#3498db");   // Blue
        categoryColors.put("Holiday", "#2ecc71"); // Green
        categoryColors.put("Other", "#f1c40f");   // Yellow
        
        for (String cat : orderedCategories) {
            Long duration = categoryDuration.getOrDefault(cat, 0L);
            PieChart.Data slice = new PieChart.Data(cat, duration);
            categoryPieChart.getData().add(slice);
            
            long val = duration;
            long total = totalMinutes;

            // Apply Color & Interaction
            String colorHex = categoryColors.getOrDefault(cat, "#95a5a6");
            
            // Logic to apply style when node is ready
            javafx.beans.value.ChangeListener<javafx.scene.Node> nodeListener = (obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + colorHex + ";");
                    newNode.setOnMouseEntered(ev -> {
                         double percentage = (total > 0) ? (double)val/total * 100 : 0;
                        donutCenterLabel.setText(cat + "\n" + Math.round(percentage) + "%");
                    });
                    newNode.setOnMouseExited(ev -> {
                        donutCenterLabel.setText("Total\n" + (total/60) + "h");
                    });
                }
            };
            
            if (slice.getNode() != null) {
                nodeListener.changed(null, null, slice.getNode());
            } else {
                slice.nodeProperty().addListener(nodeListener);
            }
        }
        donutCenterLabel.setText("Total\n" + (totalMinutes / 60) + "h");

        // 5. Update Breakdown List (Leaderboard) with Rank
        breakdownList.getChildren().clear();

        // Ensure all categories are included
        for (String cat : orderedCategories) {
            categoryDuration.putIfAbsent(cat, 0L);
        }
        
        List<Map.Entry<String, Long>> topCategories = categoryDuration.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .filter(e -> orderedCategories.contains(e.getKey())) // Filter to only standard categories if needed, or keep all
            .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<String, Long> entry : topCategories) {
            String cat = entry.getKey();
            Long minutes = entry.getValue();
            Long count = categoryCount.getOrDefault(cat, 0L);
            
            GridPane row = new GridPane();
            row.setHgap(10);
            row.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
            
            ColumnConstraints c0 = new ColumnConstraints(40); // Rank
            ColumnConstraints c1 = new ColumnConstraints(25); // Indicator
            ColumnConstraints c2 = new ColumnConstraints(); // Name
            c2.setHgrow(Priority.ALWAYS);
            c2.setMinWidth(60);
            ColumnConstraints cTrend = new ColumnConstraints(80); // Trend
            ColumnConstraints c3 = new ColumnConstraints(60); // Count
            ColumnConstraints c4 = new ColumnConstraints(100); // Duration
            c4.setHalignment(javafx.geometry.HPos.RIGHT);

            row.getColumnConstraints().addAll(c0, c1, c2, cTrend, c3, c4);

            // 0. Rank
            Label rankLbl = new Label(String.valueOf(rank++));
            rankLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #666; -fx-alignment: center-right;");

            // 1. Indicator
            javafx.scene.shape.Circle indicator = new javafx.scene.shape.Circle(5);
            indicator.setFill(Color.web(categoryColors.getOrDefault(cat, "#95a5a6")));
            
            // 2. Name
            Label nameLbl = new Label(cat);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

            // 2.5 Trend
            long prevDur = prevCategoryDuration.getOrDefault(cat, 0L);
            double trend = 0;
            boolean hasPrev = prevCategoryDuration.containsKey(cat) && prevDur > 0;
            String trendText = "-";
            String trendStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;"; // Grey

            if (hasPrev) {
                if (minutes == prevDur) {
                    trendText = "0%";
                    trendStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;"; // Grey
                } else {
                    trend = ((double)(minutes - prevDur) / prevDur) * 100;
                    if (trend > 0) {
                        trendText = String.format("▲ %.1f%%", trend);
                        trendStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #27ae60;"; // Green
                    } else {
                        trendText = String.format("▼ %.1f%%", Math.abs(trend));
                        trendStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c0392b;"; // Red
                    }
                }
            } else if (minutes > 0) {
                 // New entry (no prev data) or prev data was 0
                 trendText = "-";
                 trendStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;"; // Grey
            }

            Label trendLbl = new Label(trendText);
            trendLbl.setStyle(trendStyle);
            
            // 3. Count
            Label countLbl = new Label(String.valueOf(count));
            countLbl.setStyle("-fx-text-fill: #666;");

            // 4. Duration
            long h = minutes / 60;
            long m = minutes % 60;
            Label timeLbl = new Label(h + "h " + m + "m");
            timeLbl.setStyle("-fx-text-fill: #333;");

            row.addRow(0, rankLbl, indicator, nameLbl, trendLbl, countLbl, timeLbl);
            breakdownList.getChildren().add(row);
        }

        // 6. Update Bar Chart
        // Clear all
        hourlyBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        // Colors for each bar (cycling)
        String[] barColors = {"#e74c3c", "#3498db", "#9b59b6", "#2ecc71", "#f1c40f", "#e67e22", "#1abc9c"};
        int barColorIndex = 0;

        if (currentScope.equals("Week")) {
            // X-Axis: Days of Week (Mon-Sun)
             // Need to ensure events are mapped to correct day
             Map<java.time.DayOfWeek, Long> daySums = new HashMap<>();
             for (Event e : events) {
                 java.time.DayOfWeek dw = e.getStartDateTime().getDayOfWeek();
                 long dur = java.time.Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
                 daySums.merge(dw, dur, Long::sum);
             }
             
             // Populate Series
             for (int i=1; i<=7; i++) {
                 java.time.DayOfWeek d = java.time.DayOfWeek.of(i);
                 long m = daySums.getOrDefault(d, 0L);
                 // Format: Mon, Tue...
                 XYChart.Data<String, Number> data = new XYChart.Data<>(d.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), m/60.0);
                 series.getData().add(data);
                 
                 // Apply color after node is created
                 final String color = barColors[(i-1) % barColors.length];
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + color + ";");
                    }
                });
             }
             
        } else if (currentScope.equals("Month")) {
            // X-Axis: Days 1..31
            Map<Integer, Long> daySums = new HashMap<>();
            for (Event e : events) {
                 int day = e.getStartDateTime().getDayOfMonth();
                 long dur = java.time.Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
                 daySums.merge(day, dur, Long::sum);
            }
             int daysInMonth = selectedDate.lengthOfMonth();
             for (int i=1; i<=daysInMonth; i++) {
                 long m = daySums.getOrDefault(i, 0L);
                 XYChart.Data<String, Number> data = new XYChart.Data<>(String.valueOf(i), m/60.0);
                 series.getData().add(data);
                 
                  final String color = barColors[(i-1) % barColors.length];
                  data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + color + ";");
                    }
                });
             }

        } else { // Year
            // X-Axis: Jan..Dec
            Map<java.time.Month, Long> monthSums = new HashMap<>();
            for (Event e : events) {
                 java.time.Month mo = e.getStartDateTime().getMonth();
                 long dur = java.time.Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
                 monthSums.merge(mo, dur, Long::sum);
            }
            int monthIdx = 0;
             for (java.time.Month mo : java.time.Month.values()) {
                 long m = monthSums.getOrDefault(mo, 0L);
                 XYChart.Data<String, Number> data = new XYChart.Data<>(mo.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), m/60.0);
                 series.getData().add(data);
                 
                  final String color = barColors[monthIdx++ % barColors.length];
                  data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + color + ";");
                    }
                });
             }
        }
        
        hourlyBarChart.getData().add(series);
    }
}
