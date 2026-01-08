public class Reminder {
    private int eventId;
    private int remindBeforeMinutes;

    public Reminder(int eventId, int remindBeforeMinutes) {
        this.eventId = eventId;
        this.remindBeforeMinutes = remindBeforeMinutes;
    }

    public int getEventId() {
        return eventId;
    }

    public int getRemindBeforeMinutes() {
        return remindBeforeMinutes;
    }
}
