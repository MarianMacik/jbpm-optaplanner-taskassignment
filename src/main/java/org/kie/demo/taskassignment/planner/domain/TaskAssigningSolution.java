package org.kie.demo.taskassignment.planner.domain;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.bendable.BendableScore;

@PlanningSolution
public class TaskAssigningSolution {

    // Other problem facts might be added later when needed

    @ValueRangeProvider(id = "userRange")
    @ProblemFactCollectionProperty
    private List<User> userList;


    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "taskRange")
    private List<TaskPlanningEntity> taskList;


    @PlanningScore(bendableHardLevelsSize = 1, bendableSoftLevelsSize = 4)
    private BendableScore score;

    public TaskAssigningSolution() {
    }

    public TaskAssigningSolution(List<User> userList, List<TaskPlanningEntity> taskList, BendableScore score) {
        this.userList = userList;
        this.taskList = taskList;
        this.score = score;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public List<TaskPlanningEntity> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<TaskPlanningEntity> taskList) {
        this.taskList = taskList;
    }

    public BendableScore getScore() {
        return score;
    }

    public void setScore(BendableScore score) {
        this.score = score;
    }
}
