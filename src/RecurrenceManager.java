import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;


public class RecurrenceManager{
    public List<Event> generateOccurrences(Event base, RecurrenceRule rule, LocalDateTime searchStart, LocalDateTime searchEnd){
        List<Event> occurrences = new ArrayList<>();

        if ((rule == null) || (rule.getRecurrentInterval() == null)){
            return occurrences;
//            an empty arraylist is returned
        }
        LocalDateTime current = base.getStartDateTime();
        long durationMinutes = ChronoUnit.MINUTES.between(base.getStartDateTime(), base.getEndDateTime());

//        count of occurrences : starting at 1 for base event
        int currentCount = 1;

        int recurrentTimes = rule.getRecurrentTimes();
        LocalDateTime recurrentEndDateTime = rule.getRecurrentEndDate();

//        Step forward to reach the first event after seacrchStart datetime
        while (current.isBefore(searchStart)){
            // Check if this instance overlaps the search start window
            LocalDateTime instanceEnd = current.plusMinutes(durationMinutes);
            if (instanceEnd.isAfter(searchStart)) {
                break; // Found an overlapping instance, stop skipping
            }
            // recurrent time as variable, endDateTime = null
            if (recurrentTimes > 0 && currentCount > recurrentTimes){
                break;
            }

            // end date as variable, recurrentTime = 0

            if ((recurrentEndDateTime != null) && (current.isAfter(recurrentEndDateTime))){
                break;
            }

            current = updateCurrent(current, rule.getRecurrentInterval());
            currentCount++;

            }
        // collection of recurrence

        while (current.isBefore(searchEnd)){
            boolean hasReachLimitedTime = recurrentTimes > 0 && currentCount > recurrentTimes;
            boolean hasPassedEndDate = recurrentEndDateTime != null && current.isAfter(recurrentEndDateTime);

            if (hasReachLimitedTime || hasPassedEndDate){
                break;
            }

            LocalDateTime instanceEnd = current.plusMinutes(durationMinutes);

            occurrences.add(new Event(base, current, instanceEnd));
            current = updateCurrent(current, rule.getRecurrentInterval());
            currentCount++;

            if (occurrences.size() > 5000){
                break;
            }
        }
        return occurrences;
    }

    public LocalDateTime updateCurrent(LocalDateTime current, String interval){
        try {
            int amount = Integer.parseInt(interval.substring(0, interval.length() - 1));
            char unit = interval.toLowerCase().charAt(interval.length() - 1);
            return switch (unit) {
                case 'd' -> current.plusDays(amount);
                case 'w' -> current.plusWeeks(amount);
                case 'm' -> current.plusMonths(amount);
                case 'y' -> current.plusYears(amount);
                default -> current.plusDays(1);
            };
        }catch (Exception e){
            return current.plusDays(1);
        }
    }

}