package org.kie.demo.taskassignment.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.kie.demo.taskassignment.db.SkillEntity;
import org.kie.demo.taskassignment.db.UserEntity;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Skill;
import org.kie.demo.taskassignment.planner.domain.User;

/**
 * Utility class for convenient retrieving of users from DB
 */
public class UserServiceUtil {

    public static final String ADMINISTRATOR_USER_NAME = "Administrator";

    private static EntityManagerFactory emf;

    public static EntityManagerFactory getEmf() {
        return emf;
    }

    public static void setEmf(EntityManagerFactory emf) {
        UserServiceUtil.emf = emf;
    }

    public static List<User> getAllUsers() {
        EntityManager em = emf.createEntityManager();
        List<UserEntity> userEntities = em.createQuery("SELECT u from UserEntity u where u.name != :name", UserEntity.class).setParameter("name", ADMINISTRATOR_USER_NAME).getResultList();
        List<User> users = userEntities.stream().map(UserServiceUtil::mapUserEntityToUser).collect(Collectors.toList());

        em.close();

        return users;
    }

    public static List<String> getAllUserNames() {
        EntityManager em = emf.createEntityManager();
        List<String> userNames = em.createQuery("SELECT u.name from UserEntity u where u.name != :name", String.class).setParameter("name", ADMINISTRATOR_USER_NAME).getResultList();
        em.close();

        return userNames;
    }

    public static List<String> getAllGroupNames() {
        EntityManager em = emf.createEntityManager();
        List<String> groupNames = em.createQuery("SELECT g.name from GroupEntity g", String.class).getResultList();
        em.close();

        return groupNames;
    }

    public static List<String> getAllSkillNames() {
        EntityManager em = emf.createEntityManager();
        List<String> skillNames = em.createQuery("SELECT s.name from SkillEntity s", String.class).getResultList();
        em.close();

        return skillNames;
    }


    private static User mapUserEntityToUser(UserEntity userEntity) {
        User user = new User();
        user.setName(userEntity.getName());

        Set<Group> groups = userEntity.getUserGroups().stream().map(userGroupEntity ->
                new Group(userGroupEntity.getGroup().getName())).collect(Collectors.toSet());

        Map<String, Skill> skills = userEntity.getUserSkills().stream().map(userSkillEntity ->
                new Skill(userSkillEntity.getSkill().getName(), userSkillEntity.getSkillLevel())).collect(Collectors.toMap(Skill::getName, Function.identity()));

        user.setGroups(groups);
        user.setSkills(skills);

        return user;
    }
}
