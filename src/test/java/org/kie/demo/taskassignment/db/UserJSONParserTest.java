package org.kie.demo.taskassignment.db;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.demo.taskassignment.planner.domain.SkillLevel;
import org.kie.demo.taskassignment.util.AbstractCaseServicesBaseTest;
import org.kie.demo.taskassignment.util.UserJSONParser;

public class UserJSONParserTest extends AbstractCaseServicesBaseTest {

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
    public void testUserJSONParser() {
        UserJSONParser parser = new UserJSONParser(emf);
        File file = new File("src/test/resources/org/kie/demo/taskassignment/db/testUsers.json");

        parser.insertUsersFromFile(file);

        List<UserEntity> users = em.createQuery("SELECT u from UserEntity u", UserEntity.class).getResultList();

        Assertions.assertThat(users).hasSize(4);

        users = users.stream().filter(userEntity -> userEntity.getName().equals("Mary")).collect(Collectors.toList());

        UserEntity mary = users.get(0);

        Assertions.assertThat(mary.getName()).isEqualTo("Mary");
        Assertions.assertThat(mary.getUserSkills()).hasSize(2);
        Map<String, SkillLevel> skills = mary.getUserSkills().stream().collect(Collectors.toMap(skillEntity -> skillEntity.getSkill().getName(), UserSkillEntity::getSkillLevel));
        Assertions.assertThat(skills.get("management")).isEqualTo(SkillLevel.EXPERT);
        Assertions.assertThat(skills.get("delivering")).isEqualTo(SkillLevel.BEGINNER);
        Assertions.assertThat(mary.getUserGroups()).hasSize(1);
        Assertions.assertThat(mary.getUserGroups().iterator().next().getGroup().getName()).isEqualTo("suppliers");
    }
}
