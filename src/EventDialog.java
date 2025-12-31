import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * class to handle both creating and editing events

 * Runnable(interface): method drawCalender calls back (execute)
 * after saving of both updated events and rules
 * why uses runnable: drawCalender has no input(parameters) or output(return), which is similar as void run() in Runnable
 * * using Runnable interface-based programming
 * avoid if-else statements: don't need to know who is calling
 * WITHOUT A CALLBACK:
 * public class EventDialog {
 *   Object parent; // Could be CalendarGUI OR SearchScene
 *   public void handleSave() {
 *     // ... save logic ...
 *   if (parent instanceof CalendarGUI) {
 *       ((CalendarGUI) parent).drawCalendar();
 *   } else if (parent instanceof SearchScene) {
 *       ((SearchScene) parent).performSearch(); }}}
 * every time created a new window that want to use this dialog
 * need to add another else if block -> hard to maintain
 */
public class EventDialog {
    private final FileManager fileManager;
    private final Runnable onSaveCallback;

    public EventDialog(FileManager fileManager, Runnable onSaveCallback){
        this.fileManager = fileManager;
        this.onSaveCallback = onSaveCallback;
    }

    public void edit(Event eventToEdit){
        prepareAndShow(eventToEdit, null);
    }
    // create event from calendar cell with date
    public void create(LocalDate initialDate){
        prepareAndShow(null, initialDate);
    }
    // create event from top bar button (default date: today)
    public void create(){
        prepareAndShow(null, LocalDate.now());
    }

    /**
     * @param eventToEdit If null: "Create mode", if provided: "Edit mode"
     */
    public void prepareAndShow(Event eventToEdit, LocalDate initialDate) {
        boolean isEditMode = (eventToEdit != null);
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle(isEditMode ? "Edit Event" : "Create Event");
        dialog.setHeaderText(isEditMode ? "Modify event details" : "Enter event details");

        ButtonType saveButtonType = new ButtonType(isEditMode ? "Update" : "Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

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

        //recurrent info
        ComboBox<String> repeatUnit = new ComboBox<>();
        repeatUnit.getItems().addAll("Do not repeat", "Daily", "Weekly", "Monthly", "Annually");
        repeatUnit.setValue("Do not repeat");

        TextField repeatFreq = new TextField("1");

        // mutual exclusive(only one exists at one time):
        // either times or endDate

        ToggleGroup endConditionGroup = new ToggleGroup();

        RadioButton timesRadio = new RadioButton("Ends after X times");
        timesRadio.setToggleGroup(endConditionGroup);
        timesRadio.setSelected(true);
        TextField repeatTimes = new TextField("1");

        RadioButton dateRadio = new RadioButton("Recurrence ends on date:");
        dateRadio.setToggleGroup(endConditionGroup);
        DatePicker recEndDatePicker = new DatePicker();

        // pre-fill with existing event details
        if (isEditMode){
            titleField.setText(eventToEdit.getTitle());
            descField.setText(eventToEdit.getDescription());
            startDatePicker.setValue(eventToEdit.getStartDateTime().toLocalDate());
            startHour.getValueFactory().setValue(eventToEdit.getStartDateTime().getHour());
            startMin.getValueFactory().setValue(eventToEdit.getStartDateTime().getMinute());

            var rules = fileManager.loadRecurrentRules();
            if (rules.containsKey(eventToEdit.getEventId())){
                RecurrenceRule rule = rules.get(eventToEdit.getEventId());
                String interval = rule.getRecurrentInterval();
                repeatFreq.setText(interval.substring(0, 1));
                char unit = interval.charAt(1);
                repeatUnit.setValue(switch (unit){
                    case 'd' -> "Daily";
                    case 'w' -> "Weekly";
                    case 'm' -> "Monthly";
                    default -> "Annually";
                });
                // time is the condition
                if (rule.getRecurrentTimes() > 0){
                    timesRadio.setSelected(true);
                    repeatTimes.setText(String.valueOf(rule.getRecurrentTimes()));
                } else if (rule.getRecurrentEndDate() != null){
                    dateRadio.setSelected(true);
                    recEndDatePicker.setValue(rule.getRecurrentEndDate().toLocalDate());
                }
            } else {
                repeatUnit.setValue("Do not repeat");
            }
        } else{
            // create mode : via clicking cell or create button
            startDatePicker.setValue(initialDate != null ? initialDate : LocalDate.now());
            endDatePicker.setValue(initialDate != null ? initialDate : LocalDate.now());
            // default condition set to repeating times
            timesRadio.setSelected(true);
            recEndDatePicker.setValue(startDatePicker.getValue().plusWeeks(1));

        }

        //logic to enable/disable based on ToggleGroup and RepeatUnit
        BooleanBinding noRepeat = repeatUnit.valueProperty().isEqualTo("Do not repeat");
        repeatFreq.disableProperty().bind(noRepeat);
        timesRadio.disableProperty().bind(noRepeat);
        dateRadio.disableProperty().bind(noRepeat);

        // if radio button is selected. If not, or if repeat is Do not repeat, disable it
        repeatTimes.disableProperty().bind(timesRadio.selectedProperty().not().or(noRepeat));
        recEndDatePicker.disableProperty().bind(dateRadio.selectedProperty().not().or(noRepeat));

        // Layout
        // (col, row)
        grid.add(new Label("Title:"), 0, 0);
        // (col, row, width(col), length(row))
        grid.add(titleField, 1, 0, 2, 1);

        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1, 2, 1);

        grid.add(new Label("Start:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(startTimeBox, 2, 2);

        grid.add(new Label("End:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(endTimeBox, 2, 3);

        grid.add(new Label("Repeat Every:"), 0, 4);
        HBox freqBox = new HBox(5, repeatFreq, repeatUnit);
        grid.add(freqBox, 1, 4, 2, 1);

        grid.add(new Separator(), 0, 5, 3, 1);
        grid.add(new Label("Stop Condition:"), 0, 6);
        grid.add(timesRadio, 1, 6);
        grid.add(repeatTimes, 2, 6);
        grid.add(dateRadio, 1, 7);
        grid.add(recEndDatePicker, 2, 7);

        dialog.getDialogPane().setContent(grid);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    List<Event> allEvent = fileManager.loadEvents();
                    var allRules = fileManager.loadRecurrentRules();

                    LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(),
                            LocalTime.of(startHour.getValue(), startMin.getValue()));
                    LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(),
                            LocalTime.of(endHour.getValue(), endMin.getValue()));

                    int ID = isEditMode ? eventToEdit.getEventId() : fileManager.getNextAvailableEventId();
                    if(isEditMode){
                        // delete old events
                        allEvent.removeIf(e -> e.getEventId() == ID);
                        allRules.remove(ID);
                    }
                    // update with new event
                    Event newEvent = new Event(ID, titleField.getText(), descField.getText(), start, end);
                    allEvent.add(newEvent);

                    // Save events to file
                    System.out.println("Saving " + allEvent.size() + " events...");
                    fileManager.saveEvents(allEvent);
                    System.out.println("Event Created and Saved: " + newEvent.getTitle());

                    // if recur
                    if (!repeatUnit.getValue().equalsIgnoreCase("Do not repeat")) {
                        char unit = switch (repeatUnit.getValue()) {
                            case "Daily" -> {
                                yield 'd';
                            }
                            case "Weekly" -> 'w';
                            case "Monthly" -> 'm';
                            case "Annually" -> 'y';
                            default -> 'd';
                        };
                        String interval = repeatFreq.getText().trim() + unit;
                        // default value
                        int times = timesRadio.isSelected() ? Integer.parseInt(repeatTimes.getText()) : 0;
                        LocalDateTime recEndDate = dateRadio.isSelected() ? recEndDatePicker.getValue().atTime(23, 59) : null;

                        // save recurrentRule to the file
                        RecurrenceRule newRule = new RecurrenceRule(ID, interval, times, recEndDate);
                        allRules.put(ID, newRule);
                        System.out.println("Saving recurrent Rules...");
                        fileManager.saveRecurrenceRule(new ArrayList<>(allRules.values()));
                    }
                    onSaveCallback.run(); // drawCalender is passed to refresh view
                } catch (Exception e) {
                    System.out.println("Error creating event: " + e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }




    // delete with confirmation popup
    public void delete(Event event){
        Alert confirm = new Alert(Alert.AlertType.WARNING,
                "Are you sure to delete '" + event.getTitle() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText(null);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES){
            try{
                List<Event> allEvent = fileManager.loadEvents();
                allEvent.removeIf(e -> e.getEventId() == event.getEventId());
                fileManager.saveEvents(allEvent);

                var rules = fileManager.loadRecurrentRules();
                if (rules.containsKey(event.getEventId())){
                    rules.remove(event.getEventId());
                    fileManager.saveRecurrenceRule(new ArrayList<>(rules.values()));
                }
                onSaveCallback.run();
            } catch (Exception e) {
                System.err.println("Delete error: " + e.getMessage());
            }

        }
    }

}
