package org.kie.demo.taskassignment.planner;

import org.kie.demo.taskassignment.planner.domain.TaskOrUser;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.optaplanner.core.impl.domain.variable.listener.VariableListener;
import org.optaplanner.core.impl.score.director.ScoreDirector;

public class StartTimeUpdatingVariableListener implements VariableListener<TaskPlanningEntity> {

    @Override
    public void beforeEntityAdded(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        updateStartTime(scoreDirector, task);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        updateStartTime(scoreDirector, task);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector scoreDirector, TaskPlanningEntity task) {
        // Do nothing
    }

    protected void updateStartTime(ScoreDirector scoreDirector, TaskPlanningEntity sourceTask) {
        TaskOrUser previous = sourceTask.getPreviousTaskOrUser();
        TaskPlanningEntity shadowTask = sourceTask;
        Integer startTime = (previous == null ? null : previous.getEndTime());
        while (shadowTask != null) {
            scoreDirector.beforeVariableChanged(shadowTask, "startTime");
            shadowTask.setStartTime(startTime);
            scoreDirector.afterVariableChanged(shadowTask, "startTime");
            startTime = shadowTask.getEndTime();
            shadowTask = shadowTask.getNextTask();
        }
    }

}