package org.kie.demo.taskassignment.db;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "SKILLS")
public class SkillEntity implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String name;

    @OneToMany(mappedBy = "primaryKey.skill")
    private Set<UserSkillEntity> userSkills = new HashSet<>();

    public SkillEntity() {
    }

    public SkillEntity(String name) {
        this.name = name;
    }

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

    public Set<UserSkillEntity> getUserSkills() {
        return userSkills;
    }

    public void setUserSkills(Set<UserSkillEntity> userSkills) {
        this.userSkills = userSkills;
    }

    public void addUserSkill(UserSkillEntity userSkill) {
        this.userSkills.add(userSkill);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkillEntity that = (SkillEntity) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
