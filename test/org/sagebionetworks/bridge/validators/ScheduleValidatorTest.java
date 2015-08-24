package org.sagebionetworks.bridge.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.bridge.exceptions.InvalidEntityException;
import org.sagebionetworks.bridge.json.DateUtils;
import org.sagebionetworks.bridge.models.schedules.Activity;
import org.sagebionetworks.bridge.models.schedules.ActivityType;
import org.sagebionetworks.bridge.models.schedules.Schedule;
import org.sagebionetworks.bridge.models.schedules.ScheduleType;
import org.sagebionetworks.bridge.models.schedules.SurveyReference;

public class ScheduleValidatorTest {

    private Schedule schedule;
    ScheduleValidator validator;
    
    @Before
    public void before() {
        schedule = new Schedule();
        validator = new ScheduleValidator();
    }
    
    @Test
    public void mustHaveAtLeastOneActivityAndAScheduleType() {
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("activities cannot be null", e.getErrors().get("activities").get(0));
            assertEquals("scheduleType cannot be null", e.getErrors().get("scheduleType").get(0));
        }
    }
    
    @Test
    public void activityMustBeFullyInitialized() {
        Activity activity = new Activity(null, null);
        
        schedule.addActivity(activity);
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("scheduleType cannot be null", e.getErrors().get("scheduleType").get(0));
            assertEquals("activities[0].activityType cannot be null", e.getErrors().get("activities[0].activityType").get(0));
            assertEquals("activities[0].ref cannot be missing, null, or blank", e.getErrors().get("activities[0].ref").get(0));
        }
    }
    
    @Test
    public void datesMustBeChronologicallyOrdered() {
        // make it valid except for the dates....
        schedule.addActivity(new Activity("Label", "task:AAA"));
        schedule.setScheduleType(ScheduleType.ONCE);

        DateTime startsOn = DateUtils.getCurrentDateTime();
        DateTime endsOn = startsOn.plusMinutes(10); // should be at least an hour later
        
        schedule.setStartsOn(startsOn);
        schedule.setEndsOn(endsOn);
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("endsOn should be at least an hour after the startsOn time", e.getErrors().get("endsOn").get(0));
        }
    }
    
    @Test
    public void surveyRelativePathIsTreatedAsTaskId() {
        schedule.addActivity(new Activity("Label", "/api/v1/surveys/AAA/revisions/published"));
        schedule.setScheduleType(ScheduleType.ONCE);
        
        DateTime now = DateUtils.getCurrentDateTime();
        schedule.setStartsOn(now);
        schedule.setEndsOn(now.plusHours(1));
        
        assertEquals(ActivityType.TASK, schedule.getActivities().get(0).getActivityType());
    }
    
    @Test
    public void activityCorrectlyParsesPublishedSurveyPath() {
        Activity activity = new Activity("Label", "https://server/api/v1/surveys/AAA/revisions/published");
        
        SurveyReference ref = activity.getSurvey();
        assertEquals("AAA", ref.getGuid());
        assertNull(ref.getCreatedOn());
        
        activity = new Activity("Label", "task:AAA");
        assertNull(activity.getSurvey());
        
        activity = new Activity("Label", "https://server/api/v1/surveys/AAA/revisions/2015-01-27T17:46:31.237Z");
        ref = activity.getSurvey();
        assertEquals("AAA", ref.getGuid());
        assertEquals(DateTime.parse("2015-01-27T17:46:31.237Z"), ref.getCreatedOn());
    }

    @Test
    public void rejectsScheduleWithTwoBothCronTriggerAndInterval() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setInterval("P2D");
        schedule.setCronTrigger("0 0 12 ? * TUE,THU,SAT *");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("Schedule recurring schedules should have either a cron expression, or an interval, but not both", e.getErrors().get("Schedule").get(0));
        }
    }
    
    @Test
    public void recurringSchedulesHasNeitherCronTriggerNorInterval() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("Schedule recurring schedules should have either a cron expression, or an interval, but not both", e.getErrors().get("Schedule").get(0));
        }
        
        schedule.setInterval("P1D");
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertNull(e.getErrors().get("Schedule"));
        }
    }
    
    @Test
    public void expiresTooShort() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.setExpires("PT30M");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("expires must be at least one hour", e.getErrors().get("expires").get(0));
        }
    }
    
    @Test
    public void rejectsInvalidCronExpression() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setCronTrigger("2 3 a");

        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("cronTrigger is an invalid cron expression", e.getErrors().get("cronTrigger").get(0));
        }
    }
    
    /**
     * It's ambiguous what this means when longer intervals require a time of day as well...the interval, delay, and  
     * times can all conflict with one another.
     */
    @Test
    public void rejectsInvervalOfLessThanADay() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setInterval("PT30M");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("interval must be at least one day", e.getErrors().get("interval").get(0));
        }
    }
    
    @Test 
    public void rejectOneTimeTaskWithInterval() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.setInterval("P2D");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("Schedule executing once should not have an interval", e.getErrors().get("Schedule").get(0));
        }
    }
    
    @Test
    public void schedulesWithIntervalsShouldHaveTimesOfDay() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setInterval("P2D");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("times are required for interval-based schedules", e.getErrors().get("times").get(0));
        }
    }
    
    @Test
    public void intervalsShouldBeMoreThanADayLong() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.RECURRING);
        schedule.setInterval("PT23H59M");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("interval must be at least one day", e.getErrors().get("interval").get(0));
        }
    }
    
    @Test
    public void delaysWithTimesOfDayMustBeMoreThanOneDayLong() {
        Schedule schedule = new Schedule();
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.addTimes("08:00");
        schedule.setDelay("PT5H");
        
        try {
            Validate.entityThrowingException(validator, schedule);
            fail("Should have thrown InvalidEntityException");
        } catch(InvalidEntityException e) {
            assertEquals("delay is less than one day, and times of day are also set for this schedule, which is ambiguous", e.getErrors().get("delay").get(0));
        }
    }
    
}
