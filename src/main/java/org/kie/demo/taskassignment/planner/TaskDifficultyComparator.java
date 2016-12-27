package org.kie.demo.taskassignment.planner;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;

public class TaskDifficultyComparator implements Comparator<TaskPlanningEntity>, Serializable {

    @Override
    public int compare(TaskPlanningEntity a, TaskPlanningEntity b) {
        return new CompareToBuilder()
                .append(a.getPriority(), b.getPriority())
//                .append(a.getTaskType().getRequiredSkillList().size(), b.getTaskType().getRequiredSkillList().size())
//                .append(a.getTaskType().getBaseDuration(), b.getTaskType().getBaseDuration())
                //.append(a.getBaseDuration(), b.getBaseDuration())
                //.append(a.getId(), b.getId())
                .toComparison();
    }

}
