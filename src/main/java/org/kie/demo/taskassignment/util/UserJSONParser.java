package org.kie.demo.taskassignment.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.kie.demo.taskassignment.db.GroupEntity;
import org.kie.demo.taskassignment.db.SkillEntity;
import org.kie.demo.taskassignment.db.UserEntity;
import org.kie.demo.taskassignment.db.UserGroupEntity;
import org.kie.demo.taskassignment.db.UserSkillEntity;
import org.kie.demo.taskassignment.planner.domain.SkillLevel;

/**
 * Simple utility class for DB preparation. It reads the given JSON file and inserts users, their groups and their skills
 * into DB.
 */
public class UserJSONParser {

    private EntityManagerFactory emf;

    private EntityManager em;

    public UserJSONParser(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void insertUsersFromFile(File jsonFile) {
        em = emf.createEntityManager();

        try (JsonReader jsonReader = createJsonReader(jsonFile)) {
            JsonParser jsonParser = new JsonParser();
            JsonArray users = jsonParser.parse(jsonReader).getAsJsonArray();

            for (JsonElement userElement : users) {
                JsonObject user = userElement.getAsJsonObject();
                String userName = user.get("user").getAsString();

                // persist user
                UserEntity userEntity = createUser(userName);

                JsonArray skills = user.getAsJsonArray("skills");

                for (JsonElement skillElement : skills) {
                    JsonObject skill = skillElement.getAsJsonObject();
                    String skillName = skill.get("skill").getAsString();
                    // persist skill
                    SkillEntity skillEntity = findOrCreateSkill(skillName);

                    String jsonSkillLevel = skill.get("level").getAsString();
                    SkillLevel skillLevel = getSkillLevel(jsonSkillLevel);
                    // persist UserSkill
                    createUserSkillEntity(userEntity, skillEntity, skillLevel);

                }

                JsonArray groups = user.getAsJsonArray("groups");

                for (JsonElement groupElement : groups) {
                    String groupName = groupElement.getAsString();
                    // persist group
                    GroupEntity groupEntity = findOrCreateGroup(groupName);
                    // persist UserGroup
                    createUserGroupEntity(userEntity, groupEntity);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File does not exist, please check it!", e);
        } catch (IOException e) {
            throw new RuntimeException("There was a problem with IO, please check the file which you want to read!", e);
        } catch (Exception e) {
            throw new RuntimeException("Problem with DB", e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }


    private JsonReader createJsonReader(File jsonFile) throws FileNotFoundException {
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(jsonFile)));
        return reader;
    }

    private UserEntity createUser(String userName) {
        UserEntity newUser = new UserEntity(userName);
        persistEntity(newUser);
        return newUser;
    }

    private SkillEntity findOrCreateSkill(String skillName) {
        List<SkillEntity> skills = em.createQuery("SELECT s from SkillEntity s WHERE s.name = :name", SkillEntity.class).setParameter("name", skillName).getResultList();

        // If there is already this skill, simply return it
        if (skills.size() == 1) {
            return skills.get(0);
        }

        SkillEntity newSkill = new SkillEntity(skillName);
        persistEntity(newSkill);
        return newSkill;
    }

    private UserSkillEntity createUserSkillEntity(UserEntity userEntity, SkillEntity skillEntity, SkillLevel skillLevel) {
        UserSkillEntity newUserSkillEntity = new UserSkillEntity(userEntity, skillEntity, skillLevel);
        persistEntity(newUserSkillEntity);
        return newUserSkillEntity;
    }

    private GroupEntity findOrCreateGroup(String groupName) {
        List<GroupEntity> groups = em.createQuery("SELECT g from GroupEntity g WHERE g.name = :name", GroupEntity.class).setParameter("name", groupName).getResultList();
        if (groups.size() == 1) {
            return groups.get(0);
        }

        GroupEntity newGroup = new GroupEntity(groupName);
        persistEntity(newGroup);
        return newGroup;
    }

    private UserGroupEntity createUserGroupEntity(UserEntity userEntity, GroupEntity groupEntity) {
        UserGroupEntity newUserGroupEntity = new UserGroupEntity(userEntity, groupEntity);
        persistEntity(newUserGroupEntity);
        return newUserGroupEntity;
    }

    private void persistEntity(Object newEntity) {
        try {
            UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            utx.begin();

            em.joinTransaction();
            em.persist(newEntity);

            utx.commit();
        } catch (Exception e) {
            throw new RuntimeException("There was a problem persisting a new object", e);
        }
    }

    private SkillLevel getSkillLevel(String jsonSkillLevel) {
        switch (jsonSkillLevel) {
            case "BEGINNER":
                return SkillLevel.BEGINNER;
            case "ADVANCED":
                return SkillLevel.ADVANCED;
            case "EXPERT":
                return SkillLevel.EXPERT;
            default: throw new RuntimeException("Bad SkillLevel value in your JSON file");
        }
    }
}
