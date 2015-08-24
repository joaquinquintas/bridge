package org.sagebionetworks.bridge.services.backfill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.sagebionetworks.bridge.dao.BackfillDao;
import org.sagebionetworks.bridge.models.accounts.Account;
import org.sagebionetworks.bridge.models.backfill.BackfillRecord;
import org.sagebionetworks.bridge.models.backfill.BackfillTask;
import org.sagebionetworks.bridge.models.studies.Study;

import com.fasterxml.jackson.databind.JsonNode;

public class BackfillRecordFactoryTest {

    @Test
    public void testCreateAndSave() {
        BackfillRecordFactory recordFactory = new BackfillRecordFactory();
        BackfillDao backfillDao = mock(BackfillDao.class);
        recordFactory.setBackfillDao(backfillDao);
        BackfillTask task = mock(BackfillTask.class);
        final String taskId = "Task ID";
        when(task.getId()).thenReturn(taskId);
        Study study = mock(Study.class);
        final String studyId = "Study ID";
        Account account = mock(Account.class);
        final String accountId = "12345";
        when(account.getId()).thenReturn(accountId);
        when(study.getIdentifier()).thenReturn(studyId);
        final String operation = "Some operation";
        recordFactory.createAndSave(task, study, account, operation);
        verify(backfillDao, times(1)).createRecord(taskId, studyId, accountId, operation);
    }

    @Test
    public void testCreateOnly() {
        BackfillRecordFactory recordFactory = new BackfillRecordFactory();
        BackfillTask task = mock(BackfillTask.class);
        final String taskId = "Task ID";
        when(task.getId()).thenReturn(taskId);
        final String message = "this is a message";
        BackfillRecord record = recordFactory.createOnly(task, message);
        assertEquals(taskId, record.getTaskId());
        assertTrue(record.getTimestamp() <= DateTime.now(DateTimeZone.UTC).getMillis());
        JsonNode node = record.toJsonNode();
        assertNotNull(node);
        assertEquals(message, node.asText());
    }

    @Test
    public void testCreateOnlyWithStudyAccount() {
        BackfillRecordFactory recordFactory = new BackfillRecordFactory();
        BackfillTask task = mock(BackfillTask.class);
        final String taskId = "Task ID";
        when(task.getId()).thenReturn(taskId);
        Study study = mock(Study.class);
        final String studyId = "Study ID";
        when(study.getIdentifier()).thenReturn(studyId);
        Account account = mock(Account.class);
        final String accountId = "Account ID";
        when(account.getId()).thenReturn(accountId);
        final String message = "message";
        BackfillRecord record = recordFactory.createOnly(task, study, account, message);
        assertEquals(taskId, record.getTaskId());
        assertTrue(record.getTimestamp() <= DateTime.now(DateTimeZone.UTC).getMillis());
        JsonNode node = record.toJsonNode();
        assertNotNull(node);
        assertEquals(studyId, node.get("study").asText());
        assertEquals(accountId, node.get("account").asText());
        assertEquals(message, node.get("message").asText());
    }
}
