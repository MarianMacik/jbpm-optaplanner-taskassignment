package org.kie.demo.taskassignment.planner.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("User")
public class User extends TaskOrUser {

    private String name;

    private Set<Group> groups = new HashSet<>();

    // name of the skill and particular skill object (also with skillLevel)
    private Map<String, Skill> skills = new HashMap<>();

    public User() {
    }

    public User(String name) {
        this.name = name;
    }

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
