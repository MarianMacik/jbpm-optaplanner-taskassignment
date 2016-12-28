package org.kie.demo.taskassignment.db;

import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;

import org.assertj.core.api.Assertions;
import org.jbpm.services.task.identity.DBUserGroupCallbackImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.demo.taskassignment.db.GroupEntity;
import org.kie.demo.taskassignment.db.UserEntity;
import org.kie.demo.taskassignment.db.UserGroupEntity;
import org.kie.demo.taskassignment.test.util.AbstractCaseServicesBaseTest;

public class DBUserGroupCallbackTest extends AbstractCaseServicesBaseTest {

    private EntityManager em;

    private Properties props = buildUserGroupCallbackProperties();

    @Before
    public void setUp() throws Exception {
        buildDatasource();
        emf = Persistence.createEntityManagerFactory("org.jbpm.domain");
        em = emf.createEntityManager();

        prepareDB();
    }

    @After
    public void tearDown() throws Exception {
        em.close();
        if (emf != null) {
            emf.close();
        }
        closeDataSource();
    }

    private void prepareDB() {
        try {
            UserEntity marian = new UserEntity("Marian");
            GroupEntity hr = new GroupEntity("HR");

            UserGroupEntity marianHrGroup = new UserGroupEntity();
            marianHrGroup.setUser(marian);
            marianHrGroup.setGroup(hr);

            marian.addUserGroup(marianHrGroup);
            hr.addUserGroup(marianHrGroup);

            GroupEntity it = new GroupEntity("IT");

            UserGroupEntity marianItGroup = new UserGroupEntity();
            marianItGroup.setUser(marian);
            marianItGroup.setGroup(it);

            marian.addUserGroup(marianItGroup);
            it.addUserGroup(marianItGroup);

            UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            utx.begin();
            em.joinTransaction();

            em.persist(it);
            em.persist(hr);
            em.persist(marian);
            em.persist(marianHrGroup);
            em.persist(marianItGroup);

            utx.commit();
        } catch (Exception e) {
            throw new RuntimeException("Problem while initializing DB!", e);
        }

    }

    @Test
    public void testUserExists() {
        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsUser("Marian");
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void testGroupExists() {
        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsGroup("HR");
        Assertions.assertThat(exists).isTrue();
        exists = callback.existsGroup("IT");
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void testUserGroups() {

        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        List<String> groups = callback.getGroupsForUser("Marian");
        Assertions.assertThat(groups).isNotNull();
        Assertions.assertThat(groups).hasSize(2);
        Assertions.assertThat(groups).contains("HR", "IT");
    }

    @Test
    public void testUserNotExists() {
        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsUser("Natalie");
        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void testGroupNotExists() {
        DBUserGroupCallbackImpl callback = new DBUserGroupCallbackImpl(props);
        boolean exists = callback.existsGroup("PR");
        Assertions.assertThat(exists).isFalse();
    }


}
