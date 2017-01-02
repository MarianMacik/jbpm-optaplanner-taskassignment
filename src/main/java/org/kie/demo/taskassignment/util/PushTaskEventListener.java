package org.kie.demo.taskassignment.util;

import java.util.List;

import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.demo.taskassignment.app.view.SolutionController;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.kie.demo.taskassignment.planner.domain.TaskOrUser;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushTaskEventListener extends DefaultTaskEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PushTaskEventListener.class);

    private List<TaskPlanningEntity> tasks;

    private SolutionController solutionController;

    public PushTaskEventListener(List<TaskPlanningEntity> tasks, SolutionController solutionController) {
        this.tasks = tasks;
        this.solutionController = solutionController;
    }

    /*
    * Conversion to planning entities is done in every event listener
    *
    * */

    @Override
    public void afterTaskAddedEvent(TaskEvent event) {
        logger.debug("Task has been added " + event.getTask().getName() + " " + event.getTask().getId());
        // no need to load task input variables since they are available immediately in this type of event
        TaskPlanningEntity newTask = mapTaskToPlanningEntity(event.getTask());


        // only problem fact add should be called
        // if there is a loaded solution on a controller - add it via problem fact change, otherwise simply add it to tasks list
        if (solutionController.isSolutionLoaded()) {
            solutionController.doProblemFactChange(scoreDirector -> {
                TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
                scoreDirector.beforeEntityAdded(newTask);
                solution.getTaskList().add(newTask);
                scoreDirector.afterEntityAdded(newTask);
                scoreDirector.triggerVariableListeners();
            });
        } else {
            tasks.add(newTask);
        }
    }

    @Override
    public void afterTaskClaimedEvent(TaskEvent event) {
        logger.debug("Task has been claimed " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());

        solutionController.doProblemFactChange(scoreDirector -> {
            TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
            for (TaskPlanningEntity task : solution.getTaskList()) {
                if (task.getId().equals(event.getTask().getId())){
                    scoreDirector.beforeProblemPropertyChanged(task);
                    task.setStatus(Status.Reserved);
                    task.setActualOwner(event.getTask().getTaskData().getActualOwner().getId());
                    scoreDirector.afterProblemPropertyChanged(task);
                    break;
                }
            }
            scoreDirector.triggerVariableListeners();
        });
    }

    @Override
    public void afterTaskStartedEvent(TaskEvent event) {
        logger.debug("Task has been started " + event.getTask().getName() + " " + event.getTask().getId());
        // in this type of event we need to load task variables
        event.getTaskContext().loadTaskVariables(event.getTask());

        solutionController.doProblemFactChange(scoreDirector -> {
            TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
            for (TaskPlanningEntity task : solution.getTaskList()) {
                if (task.getId().equals(event.getTask().getId())){
                    scoreDirector.beforeProblemPropertyChanged(task);
                    task.setStatus(Status.InProgress);
                    scoreDirector.afterProblemPropertyChanged(task);
                    break;
                }
            }
            scoreDirector.triggerVariableListeners();
        });
    }

    @Override
    public void afterTaskCompletedEvent(TaskEvent event) {
        logger.debug("Task has been completed " + event.getTask().getName() + " " + event.getTask().getId());
        event.getTaskContext().loadTaskVariables(event.getTask());

        // find this task in solution and set next task so it does not point to this task, but points to null
        // remove problem fact - task - from task collection


        solutionController.doProblemFactChange(scoreDirector -> {
            TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
            TaskPlanningEntity completedTask = null;
            TaskOrUser prevTaskOrUser;
            TaskPlanningEntity nextTask;
            for (TaskPlanningEntity task : solution.getTaskList()) {
                if (task.getId().equals(event.getTask().getId())){
                    completedTask = task;
                    break;
                }
            }
            prevTaskOrUser = completedTask.getPreviousTaskOrUser();
            nextTask = completedTask.getNextTask();
            if (nextTask != null) {
                // in case completed task is last it can be null
                scoreDirector.beforeVariableChanged(nextTask,"previousTaskOrUser");
                nextTask.setPreviousTaskOrUser(prevTaskOrUser);
                scoreDirector.afterVariableChanged(nextTask,"previousTaskOrUser");
            }
            scoreDirector.beforeVariableChanged(prevTaskOrUser,"nextTask");
            prevTaskOrUser.setNextTask(null);
            scoreDirector.afterVariableChanged(prevTaskOrUser,"nextTask");

            completedTask.setPreviousTaskOrUser(null);

            scoreDirector.beforeVariableChanged(completedTask,"nextTask");
            completedTask.setNextTask(null);
            scoreDirector.afterVariableChanged(completedTask,"nextTask");


            scoreDirector.beforeEntityRemoved(completedTask);
            solution.getTaskList().remove(completedTask);
            scoreDirector.afterEntityRemoved(completedTask);

            scoreDirector.triggerVariableListeners();
        });

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
