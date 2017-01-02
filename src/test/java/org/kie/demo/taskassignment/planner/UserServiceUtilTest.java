package org.kie.demo.taskassignment.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Persistence;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.SkillLevel;
import org.kie.demo.taskassignment.planner.domain.User;
import org.kie.demo.taskassignment.util.AbstractCaseServicesBaseTest;
import org.kie.demo.taskassignment.util.UserServiceUtil;

public class UserServiceUtilTest extends AbstractCaseServicesBaseTest {

    @Before
    public void setUp() throws Exception {
        buildDatasource();
        emf = Persistence.createEntityManagerFactory("org.jbpm.domain");
        UserServiceUtil.setEmf(emf);
        insertUsersAndGroupsToDB();
    }

    @After
    public void tearDown() throws Exception {
        if (emf != null) {
            emf.close();
        }
        UserServiceUtil.setEmf(null);
        closeDataSource();
    }

    @Test
    public void testGetAllUsers() {
        List<User> users = UserServiceUtil.getAllUsers();

        // Assert that Administrator is not in the result
        Assertions.assertThat(users).hasSize(3);

        List<User> filteredUsers = users.stream().filter(user -> user.getName().equals("Marian")).collect(Collectors.toList());
        List<Group> groups = new ArrayList<>();
        assertUser(filteredUsers, "Marian", "management", SkillLevel.ADVANCED, "delivering", SkillLevel.ADVANCED, groups);

        filteredUsers = users.stream().filter(user -> user.getName().equals("John")).collect(Collectors.toList());
        groups.add(new Group("managers"));
        assertUser(filteredUsers, "John", "management", SkillLevel.BEGINNER, "delivering", SkillLevel.EXPERT, groups);

        filteredUsers = users.stream().filter(user -> user.getName().equals("Mary")).collect(Collectors.toList());
        groups.clear();
        groups.add(new Group("suppliers"));
        assertUser(filteredUsers, "Mary", "management", SkillLevel.EXPERT, "delivering", SkillLevel.BEGINNER, groups);

    }

    private void assertUser(List<User> filteredUsers, String name, String skillName1, SkillLevel skillLevel1, String skillName2, SkillLevel skillLevel2, List<Group> groups) {
        Assertions.assertThat(filteredUsers).hasSize(1);
        User user = filteredUsers.get(0);
        Assertions.assertThat(user.getName()).isEqualTo(name);

        Assertions.assertThat(user.getSkills().get(skillName1).getSkillLevel()).isEqualTo(skillLevel1);
        Assertions.assertThat(user.getSkills().get(skillName2).getSkillLevel()).isEqualTo(skillLevel2);

        groups.forEach(group -> Assertions.assertThat(user.getGroups()).contains(group));
    }
}
