import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SearchScene {
    private final EventSearcher searcher;
    private final CalendarGUI calendarGUI;
    private TableView<Event> resultsTable;


    public SearchScene(EventSearcher searcher, CalendarGUI calendarGUI){
        this.searcher = searcher;
        this.calendarGUI = calendarGUI;
    }

    public void show(){
        Stage searchStage = new Stage();
        searchStage.initModality(Modality.APPLICATION_MODAL);
        searchStage.setTitle("Find Events");

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f4f4f4;");

        // search parameters
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        // date range inputs (basic search)
        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker endDate = new DatePicker(LocalDate.now().plusWeeks(1));

        // advance filter inputs
        TextField keywordField = new TextField();
        keywordField.setPromptText("Title or Description");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("General", "Work", "Personal", "Study", "Holiday", "Other");
        categoryBox.setValue("General");

        TextField locationField = new TextField();
        locationField.setPromptText("Location...");

        TextField attendeesField = new TextField();
        attendeesField.setPromptText("e.g. John;Jacky");

        // layout inputs

        inputGrid.add(new Label("Start Date:"), 0, 0);
        inputGrid.add(startDate, 1, 0);
        inputGrid.add(new Label("End Date:"), 2, 0);
        inputGrid.add(endDate, 3, 0);

        inputGrid.add(new Label("Keyword"), 0, 1);
        inputGrid.add(keywordField, 1, 1);
        inputGrid.add(new Label("Category"), 2, 1);
        inputGrid.add(categoryBox, 3,1);
        inputGrid.add(new Label("Location"), 0, 2);
        inputGrid.add(locationField, 1, 2);
        inputGrid.add(new Label("Attendees"), 2, 2);
        inputGrid.add(attendeesField, 3, 2);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        // results table

        resultsTable = new TableView<>();

        // <dtype row, dtype col>
        TableColumn<Event, String> dateStartCol = new TableColumn<>("Starting Date");
        /* PropertyValueFactory
         1. takes String input eg. title
         2. capitalize first char -> Title
         3. look inside class Event for getTitle() method and apply it
        */
        dateStartCol.setCellValueFactory(new PropertyValueFactory<>("startDateTime"));

        TableColumn<Event, String> dateEndCol = new TableColumn<>("Ending Date");
        dateEndCol.setCellValueFactory(new PropertyValueFactory<>("endDateTime"));

        TableColumn<Event, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Event, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Event, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        TableColumn<Event, String> attendeesCol = new TableColumn<>("Attendees");
        attendeesCol.setCellValueFactory(new PropertyValueFactory<>("attendees"));

        //noinspection unchecked
        resultsTable.getColumns().addAll(dateStartCol, dateEndCol, titleCol, catCol, locationCol, attendeesCol);
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // interaction: double click to edit/delete
        resultsTable.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())){
                    Event rowData = row.getItem();
                    calendarGUI.handleEventInteraction(rowData);
                }
            });
            return row;
        });

        // perform search
        searchBtn.setOnAction(e -> {
            LocalDateTime start = startDate.getValue().atStartOfDay();
            LocalDateTime end = endDate.getValue().atTime(23,59);

            List<Event> basicResult = searcher.searchByDateRange(start, end);

            List<Event> finalResults = searcher.advanceFilter(
                    basicResult,
                    keywordField.getText(),
                    categoryBox.getValue(),
                    locationField.getText(),
                    attendeesField.getText()
            );
            // getItems returns an ObservableList, setAll on that list replace all its elements

            resultsTable.getItems().setAll(finalResults);
        });

        mainLayout.getChildren().addAll(new Label("Search Criteria"),
                inputGrid, searchBtn, new Separator(),
                new Label("Results"), resultsTable);
        Scene scene = new Scene(mainLayout, 800, 600);
        searchStage.setScene(scene);
        searchStage.show();

    }

}
