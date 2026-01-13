# Hi5 Calendar App

A lightweight Java desktop calendar application built with JavaFX. It supports creating and editing events, recurring rules, reminders, searching & filtering, analytics, conflict detection, and backup/restore. The app stores its data as CSV files in a local `data/` folder so it is simple to inspect and back up.

This README explains the features, how the code is organized, how to build and run the app, and how to use backups and other utilities included in the repository.

---

## Table of contents

- [Features](#features)
- [Repository structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Build & run (quick)](#build--run-quick)
- [Detailed run instructions (CLI & IDE)](#detailed-run-instructions-cli--ide)
  - [Compile and run using command line (Linux / macOS)](#compile-and-run-using-command-line-linux--macos)
  - [Compile and run using command line (Windows)](#compile-and-run-using-command-line-windows)
  - [Run from an IDE (IntelliJ IDEA / Eclipse)](#run-from-an-ide-intellij-idea--eclipse)
  - [Create an executable JAR (optional)](#create-an-executable-jar-optional)
- [Data files & format](#data-files--format)
- [Backup & restore](#backup--restore)
- [How features work (developer overview)](#how-features-work-developer-overview)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- Calendar views
  - Month view (visual calendar grid)
  - Week view
  - Day / List view (day/week/month list-style)
- Full event CRUD (Create / Read / Update / Delete)
  - Title, description, date/time, location, category, attendees
- Recurrence support
  - Recurrence rules (intervals like `1d`, `1w`, `1m`, `1y`)
  - End by count or end date
  - Recurrence expansion bounded to avoid infinite generation
- Reminders
  - Save reminders in minutes before event
  - Configurable presets and custom reminders in UI
- Conflict detection
  - Checks time overlap for single events and generated recurrence instances
- Search & Advanced Filtering
  - Date range search
  - Keyword, category, location, attendees filters
- Analytics / Statistics
  - Pie/donut chart by category
  - Bar charts for daily / monthly / hourly activity
  - Leaderboard-style breakdown and trend comparisons
- Backup & Restore
  - Single-file backup combining events, recurrence rules, and additional fields
  - Append (merge) or Replace restore modes
  - ID mapping when merging (prevents ID conflicts)
- Utilities
  - Console calendar printing helper (CalendarPrinter)
  - Reminder service that produces notifications (logic separated from GUI)
- Self-healing: creates `data/` folder and CSV headers if they are missing

---

## Repository structure (important files)

- `src/` — Java source files
  - `App.java` — simple launcher (sets locale, launches the JavaFX app)
  - `CalendarGUI.java` — main JavaFX UI
  - `FileManager.java` — persistent CSV read/write; handles `data/` folder and `event.csv`, `recurrent.csv`, `additional.csv`
  - `Event.java` — event model
  - `RecurrenceRule.java` — recurrence rule model and CSV parsing
  - `RecurrenceManager.java` — generate recurrence occurrences
  - `EventSearcher.java` — find events and filter them
  - `ConflictDetector.java` — checks time overlaps
  - `ReminderManager.java`, `ReminderFileReader.java`, `Reminder.java` — reminder persistence and helpers
  - `BackupManager.java` — single-file backup/restore helpers
  - `CalendarPrinter.java` — console printer utilities
  - `EventDialog.java` — event create/edit dialog logic
  - `EventStatistic.java` — analytics dashboard
  - `SearchScene.java` — search UI
- `src/style.css` — basic styling for UI
- `data/` — runtime directory created by the app (contains CSV files)

---

## Prerequisites

- Java Development Kit (JDK) 11 or later (Java 17 recommended).
- JavaFX SDK matching your JDK (JavaFX is no longer bundled with the JDK).
  - Download from: https://gluonhq.com/products/javafx/
  - You will use the `lib` directory inside the JavaFX SDK for `--module-path`.

Notes:
- Use a JavaFX SDK version compatible with your JDK (e.g., JavaFX 17 with JDK 17).
- On Linux/macOS the module path separator is `:`. On Windows it is `;`.

---

## Build & run (quick)

1. Download and extract JavaFX SDK.
2. From project root run (adjust paths to your JavaFX SDK):

Linux/macOS example:
```bash
javac -d out src/*.java
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp out App
```

Windows example (PowerShell or CMD):
```powershell
javac -d out src\*.java
java --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp out App
```

App entry point is `App` which calls `Application.launch(CalendarGUI.class, args)`.

See the next section for detailed examples, including IDE instructions.

---

## Detailed run instructions (CLI & IDE)

### Compile and run using command line (Linux / macOS)

1. Create a build output directory:
   ```bash
   mkdir -p out
   ```
2. Compile:
   ```bash
   javac -d out src/*.java
   ```
3. Run:
   ```bash
   java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp out App
   ```
   Replace `/path/to/javafx-sdk/lib` with the actual path where JavaFX `lib` JARs are located.

If you want to run a specific main class:
- `App` is the recommended launcher. `CalendarGUI` also contains a `main` method and can be launched directly.

### Compile and run using command line (Windows)

PowerShell / CMD:

1. Compile:
   ```powershell
   mkdir out
   javac -d out src\*.java
   ```
2. Run:
   ```powershell
   java --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp out App
   ```

Make sure you escape or quote paths with spaces.

### Run from an IDE (IntelliJ IDEA / Eclipse)

- Create a new Java project and add the `src/` directory as Sources.
- In project SDK settings, point to your JDK (11+).
- Add JavaFX libraries to the project's module path / classpath:
  - In IntelliJ: Project Structure > Libraries > + > Java and add all JARs from JavaFX `lib` folder. Then in Run Configuration, add VM options:
    ```
    --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
    ```
- Run the `App` class (or `CalendarGUI#main()`).

### Create an executable JAR (optional)

1. Compile and package classes:
   ```bash
   javac -d out src/*.java
   jar --create --file hi5-calendar.jar -C out .
   ```
2. Run with JavaFX modules:
   ```bash
   java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar hi5-calendar.jar
   ```

Note: When bundling into a platform-distributable package, consider using jlink or packaging tools that bundle the Java runtime and JavaFX modules.

---

## Data files & format

At runtime the application creates and uses a `data/` folder containing CSV files. The main files are:

- `data/event.csv`
  - Header: `eventId,title,description,startDateTime,endDateTime,location,category,attendees`
  - `startDateTime` and `endDateTime` are ISO-8601 like `yyyy-MM-ddTHH:mm` (e.g., `2026-01-12T09:30`).
  - `eventId` is an integer identifier.

- `data/recurrent.csv`
  - Header: `eventId, recurrentInterval, recurrentTimes, recurrentEndDate`
  - `recurrentInterval` example: `1d` (every 1 day), `2w` (every 2 weeks), `1m`, `1y`.
  - `recurrentTimes` is an integer count (0 means unlimited until end date).
  - `recurrentEndDate` is `0` (none) or ISO-8601 `LocalDateTime` string.

- `data/additional.csv`
  - Header: `eventId,location,category,attendees`
  - This file holds additional fields (keeps `event.csv` writing simpler).

- `data/reminder.csv`
  - Header: `eventId, minutesBefore`
  - Stores reminders in minutes before event start.

File read/write is implemented with simple CSV splitting by comma. Be careful if fields include commas — the current code does not support quoted CSV fields. Use semicolons for attendee lists (the UI suggests `John;Jane`).

---

## Backup & restore

There is a single-file backup format (text) produced/consumed by `BackupManager`:

- Backup file markers used:
  - `---EVENTS---`
  - `---RECURRENCES---`
  - `---ADDITIONAL---`

The backup file layout:
- Marker `---EVENTS---` then event CSV lines (header may be skipped)
- Marker `---RECURRENCES---` then recurrence CSV lines
- Marker `---ADDITIONAL---` then additional CSV lines

Restore modes:
- Append (merge): merges events from backup into the current calendar; if duplicate events exist they are not duplicated — a translation ID map is created to remap recurrence/additional entries to new IDs. Recurrence rules and additional rows are remapped and appended. This is useful when you want to combine backups from another machine or a previous export.
- Replace: replaces all current data with the backup. This will overwrite `event.csv` and `recurrent.csv` (and `additional.csv`) using the data parsed from the backup.

Important notes:
- During merge the `FileManager` will avoid duplicate events by detecting matches using title & start timestamp.
- If you rely on `eventId` references outside CSV files, make sure your backup/restore strategy keeps them consistent.

---

## How features work (developer overview)

- Events
  - `Event` is the model with a few constructors:
    - full constructor (used by FileManager)
    - copy-constructor for recurrence-generated instances
    - constructor from `String[]` (used when parsing CSV lines)
  - `toCsvString()` and `toAdditionalCsv()` used for writing CSVs

- Recurrence
  - `RecurrenceRule` stores recurrence metadata.
  - `RecurrenceManager.generateOccurrences()` expands a base event over a search range using the interval encoded as `Nd`, `Nw`, `Nm`, `Ny`.
  - Expansion is constrained (counts, endDate, or a maximum number of occurrences).

- Searching & conflict detection
  - `EventSearcher.searchByDateRange()` loads base events and recurrence rules and returns all occurrences that overlap a provided date-time range.
  - `ConflictDetector` asks `EventSearcher` for events overlapping a candidate event/rule. For recurring events, it generates occurrences for up to one year by default to check collisions.

- Reminders
  - Reminders stored in `data/reminder.csv`. `ReminderService.getReminders()` accepts a list of events and reminders and returns reminder notifications (not shown as dialogs by the service itself — the GUI consumes the returned notifications).
  - `ReminderManager` is the persistence helper to save or delete reminders.

- Persistence
  - `FileManager` is the single place for read/write operations and helpers such as `mergeAndSaveBackup()`, `appendRecurrences()`, and `appendAdditional()` for backup merges.

- UI
  - Built with JavaFX (controls and charts).
  - `CalendarGUI` orchestrates the views, toolbar, search, reminders, and backup/restore dialogs.
  - `EventDialog` contains the create/edit workflow including validation and conflict checks.

---

## Troubleshooting & FAQ

- JavaFX runtime errors (NoClassDefFoundError, javafx not found)
  - Ensure you downloaded JavaFX SDK and are passing `--module-path` and `--add-modules` when launching.
  - Example:
    ```
    java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp out App
    ```

- Blank window or GUI not showing
  - Make sure the right JavaFX modules are included and the JDK/JRE bitness matches the JavaFX SDK you downloaded (both 64-bit typically).

- CSV parsing issues (fields with commas)
  - Current code splits CSV lines by comma. If your titles/descriptions contain commas, fields may break. Avoid commas inside fields or extend CSV parsing to use a proper CSV library (e.g., OpenCSV) or a quoting scheme.

- Reminders are not firing
  - Reminders are produced by checking "events starting within X minutes". The GUI is responsible for scheduling repeated checks (see `ReminderService.getReminders()` usage in GUI). Ensure the GUI's scheduled executor is running.

- Backup merge created duplicate additional rows
  - If you repeatedly append the same backup file without replace or ID remapping, duplicate additional rows may occur. The merge logic attempts to remap by event title & start time and only remaps additional rows when the original event is found. Still, duplicates might appear if multiple similar events exist.

---

## Contributing

Contributions, bug reports and pull requests are welcome.

If you want to:
- Fix CSV quoting & parsing using a proper CSV library.
- Add i18n (currently defaults to English).
- Add packaging scripts (jlink / native installers) for each platform.
- Improve UI accessibility and keyboard navigation.

Please open an issue describing the change, then a PR with clear commit messages.

---

## Known limitations & future improvements

- CSV splitting using `String.split(",")` does not support quoted commas.
- Recurrence engine is intentionally simple — advanced rules (like "every last weekday") are not supported.
- Export/Import formats besides the custom single-file backup are not implemented.
- No user authentication — local single-user usage only.
- Mobile or web clients not included — desktop JavaFX only.

---

## License

MIT License — see LICENSE (or create one if you want to apply a different license).

---

## Contact / Author

Repository: 25006675-png/Hi5_calender_app  
Author: (project owner in repository)  

If you have questions or want help running the project on your platform, open an issue in the repository and include:
- OS and Java version (`java -version`)
- JavaFX SDK version
- Exact command you ran and the error output (if any)

---

Thank you for checking out Hi5 Calendar App — enjoy organizing your schedule!
