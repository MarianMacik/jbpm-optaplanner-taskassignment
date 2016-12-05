package org.kie.demo.taskassignment.planner.domain;

public class Skill {

    private String name;

    private SkillLevel skillLevel;

    public Skill() {
    }

    public Skill(String name, SkillLevel skillLevel) {
        this.name = name;
        this.skillLevel = skillLevel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }
}
