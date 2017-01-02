package org.kie.demo.taskassignment.db;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "USERS_GROUPS")
public class UserGroupEntity implements Serializable{

    @EmbeddedId
    private UserGroupId primaryKey = new UserGroupId();

    public UserGroupEntity() {
    }

    public UserGroupEntity(UserEntity user, GroupEntity group) {
        setUser(user);
        setGroup(group);
    }

    public UserGroupId getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(UserGroupId primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Transient
    public UserEntity getUser() {
        return primaryKey.getUser();
    }

    public void setUser(UserEntity user) {
        primaryKey.setUser(user);
    }

    @Transient
    public GroupEntity getGroup() {
        return primaryKey.getGroup();
    }

    public void setGroup(GroupEntity group) {
        primaryKey.setGroup(group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserGroupEntity that = (UserGroupEntity) o;

        return primaryKey != null ? primaryKey.equals(that.primaryKey) : that.primaryKey == null;

    }

    @Override
    public int hashCode() {
        return primaryKey != null ? primaryKey.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User --- Group pairing: " + primaryKey.getUser().getName() + " --- " + primaryKey.getGroup().getName();
    }
}
