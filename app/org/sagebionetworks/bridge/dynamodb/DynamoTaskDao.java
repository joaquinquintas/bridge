package org.sagebionetworks.bridge.dynamodb;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.TaskDao;
import org.sagebionetworks.bridge.models.schedules.Task;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.common.collect.Lists;

@Component
public class DynamoTaskDao implements TaskDao {
    
    private static final Comparator<Task> TASK_COMPARATOR = new Comparator<Task>() {
        @Override 
        public int compare(Task task1, Task task2) {
            int result = (int)(task1.getScheduledOn() - task2.getScheduledOn());
            if (result == 0) {
                result = task1.getActivity().getLabel().compareTo(task2.getActivity().getLabel());
            }
            return result;
        }
    };
    
    private DynamoDBMapper mapper;

    @Resource(name = "taskDdbMapper")
    public void setDdbMapper(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }
    
    /** {@inheritDoc} */
    @Override
    public List<Task> getTasks(String healthCode, DateTime endsOn) {
        DynamoTask hashKey = new DynamoTask();
        hashKey.setHealthCode(healthCode);

        // Exclude everything hidden before *now*
        AttributeValue attribute = new AttributeValue().withN(Long.toString(DateTime.now().getMillis()));
        Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.GT)
            .withAttributeValueList(attribute);

        DynamoDBQueryExpression<DynamoTask> query = new DynamoDBQueryExpression<DynamoTask>()
            .withQueryFilterEntry("hidesOn", condition)
            .withHashKeyValues(hashKey);

        PaginatedQueryList<DynamoTask> queryResults = mapper.query(DynamoTask.class, query);
        List<Task> tasks = Lists.newArrayList();
        tasks.addAll(queryResults);
        Collections.sort(tasks, TASK_COMPARATOR);
        return tasks;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean taskRunHasNotOccurred(String healthCode, String runKey) {
        DynamoTask hashKey = new DynamoTask();
        hashKey.setHealthCode(healthCode);
        hashKey.setRunKey(runKey);
        
        DynamoDBQueryExpression<DynamoTask> query = new DynamoDBQueryExpression<DynamoTask>()
            .withHashKeyValues(hashKey);

        return (mapper.count(DynamoTask.class, query) == 0);
    }
    
    /** {@inheritDoc} */
    @Override
    public void saveTasks(String healthCode, List<Task> tasks) {
        if (!tasks.isEmpty()) {
            for (Task task : tasks) {
                task.setHealthCode(healthCode);
            }
            List<FailedBatch> failures = mapper.batchSave(tasks);
            BridgeUtils.ifFailuresThrowException(failures);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void updateTasks(String healthCode, List<Task> tasks) {
        List<Task> tasksToSave = Lists.newArrayList();
        for (Task task : tasks) {
            if (task != null && (task.getStartedOn() != null || task.getFinishedOn() != null)) {
                DynamoTask hashKey = new DynamoTask();
                hashKey.setHealthCode(healthCode);
                hashKey.setGuid(task.getGuid());
                Task dbTask = mapper.load(hashKey);
                
                if (dbTask != null) {
                    if (task.getStartedOn() != null) {
                        dbTask.setStartedOn(task.getStartedOn());
                        dbTask.setHidesOn(new Long(Long.MAX_VALUE));
                    }
                    if (task.getFinishedOn() != null) {
                        dbTask.setFinishedOn(task.getFinishedOn());
                        dbTask.setHidesOn(task.getFinishedOn());
                    }
                    tasksToSave.add(dbTask);
                }
            }
        }
        if (!tasksToSave.isEmpty()) {
            List<FailedBatch> failures = mapper.batchSave(tasksToSave);
            BridgeUtils.ifFailuresThrowException(failures);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteTasks(String healthCode) {
        DynamoTask hashKey = new DynamoTask();
        hashKey.setHealthCode(healthCode);

        DynamoDBQueryExpression<DynamoTask> query = new DynamoDBQueryExpression<DynamoTask>().withHashKeyValues(hashKey);
        
        PaginatedQueryList<DynamoTask> queryResults = mapper.query(DynamoTask.class, query);
        
        // Confirmed that you have to transfer these tasks to a list or the batchDelete does not work. 
        List<DynamoTask> tasksToDelete = Lists.newArrayListWithCapacity(queryResults.size());
        tasksToDelete.addAll(queryResults);
        
        if (!tasksToDelete.isEmpty()) {
            List<FailedBatch> failures = mapper.batchDelete(tasksToDelete);
            BridgeUtils.ifFailuresThrowException(failures);
        }
    }

}
