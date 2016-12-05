package org.kie.demo.taskassignment.planner.domain;

import java.util.Map;
import java.util.Set;

public class User extends TaskOrUser {

    private String name;

    private Set<Group> groups;

    // name of the skill and particular skill object (also with skillLevel)
    private Map<String, Skill> skills;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Map<String, Skill> getSkills() {
        return skills;
    }

    public void setSkills(Map<String, Skill> skills) {
        this.skills = skills;
    }


    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public Integer getEndTime() {
        return 0;
    }

    @Override
    public User getUser() {
        return this;
    }
}
