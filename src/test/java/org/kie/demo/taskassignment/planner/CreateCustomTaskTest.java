package org.kie.demo.taskassignment.planner;

import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jbpm.casemgmt.api.model.instance.CaseFileInstance;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;
import org.kie.demo.taskassignment.test.util.AbstractCaseServicesBaseTest;
import org.kie.demo.taskassignment.util.UserServiceUtil;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.scanner.MavenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateCustomTaskTest extends AbstractCaseServicesBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssigningTest.class);
    private static final String TEST_DOC_STORAGE = "target/docs";

    // Overridden from parent
    protected static final String GROUP_ID = "org.kie.demo";
    protected static final String ARTIFACT_ID = "taskassignment-cases";
    protected static final String VERSION = "1.0.0-SNAPSHOT";

    private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();

    private List<String> activeCaseIds = new ArrayList<>();

    private static final String ONE_TASK_PROC_ID = "OptaplannerTasks.OneTask";

    protected static final String OPTTASK_CASE_ID = "OPTTASK-0000000001";

    public static List<TaskPlanningEntity> tasks = new ArrayList<>();

    private List<User> users;

    @Before
    public void prepare() {
        System.setProperty("org.jbpm.document.storage", TEST_DOC_STORAGE);
        deleteFolder(TEST_DOC_STORAGE);
        configureServices();
        insertUsersAndGroupsToDB();

        UserServiceUtil.setEmf(emf);
        users = UserServiceUtil.getAllUsers();

        Assertions.assertThat(users.size()).isGreaterThan(0);

        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        List<String> processes = new ArrayList<String>();
        processes.add("org/kie/demo/taskassignment/planner/OneTask.bpmn2");


        InternalKieModule kJar1 = createKieJar(ks, releaseId, processes);
        File pom = new File("target/kmodule", "pom.xml");
        pom.getParentFile().mkdir();
        try {
            FileOutputStream fs = new FileOutputStream(pom);
            fs.write(getPom(releaseId).getBytes());
            fs.close();
        } catch (Exception e) {

        }
        MavenRepository repository = getMavenRepository();
        repository.deployArtifact(releaseId, kJar1, pom);
    }

    @After
    public void cleanup() {

        System.clearProperty("org.jbpm.document.storage");
        cleanupSingletonSessionId();
        if (units != null && !units.isEmpty()) {
            for (DeploymentUnit unit : units) {
                deploymentService.undeploy(unit);
            }
            units.clear();
        }
        UserServiceUtil.setEmf(null);
        close();
        // CountDownListenerFactory.clear(); No CountDownListenerFacotry in these tests
    }

    @Test
    public void testCustomTask() {

        String deploymentId = deployAndAssert();

        // let's assign users to roles so they can be participants in the case


        // start new instance of a case with data and role assignment
        Map<String, Object> data = new HashMap<>();
        data.put("TaskName", "Send a letter");
        data.put("Skill", "administration");
        data.put("BaseDuration", 55);
        data.put("Priority", 3);

        // assignments will be given later
        Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, ONE_TASK_PROC_ID, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, ONE_TASK_PROC_ID, caseFile);

        caseService.assignToCaseRole(caseId, "users", new UserImpl("John"));
        caseService.assignToCaseRole(caseId, "users", new UserImpl("Marian"));
        // this will add Mary to potential owners as well
        caseService.assignToCaseRole(caseId, "groups", new GroupImpl("suppliers"));

        Assertions.assertThat(tasks).isEmpty();

        // now we will trigger task, so it should be pushed
        caseService.triggerAdHocFragment(caseId, "#{caseFile_TaskName}", null);

        Assertions.assertThat(tasks).hasSize(1);

        List<TaskSummary> taskSummaries = runtimeDataService.getTasksAssignedAsPotentialOwner("John", new QueryFilter());
        Assertions.assertThat(taskSummaries).hasSize(1);

        taskSummaries = runtimeDataService.getTasksAssignedAsPotentialOwner("Marian", new QueryFilter());
        Assertions.assertThat(taskSummaries).hasSize(1);

        taskSummaries = runtimeDataService.getTasksAssignedAsPotentialOwner("Mary", new QueryFilter());
        Assertions.assertThat(taskSummaries).hasSize(1);

        Assertions.assertThat(taskSummaries.get(0).getName()).isEqualTo("Send a letter");
        Assertions.assertThat(taskSummaries.get(0).getStatus()).isEqualTo(Status.Ready);

        // now check the pushed task
        Assertions.assertThat(tasks.get(0).getActualOwner()).isNull();
        Assertions.assertThat(tasks.get(0).getName()).isEqualTo("Send a letter");
        Assertions.assertThat(tasks.get(0).getStatus()).isEqualTo(Status.Ready);
        Assertions.assertThat(tasks.get(0).getId()).isEqualTo(1);
        Assertions.assertThat(tasks.get(0).getBaseDuration()).isEqualTo(55);
        Assertions.assertThat(tasks.get(0).getPotentialUserOwners()).contains("John", "Marian");
        Assertions.assertThat(tasks.get(0).getPotentialGroupOwners()).contains(new Group("suppliers"));
        Assertions.assertThat(tasks.get(0).getPriority()).isEqualTo(Priority.HIGH);
        Assertions.assertThat(tasks.get(0).getSkill()).isEqualTo("administration");

        caseService.cancelCase(caseId);

    }

    /*
     * Helper methods
     */
    protected String deployAndAssert() {
        Assertions.assertThat(deploymentService).isNotNull();
        DeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);

        deploymentService.deploy(deploymentUnit);

        units.add(deploymentUnit);

        return deploymentUnit.getIdentifier();
    }

    @Override
    protected List<ObjectModel> getTaskEventListeners() {
        List<ObjectModel> listeners = super.getTaskEventListeners();

        listeners.add(new ObjectModel("mvel", "new org.kie.demo.taskassignment.test.util.PushTaskEventListener(org.kie.demo.taskassignment.planner.CreateCustomTaskTest.tasks)"));

        return listeners;
    }
}
