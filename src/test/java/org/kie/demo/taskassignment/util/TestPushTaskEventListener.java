package org.kie.demo.taskassignment.util;

import java.util.List;

import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPushTaskEventListener extends DefaultTaskEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TestPushTaskEventListener.class);

    private List<TaskPlanningEntity> tasks;

    public TestPushTaskEventListener(List<TaskPlanningEntity> tasks) {
        this.tasks = tasks;
    }

    /*
    * Conversion to planning entities is done in every event listener
    *
    * */

    @Override
    public void afterTaskAddedEvent(TaskEvent event) {
        logger.debug("Task has been added " + event.getTask().getName() + " " + event.getTask().getId());
        // no need to load task input variables since they are available immediately in this type of event
        tasks.add(mapTaskToPlanningEntity(event.getTask()));
    }

    @Override
    public void afterTaskClaimedEvent(TaskEvent event) {
        logger.debug("Task has been claimed " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());
        TaskPlanningEntity newTask = mapTaskToPlanningEntity(event.getTask());
        int taskIndex = tasks.indexOf(newTask);
        tasks.set(taskIndex, newTask);
    }

    @Override
    public void afterTaskStartedEvent(TaskEvent event) {
        logger.debug("Task has been started " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());
        TaskPlanningEntity newTask = mapTaskToPlanningEntity(event.getTask());
        int taskIndex = tasks.indexOf(newTask);
        tasks.set(taskIndex, newTask);
    }

    @Override
    public void afterTaskCompletedEvent(TaskEvent event) {
        logger.debug("Task has been completed " + event.getTask().getName() + " " + event.getTask().getId());
        event.getTaskContext().loadTaskVariables(event.getTask());
        tasks.remove(mapTaskToPlanningEntity(event.getTask()));
    }

    @Override
    public void afterTaskExitedEvent(TaskEvent event) {
        // Task is exited when there is a cancelCase or a destroyCase command called
        logger.debug("Task has been exited " + event.getTask().getName() + " " + event.getTask().getId());
        event.getTaskContext().loadTaskVariables(event.getTask());
        tasks.remove(mapTaskToPlanningEntity(event.getTask()));
    }

    private static TaskPlanningEntity mapTaskToPlanningEntity(Task engineTask) {
        TaskPlanningEntity taskPlanningEntity = new TaskPlanningEntity();

        taskPlanningEntity.setId(engineTask.getId());
        // engineTask.getName() cannot be used as this returns the node name and then we cannot use a signal to a custom-named node, e.g. #{caseFile_TaskName}
        taskPlanningEntity.setName(engineTask.getTaskData().getTaskInputVariables().get("TaskName").toString());

        User actualOwner = engineTask.getTaskData().getActualOwner();
        taskPlanningEntity.setActualOwner( actualOwner != null ? actualOwner.getId() : null);

        // fill potential users and potential groups
        engineTask.getPeopleAssignments().getPotentialOwners().forEach(organizationalEntity -> {
            if (organizationalEntity instanceof User) {
                taskPlanningEntity.addPotentialUserOwner(organizationalEntity.getId());
            } else {
                taskPlanningEntity.addPotentialGroupOwner(new Group(organizationalEntity.getId()));
            }
        });

        // setBaseDuration
        taskPlanningEntity.setBaseDuration(Integer.parseInt(String.valueOf(engineTask.getTaskData().getTaskInputVariables().get("BaseDuration"))));
        // set the current status of the task
        taskPlanningEntity.setStatus(engineTask.getTaskData().getStatus());
        taskPlanningEntity.setPriority(mapPriority(engineTask.getPriority()));
        taskPlanningEntity.setSkill((String) engineTask.getTaskData().getTaskInputVariables().get("Skill"));
        taskPlanningEntity.setInputVariables(engineTask.getTaskData().getTaskInputVariables());

        return taskPlanningEntity;
    }

    private static Priority mapPriority(int priority) {
        if (priority < 1 || priority > 3) {
            throw new RuntimeException("Priority number is not in interval <1,3>");
        }

        switch (priority) {
            case 1 : return Priority.LOW;
            case 2 : return Priority.MEDIUM;
            default : return Priority.HIGH;
        }
    }
}
