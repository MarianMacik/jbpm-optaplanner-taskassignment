package org.kie.demo.taskassignment.test.util;

import java.util.List;

import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;

public class PushTaskEventListener extends DefaultTaskEventListener {

    private List<TaskPlanningEntity> tasks;

    public PushTaskEventListener(List<TaskPlanningEntity> tasks) {
        this.tasks = tasks;
    }

    /*
    * Conversion to planning entities will be done in every event listener
    *
    *
    * */

    @Override
    public void afterTaskAddedEvent(TaskEvent event) {
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Task has been added " + event.getTask().getName() + " " + event.getTask().getId());
        // no need to load task input variables since they are available immediately in this type of event
        tasks.add(mapTaskToPlanningEntity(event.getTask()));

        // only problem fact add should be called
    }

    @Override
    public void afterTaskClaimedEvent(TaskEvent event) {
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Task has been claimed " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());
        TaskPlanningEntity newTask = mapTaskToPlanningEntity(event.getTask());
        int taskIndex = tasks.indexOf(newTask);
        tasks.set(taskIndex, newTask);

        // only problem fact changed should be called - find task in solution and change its status
    }

    @Override
    public void afterTaskStartedEvent(TaskEvent event) {
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Task has been started " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());
        TaskPlanningEntity newTask = mapTaskToPlanningEntity(event.getTask());
        int taskIndex = tasks.indexOf(newTask);
        tasks.set(taskIndex, newTask);

        // only problem fact changed should be called - find task in solution and change its status
    }

    @Override
    public void afterTaskCompletedEvent(TaskEvent event) {
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Task has been completed " + event.getTask().getName() + " " + event.getTask().getId());
        event.getTaskContext().loadTaskVariables(event.getTask());
        tasks.remove(mapTaskToPlanningEntity(event.getTask()));

        // find this task in solution and set next task so it does not point to this task, but points to null
        // remove problem fact - task - from task collection
    }

    @Override
    public void afterTaskExitedEvent(TaskEvent event) {
        // Task is exited when there is a cancelCase or a destroyCase command called
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Task has been exited " + event.getTask().getName() + " " + event.getTask().getId());
        event.getTaskContext().loadTaskVariables(event.getTask());
        tasks.remove(mapTaskToPlanningEntity(event.getTask()));
    }

    private static TaskPlanningEntity mapTaskToPlanningEntity(Task engineTask) {
        TaskPlanningEntity taskPlanningEntity = new TaskPlanningEntity();

        taskPlanningEntity.setId(engineTask.getId());
        taskPlanningEntity.setName(engineTask.getName());

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
        taskPlanningEntity.setBaseDuration(Integer.parseInt((String) engineTask.getTaskData().getTaskInputVariables().get("BaseDuration")));
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