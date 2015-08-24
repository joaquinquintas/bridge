package org.sagebionetworks.bridge.models.schedules;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.joda.time.DateTime;

public class ScheduleTestUtils {
    
    /**
     * Create a DateTime from a string like "2015-05-01 20:00".
     * @param string
     * @return
     */
    public static DateTime asDT(String string) {
        return DateTime.parse(string.replace(" ", "T") + ":00Z");
    }
    
    /**
     * Create a DateTime from a string like "2015-05-01 20:00".
     * @param string
     * @return
     */
    public static Long asLong(String string) {
        return asDT(string).getMillis();
    }
    
    /**
     * Assert that a list of tasks has the list of startsOn dates, in the 
     * order specified.
     * @param tasks
     * @param output
     */
    public static void assertDates(List<Task> tasks, String... output) {
        assertEquals(output.length, tasks.size());
        for (int i=0; i < tasks.size(); i++) {
            assertEquals(asLong(output[i]), tasks.get(i).getScheduledOn());
        }
    }
}
