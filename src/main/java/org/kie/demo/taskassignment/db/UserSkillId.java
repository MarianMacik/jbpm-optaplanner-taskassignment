package org.kie.demo.taskassignment.db;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Embeddable
public class UserSkillId implements Serializable{

    @ManyToOne
    private UserEntity user;

    @ManyToOne
    private SkillEntity skill;

    public UserSkillId() {
    }

    public UserSkillId(UserEntity user, SkillEntity skill) {
        this.user = user;
        this.skill = skill;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public SkillEntity getSkill() {
        return skill;
    }

    public void setSkill(SkillEntity skill) {
        this.skill = skill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserSkillId that = (UserSkillId) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        return skill != null ? skill.equals(that.skill) : that.skill == null;

    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (skill != null ? skill.hashCode() : 0);
        return result;
    }
}
