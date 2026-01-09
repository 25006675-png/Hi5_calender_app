import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.swing.*;
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

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("General", "Work", "Personal", "Study", "Holiday", "Other");
        categoryBox.setEditable(true);
        categoryBox.setValue("General");

        TextField locationField = new TextField();
        TextField attendeesField = new TextField();
        attendeesField.setPromptText("Names (e.g. John; Jane)");

        // REMINDER
        ComboBox<String> reminderBox = new ComboBox<>();
        reminderBox.getItems().addAll("None", "15 minutes before", "30 minutes before", "1 hour before", "1 day before", "Custom");
        reminderBox.getSelectionModel().selectFirst();
        
        // Custom Reminder Inputs (Hidden by default)
        HBox customReminderBox = new HBox(5);
        Spinner<Integer> customValSpinner = new Spinner<>(1, 365, 1);
        customValSpinner.setEditable(true);
        customValSpinner.setPrefWidth(60);
        
        ComboBox<String> customUnitBox = new ComboBox<>();
        customUnitBox.getItems().addAll("minute before", "hour before", "day before"); // Default singular
        customUnitBox.setValue("minute before");
        
        // Listener for singular/plural
        customValSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            String currentUnit = customUnitBox.getValue();
            // Get base (e.g., "minute" from "minute before" or "minutes before")
            String base = currentUnit.split(" ")[0];
            if (base.endsWith("s")) base = base.substring(0, base.length() - 1);
            
            if (newVal > 1) {
                // Pluralize
                customUnitBox.getItems().setAll("minutes before", "hours before", "days before");
                customUnitBox.setValue(base + "s before");
            } else {
                // Singularize
                customUnitBox.getItems().setAll("minute before", "hour before", "day before");
                customUnitBox.setValue(base + " before");
            }
        });

        customReminderBox.getChildren().addAll(customValSpinner, customUnitBox);
        customReminderBox.setVisible(false);
        customReminderBox.setManaged(false); // Don't take up space when hidden

        // Send custom reminder logic
        reminderBox.setOnAction(e -> {
            boolean isCustom = "Custom".equals(reminderBox.getValue());
            customReminderBox.setVisible(isCustom);
            customReminderBox.setManaged(isCustom);
        });

        LocalTime nowTime = LocalTime.now();
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        
        // Helper to create 2-digit formatted editable spinner
        java.util.function.Function<Integer, Spinner<Integer>> createTimeSpinner = (max) -> {
            Spinner<Integer> s = new Spinner<>(0, max, 0);
            s.setEditable(true);
            s.setPrefWidth(60);
            SpinnerValueFactory.IntegerSpinnerValueFactory factory = 
                (SpinnerValueFactory.IntegerSpinnerValueFactory) s.getValueFactory();
            factory.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) {
                    return String.format("%02d", object);
                }
                @Override
                public Integer fromString(String string) {
                    try { return Integer.parseInt(string); } catch(Exception e) { return 0; }
                }
            });
            return s;
        };

        Spinner<Integer> startHour = createTimeSpinner.apply(23);
        startHour.getValueFactory().setValue(nowTime.getHour());
        
        Spinner<Integer> startMin = createTimeSpinner.apply(59);
        startMin.getValueFactory().setValue(nowTime.getMinute());
        
        HBox startTimeBox = new HBox(5, startHour, new Label(":"), startMin);
        startTimeBox.setAlignment(Pos.CENTER_LEFT);

        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        Spinner<Integer> endHour = createTimeSpinner.apply(23);
        endHour.getValueFactory().setValue(nowTime.plusHours(1).getHour()); // Default 1 hour later
        
        Spinner<Integer> endMin = createTimeSpinner.apply(59);
        endMin.getValueFactory().setValue(nowTime.getMinute());
        
        HBox endTimeBox = new HBox(5, endHour, new Label(":"), endMin);
        endTimeBox.setAlignment(Pos.CENTER_LEFT);

        // Populate if edit mode
        if (isEditMode) {
             titleField.setText(eventToEdit.getTitle());
             descField.setText(eventToEdit.getDescription());
             startDatePicker.setValue(eventToEdit.getStartDateTime().toLocalDate());
             startHour.getValueFactory().setValue(eventToEdit.getStartDateTime().getHour());
             startMin.getValueFactory().setValue(eventToEdit.getStartDateTime().getMinute());
             endDatePicker.setValue(eventToEdit.getEndDateTime().toLocalDate());
             endHour.getValueFactory().setValue(eventToEdit.getEndDateTime().getHour());
             endMin.getValueFactory().setValue(eventToEdit.getEndDateTime().getMinute());
             locationField.setText(eventToEdit.getLocation());
             categoryBox.setValue(eventToEdit.getCategory());
             attendeesField.setText(eventToEdit.getAttendees());
             
             // Load existing reminder
             ReminderManager reminderManager = new ReminderManager();
             int existingMinutes = reminderManager.getReminderMinutes(eventToEdit.getEventId());
             if (existingMinutes == 15) reminderBox.setValue("15 minutes before");
             else if (existingMinutes == 30) reminderBox.setValue("30 minutes before");
             else if (existingMinutes == 60) reminderBox.setValue("1 hour before");
             else if (existingMinutes == 1440) reminderBox.setValue("1 day before");
             else if (existingMinutes > 0) {
                 reminderBox.setValue("Custom");
                 customReminderBox.setVisible(true);
                 customReminderBox.setManaged(true);
                 if (existingMinutes % 1440 == 0) {
                     int val = existingMinutes / 1440;
                     customValSpinner.getValueFactory().setValue(val);
                     customUnitBox.setValue(val > 1 ? "days before" : "day before");
                 } else if (existingMinutes % 60 == 0) {
                     int val = existingMinutes / 60;
                     customValSpinner.getValueFactory().setValue(val);
                     customUnitBox.setValue(val > 1 ? "hours before" : "hour before");
                 } else {
                     customValSpinner.getValueFactory().setValue(existingMinutes);
                     customUnitBox.setValue(existingMinutes > 1 ? "minutes before" : "minute before");
                 }
             }
             else reminderBox.setValue("None");
        }

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

        // since end date always on the same day as start, picker is diabled
        endDatePicker.setDisable(true);
        endDatePicker.valueProperty().bind(startDatePicker.valueProperty());
        endDatePicker.setStyle("-fx-opacity: 0.7;"); // Make it look "read-only" but readable

        // pre-fill with existing event details
        if (isEditMode){
            titleField.setText(eventToEdit.getTitle());
            descField.setText(eventToEdit.getDescription());
            locationField.setText(eventToEdit.getLocation());
            categoryBox.setValue(eventToEdit.getCategory());
            attendeesField.setText(eventToEdit.getAttendees());

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
            // default condition set to repeating times
            timesRadio.setSelected(true);
            recEndDatePicker.setValue(startDatePicker.getValue().plusWeeks(1));

        }

        // Date restriction logic
        // customized date picker :for every cell(picker) , return new cell with custom rules and style

        recEndDatePicker.setDayCellFactory(picker -> new DateCell(){
            @Override
            public void updateItem(LocalDate date, boolean empty){
                super.updateItem(date, empty);  // draw default cell

                // disable if date is before Start Date
                LocalDate start = startDatePicker.getValue();
                if (date != null && start != null && date.isBefore(start)){
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // pink colour
                }
            }

        });

        // refresh end date when start date changes
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null){
                if (endDatePicker.getValue().isBefore(newVal)){
                    endDatePicker.setValue(newVal);
                }

                recEndDatePicker.setDayCellFactory(null); // clear and reset factory
                recEndDatePicker.setDayCellFactory(recEndDatePicker.getDayCellFactory());
            }
        });

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

        grid.add(new Label("Category"), 0, 4);
        grid.add(categoryBox, 1, 4);
        grid.add(new Label("Location"), 0,5 );
        grid.add(locationField, 1, 5);
        grid.add(new Label("Attendees"), 0, 6);
        grid.add(attendeesField, 1, 6);

        grid.add(new Label("Reminder:"), 0, 7);
        HBox reminderContainer = new HBox(10, reminderBox, customReminderBox);
        reminderContainer.setAlignment(Pos.CENTER_LEFT);
        grid.add(reminderContainer, 1, 7, 2, 1);

        grid.add(new Label("Repeat Every:"), 0, 8);
        HBox freqBox = new HBox(5, repeatFreq, repeatUnit);
        grid.add(freqBox, 1, 8, 2, 1);

        grid.add(new Separator(), 0, 9, 3, 1);
        grid.add(new Label("Stop Condition:"), 0, 10);
        grid.add(timesRadio, 1, 10);
        grid.add(repeatTimes, 2, 10);
        grid.add(dateRadio, 1, 11);
        grid.add(recEndDatePicker, 2, 11);

        dialog.getDialogPane().setContent(grid);
        
        // Validation: Disable submit button if title is empty
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true); // Initial state
        if (isEditMode && eventToEdit.getTitle() != null && !eventToEdit.getTitle().trim().isEmpty()) {
            saveButton.setDisable(false);
        }
        
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue.trim().isEmpty());
        });

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

                    String attendeesReplaceComma = attendeesField.getText().replace(",",";");
                    if(isEditMode){
                        // delete old events
                        allEvent.removeIf(e -> e.getEventId() == ID);
                        allRules.remove(ID);
                    }

                    // auto fix if invalid end date
                    if (end.isBefore(start)){
                        end = start.plusHours(1);
                    }
                    // update with new event
                    Event newEvent = new Event(ID, titleField.getText(), descField.getText(), start, end,
                            locationField.getText(), categoryBox.getValue(), attendeesReplaceComma);
                    allEvent.add(newEvent);

                    // Save events to file
                    System.out.println("Saving " + allEvent.size() + " events...");
                    fileManager.saveEvents(allEvent);
                    System.out.println("Event Created and Saved: " + newEvent.getTitle());

                    // SAVE REMINDER
                    ReminderManager reminderManager = new ReminderManager();
                    String reminderSelection = reminderBox.getValue();
                    int minutes = 0;
                    if (reminderSelection != null) {
                        try {
                            if ("Custom".equals(reminderSelection)) {
                                int val = customValSpinner.getValue();
                                String unit = customUnitBox.getValue();
                                if (unit.startsWith("hour")) {
                                    minutes = val * 60;
                                } else if (unit.startsWith("day")) {
                                    minutes = val * 1440; // 24 * 60
                                } else {
                                    minutes = val;
                                }
                            } else {
                                switch (reminderSelection) {
                                    case "15 minutes before" -> minutes = 15;
                                    case "30 minutes before" -> minutes = 30;
                                    case "1 hour before" -> minutes = 60;
                                    case "1 day before" -> minutes = 1440;
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Invalid custom reminder number.");
                        }
                    }
                    if (minutes > 0) {
                        reminderManager.saveReminder(newEvent.getEventId(), minutes);
                    } else {
                        // Ensure if they switched back to "None", we delete old reminder
                        reminderManager.deleteReminder(newEvent.getEventId());
                    }

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
        /* Optional act as a container to prevent nullPointerException
         * if contain any < ButtonType> , return true, else false
        */
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
                
                // Delete reminder
                new ReminderManager().deleteReminder(event.getEventId());
                
                onSaveCallback.run();
            } catch (Exception e) {
                System.err.println("Delete error: " + e.getMessage());
            }

        }
    }

}
