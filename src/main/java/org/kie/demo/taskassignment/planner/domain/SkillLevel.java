package org.kie.demo.taskassignment.planner.domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("SkillLevel")
public enum SkillLevel {
    NONE(4),  // only for score calculation in case no user is assigned to a task
    BEGINNER(3),
    ADVANCED(2),
    EXPERT(1);

    private int durationMultiplier;

    SkillLevel(int durationMultiplier) {
        this.durationMultiplier = durationMultiplier;
    }

    public int getDurationMultiplier() {
        return durationMultiplier;
    }

}
