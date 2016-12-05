package org.kie.demo.taskassignment.db;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.kie.demo.taskassignment.planner.domain.SkillLevel;

@Entity
@Table(name = "USERS_SKILLS")
public class UserSkillEntity implements Serializable {

    @EmbeddedId
    private UserSkillId primaryKey = new UserSkillId();

    //skill level of the user for the particular skill
    @Enumerated(EnumType.STRING)
    private SkillLevel skillLevel;

    public UserSkillEntity() {
    }

    public UserSkillEntity(UserEntity user, SkillEntity skill, SkillLevel skillLevel) {
        setUser(user);
        setSkill(skill);
        this.skillLevel = skillLevel;
    }

    @Transient
    public UserEntity getUser() {
        return primaryKey.getUser();
    }

    public void setUser(UserEntity user) {
        primaryKey.setUser(user);
    }

    @Transient
    public SkillEntity getSkill() {
        return primaryKey.getSkill();
    }

    public void setSkill(SkillEntity skill) {
        primaryKey.setSkill(skill);
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skill) {
        this.skillLevel = skill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserSkillEntity that = (UserSkillEntity) o;

        return primaryKey != null ? primaryKey.equals(that.primaryKey) : that.primaryKey == null;

    }

    @Override
    public int hashCode() {
        return primaryKey != null ? primaryKey.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User --- Skill pairing: " + primaryKey.getUser().getName() + " --- " + primaryKey.getSkill().getName();
    }
}
