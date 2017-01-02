package org.kie.demo.taskassignment.db;

import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.demo.taskassignment.planner.domain.SkillLevel;
import org.kie.demo.taskassignment.util.AbstractCaseServicesBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserGroupSkillEntityTest extends AbstractCaseServicesBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(UserGroupSkillEntityTest.class);

    private EntityManager em;

    @Before
    public void setUp() throws Exception {
        buildDatasource();
        emf = Persistence.createEntityManagerFactory("org.jbpm.domain");
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() throws Exception {
        em.close();
        if (emf != null) {
            emf.close();
        }
        closeDataSource();
    }

    @Test
    public void testPersistUser() throws Exception {
        UserEntity marian = new UserEntity("Marian");
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        utx.begin();
        em.joinTransaction();
        em.persist(marian);
        utx.commit();

        em.clear();
        em.close();
        em = emf.createEntityManager();

        List<UserEntity> users = em.createQuery("SELECT u from UserEntity u", UserEntity.class).getResultList();

        users.forEach(userEntity -> logger.debug(userEntity.getName()));

        Assertions.assertThat(users).hasSize(1);
        Assertions.assertThat(users.get(0)).isEqualTo(marian);
    }

    @Test
    public void testPersistGroup() throws Exception {
        GroupEntity hr = new GroupEntity("HR");
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        utx.begin();
        em.joinTransaction();
        em.persist(hr);
        utx.commit();

        em.clear();
        em.close();
        em = emf.createEntityManager();

        List<GroupEntity> groups = em.createQuery("SELECT g from GroupEntity g", GroupEntity.class).getResultList();

        groups.forEach(groupEntity -> logger.debug(groupEntity.getName()));

        Assertions.assertThat(groups).hasSize(1);
        Assertions.assertThat(groups.get(0)).isEqualTo(hr);
    }

    @Test
    public void testPersistSkill() throws Exception {
        SkillEntity manager = new SkillEntity("manager");
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        utx.begin();
        em.joinTransaction();
        em.persist(manager);
        utx.commit();

        em.clear();
        em.close();
        em = emf.createEntityManager();

        List<SkillEntity> roles = em.createQuery("SELECT r from SkillEntity r", SkillEntity.class).getResultList();

        roles.forEach(skillEntity -> logger.debug(skillEntity.getName()));

        Assertions.assertThat(roles).hasSize(1);
        Assertions.assertThat(roles.get(0)).isEqualTo(manager);
    }

    @Test
    public void testPersistUserGroup() throws Exception {
        UserEntity marian = new UserEntity("Marian");
        GroupEntity hr = new GroupEntity("HR");

        UserGroupEntity marianHrGroup = new UserGroupEntity();
        marianHrGroup.setUser(marian);
        marianHrGroup.setGroup(hr);

        marian.addUserGroup(marianHrGroup);
        hr.addUserGroup(marianHrGroup);

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        utx.begin();
        em.joinTransaction();

        em.persist(hr);
        em.persist(marian);
        em.persist(marianHrGroup);

        logger.debug("Groups: " + marian.getUserGroups());
        utx.commit();

        em.clear();
        em.close();
        em = emf.createEntityManager();

        List<UserEntity> users = em.createQuery("SELECT u from UserEntity u", UserEntity.class).getResultList();

        users.forEach(userEntity -> logger.debug("Users: " + userEntity.getName() + userEntity.getId() + userEntity.getUserGroups()));

        Assertions.assertThat(users).hasSize(1);
        Assertions.assertThat(users.get(0)).isEqualTo(marian);

        List<GroupEntity> groups = em.createQuery("SELECT g from GroupEntity g", GroupEntity.class).getResultList();

        groups.forEach(groupEntity -> logger.debug("Groups: " + groupEntity.getName() + groupEntity.getId() + groupEntity.getUserGroups()));

        Assertions.assertThat(groups).hasSize(1);
        Assertions.assertThat(groups.get(0)).isEqualTo(hr);

        List<UserGroupEntity> userGroups = em.createQuery("SELECT u from UserGroupEntity u", UserGroupEntity.class).getResultList();

        userGroups.forEach(userGroupEntity -> logger.debug("UserGroups: " + userGroupEntity + userGroupEntity.getUser().getId() + userGroupEntity.getGroup().getId()));

        Assertions.assertThat(userGroups).hasSize(1);
        Assertions.assertThat(userGroups.get(0)).isEqualTo(marianHrGroup);

    }

    @Test
    public void testPersistUserSkill() throws Exception {
        UserEntity marian = new UserEntity("Marian");
        SkillEntity manager = new SkillEntity("manager");

        UserSkillEntity marianManagerRole = new UserSkillEntity(marian, manager, SkillLevel.EXPERT);
        marianManagerRole.setUser(marian);
        marianManagerRole.setSkill(manager);

        marian.addUserSkill(marianManagerRole);
        manager.addUserSkill(marianManagerRole);

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        utx.begin();
        em.joinTransaction();

        em.persist(manager);
        em.persist(marian);
        em.persist(marianManagerRole);

        logger.debug("Roles: " + marian.getUserSkills());
        utx.commit();

        em.clear();
        em.close();
        em = emf.createEntityManager();

        List<UserEntity> users = em.createQuery("SELECT u from UserEntity u", UserEntity.class).getResultList();

        users.forEach(userEntity -> logger.debug("Users: " + userEntity.getName() + userEntity.getId() + userEntity.getUserSkills()));

        Assertions.assertThat(users).hasSize(1);
        Assertions.assertThat(users.get(0)).isEqualTo(marian);

        List<SkillEntity> roles = em.createQuery("SELECT r from SkillEntity r", SkillEntity.class).getResultList();

        roles.forEach(skillEntity -> logger.debug("Groups: " + skillEntity.getName() + skillEntity.getId() + skillEntity.getUserSkills()));

        Assertions.assertThat(roles).hasSize(1);
        Assertions.assertThat(roles.get(0)).isEqualTo(manager);

        List<UserSkillEntity> userRoles = em.createQuery("SELECT u from UserSkillEntity u", UserSkillEntity.class).getResultList();

        userRoles.forEach(userSkillEntity -> logger.debug("UserGroups: " + userSkillEntity + userSkillEntity.getUser().getId() + userSkillEntity.getSkill().getId()));

        Assertions.assertThat(userRoles).hasSize(1);
        Assertions.assertThat(userRoles.get(0)).isEqualTo(marianManagerRole);

    }

}