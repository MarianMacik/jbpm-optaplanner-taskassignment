package org.kie.demo.taskassignment.planner.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public abstract class TaskOrUser {

    // Optaplanner variables

    // Shadow variables
    @InverseRelationShadowVariable(sourceVariableName = "previousTaskOrUser")
    private TaskPlanningEntity nextTask;


    public TaskPlanningEntity getNextTask() {
        return nextTask;
    }

    public void setNextTask(TaskPlanningEntity nextTask) {
        this.nextTask = nextTask;
    }

    public abstract Integer getEndTime();

    public abstract User getUser();

}
