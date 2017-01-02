package org.kie.demo.taskassignment.planner.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.kie.api.task.model.Status;
import org.kie.demo.taskassignment.planner.StartTimeUpdatingVariableListener;
import org.kie.demo.taskassignment.planner.TaskDifficultyComparator;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.AnchorShadowVariable;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;

@PlanningEntity(/*movableEntitySelectionFilter = MovableTaskSelectionFilter.class,*/
        difficultyComparatorClass = TaskDifficultyComparator.class)
@XStreamAlias("TaskPlanningEntity")
public class TaskPlanningEntity extends TaskOrUser{

    // id from the engine
    private Long id;

    private String name;

    // this actualOwner is present only if this task is in Reserved/InProgress/Completed state, otherwise null
    private String actualOwner;

    // only names of the potential users are sufficient to check if the chosen user is among them
    private Set<String> potentialUserOwners = new HashSet<>();

    // potential groups
    private Set<Group> potentialGroupOwners = new HashSet<>();

    // base duration in minutes
    private Integer baseDuration;

    // status of the task - the same type engine uses
    private Status status;

    // task priority - CRITICAL, MEDIUM, LOW
    private Priority priority;

    // skill needed for this task
    private String skill;

    // things like readyTime can be added in the future, not som important now
    private Map<String, Object> inputVariables = new HashMap<>();



    // Optaplanner variables

    // Planning variables

    @PlanningVariable(valueRangeProviderRefs = {"userRange", "taskRange"},
            graphType = PlanningVariableGraphType.CHAINED)
    private TaskOrUser previousTaskOrUser;

    // Shadow variables

    // TaskPlanningEntity nextTask inherited from superclass

    @AnchorShadowVariable(sourceVariableName = "previousTaskOrUser")
    private User user;

    @CustomShadowVariable(variableListenerClass = StartTimeUpdatingVariableListener.class,
            sources = {@PlanningVariableReference(variableName = "previousTaskOrUser"), @PlanningVariableReference(variableName = "user")})
    private Integer startTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActualOwner() {
        return actualOwner;
    }

    public void setActualOwner(String actualOwner) {
        this.actualOwner = actualOwner;
    }

    public Set<String> getPotentialUserOwners() {
        return potentialUserOwners;
    }

    public void setPotentialUserOwners(Set<String> potentialUserOwners) {
        this.potentialUserOwners = potentialUserOwners;
    }

    public void addPotentialUserOwner(String userName) {
        potentialUserOwners.add(userName);
    }

    public Set<Group> getPotentialGroupOwners() {
        return potentialGroupOwners;
    }

    public void addPotentialGroupOwner(Group group) {
        potentialGroupOwners.add(group);
    }

    public void setPotentialGroupOwners(Set<Group> potentialGroupOwners) {
        this.potentialGroupOwners = potentialGroupOwners;
    }

    public Integer getBaseDuration() {
        return baseDuration;
    }

    public void setBaseDuration(Integer baseDuration) {
        this.baseDuration = baseDuration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Map<String, Object> getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(Map<String, Object> inputVariables) {
        this.inputVariables = inputVariables;
    }

    public TaskOrUser getPreviousTaskOrUser() {
        return previousTaskOrUser;
    }

    public void setPreviousTaskOrUser(TaskOrUser previousTaskOrUser) {
        this.previousTaskOrUser = previousTaskOrUser;
    }

    @Override
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskPlanningEntity that = (TaskPlanningEntity) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "TaskPlanningEntity{" +
                "previousTaskOrUser=" + previousTaskOrUser +
                ", user=" + user +
                ", startTime=" + startTime +
                ", endTime=" + getEndTime() +
                '}';
    }

    public boolean isAssignmentFeasible() {
        // If task is claimed or started, then it must be assigned only to its actualOwner
        if (status == Status.Reserved || status == Status.InProgress) {
            return user.getName().equals(actualOwner);
        }

        // If it is in Ready state - anybody from potential owners can be assigned
        // Check if user is among potential owners
        if (potentialUserOwners.contains(user.getName())) {
            return true;
        // Otherwise check if one of his groups is among potential groups of this task
        // if yes, it is feasible assignment, otherwise a user is not eligible to claim this task
        } else {
            return (user.getGroups().stream().anyMatch(potentialGroupOwners::contains));
        }
    }


    @Override
    public Integer getEndTime() {
        if (startTime == null) {
            return null;
        }

        return startTime + getRealDuration();
    }

    public int getRealDuration() {
        // By default, if there is no user, the task is initialized with skillLevel NONE(4)
        SkillLevel skillLevel = SkillLevel.NONE;
        if (user != null) {
            skillLevel = user.getSkills().get(skill).getSkillLevel();
        }
        Integer realDuration = skillLevel.getDurationMultiplier() * baseDuration;

        return realDuration;
    }

}
