package org.kie.demo.taskassignment.planner;

import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.util.AbstractCaseServicesBaseTest;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.scanner.MavenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushTaskEventListenerTest extends AbstractCaseServicesBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(PushTaskEventListenerTest.class);

    protected static final String GROUP_ID = "org.kie.demo";
    protected static final String ARTIFACT_ID = "taskassignment-cases";
    protected static final String VERSION = "1.0.0-SNAPSHOT";

    private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();

    private static final String TASKS_TO_ASSIGN_PROC_ID = "OptaplannerTasks.TasksToAssign";

    protected static final String OPTTASK_CASE_ID = "OPTTASK-0000000001";

    public static List<TaskPlanningEntity> tasks = new ArrayList<>();

    @Before
    public void prepare() {
        configureServices();
        insertUsersAndGroupsToDB();

        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        List<String> processes = new ArrayList<String>();
        processes.add("org/kie/demo/taskassignment/planner/TasksToAssign.bpmn2");


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
        cleanupSingletonSessionId();
        if (units != null && !units.isEmpty()) {
            for (DeploymentUnit unit : units) {
                deploymentService.undeploy(unit);
            }
            units.clear();
        }
        close();
    }

    @Test
    public void testTasksToAssignCase() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case

        Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
        roleAssignments.put("manager", new UserImpl("John"));
        roleAssignments.put("supplier", new UserImpl("Marian"));
        roleAssignments.put("suppliers", new GroupImpl("suppliers"));


        // start new instance of a case with data and role assignment
        Map<String, Object> data = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, TASKS_TO_ASSIGN_PROC_ID, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, TASKS_TO_ASSIGN_PROC_ID, caseFile);
        Assertions.assertThat(caseId).isNotNull();
        Assertions.assertThat(caseId).isEqualTo(OPTTASK_CASE_ID);

        Assertions.assertThat(tasks).hasSize(2); // both tasks should be pushed

        // John
        List<TaskSummary> assignedTasks = runtimeDataService.getTasksAssignedAsPotentialOwner("John", new QueryFilter());
        Assertions.assertThat(assignedTasks).hasSize(1);
        TaskSummary engineTask = assignedTasks.get(0);

        List<TaskPlanningEntity> filteredTasks = tasks.stream().filter(taskPlanningEntity ->  taskPlanningEntity.getPotentialUserOwners().contains("John")).collect(Collectors.toList());
        Assertions.assertThat(filteredTasks).hasSize(1);
        TaskPlanningEntity filteredTask = filteredTasks.get(0);

        Assertions.assertThat(filteredTask.getId()).isEqualTo(engineTask.getId());
        Assertions.assertThat(filteredTask.getName()).isEqualTo(engineTask.getName());
        // this task is already in reserved state so john should be an actual owner
        Assertions.assertThat(filteredTask.getActualOwner()).isEqualTo(engineTask.getActualOwner().getId());
        Assertions.assertThat(filteredTask.getBaseDuration()).isEqualTo(30);
        Assertions.assertThat(filteredTask.getStatus()).isEqualTo(engineTask.getStatus());
        Assertions.assertThat(filteredTask.getPriority()).isEqualTo(Priority.LOW);
        Assertions.assertThat(filteredTask.getSkill()).isEqualTo("management");


        // we will start john's task to check if it gets updated in our memory
        userTaskService.start(filteredTask.getId(), "John");
        // still size 2, but now john's task should be InProgress
        Assertions.assertThat(tasks).hasSize(2);
        // find updated task with the same ID
        Assertions.assertThat(tasks.get(tasks.indexOf(filteredTask)).getStatus()).isEqualTo(Status.InProgress);

        userTaskService.complete(filteredTask.getId(), "John", new HashMap<>());
        Assertions.assertThat(tasks).hasSize(1);
        Assertions.assertThat(tasks.contains(filteredTask)).isFalse();

        // Marian
        assignedTasks = runtimeDataService.getTasksAssignedAsPotentialOwner("Marian", new QueryFilter());
        Assertions.assertThat(assignedTasks).hasSize(1);

        engineTask = assignedTasks.get(0);

        filteredTasks = tasks.stream().filter(taskPlanningEntity ->  taskPlanningEntity.getPotentialUserOwners().contains("Marian")).collect(Collectors.toList());
        Assertions.assertThat(filteredTasks).hasSize(1);
        filteredTask = filteredTasks.get(0);

        Assertions.assertThat(filteredTask.getId()).isEqualTo(engineTask.getId());
        Assertions.assertThat(filteredTask.getName()).isEqualTo(engineTask.getName());
        // this task is in ready state so nobody is the actualOwner
        Assertions.assertThat(filteredTask.getActualOwner()).isNull();
        Assertions.assertThat(filteredTask.getBaseDuration()).isEqualTo(60);
        Assertions.assertThat(filteredTask.getStatus()).isEqualTo(engineTask.getStatus());
        Assertions.assertThat(filteredTask.getPriority()).isEqualTo(Priority.MEDIUM);
        Assertions.assertThat(filteredTask.getSkill()).isEqualTo("delivering");
        // this task should be for suppliers group too, e.g. Mary is a member of this group
        Assertions.assertThat(filteredTask.getPotentialGroupOwners().iterator().next().getName()).isEqualTo("suppliers");

        if (caseId != null) {
            caseService.cancelCase(caseId);
        }

        Assertions.assertThat(tasks).hasSize(0);

        // there should not be any active tasks in the engine either
        assignedTasks = runtimeDataService.getTasksAssignedAsPotentialOwner("John", new QueryFilter());
        Assertions.assertThat(assignedTasks).hasSize(0);
        assignedTasks = runtimeDataService.getTasksAssignedAsPotentialOwner("Marian", new QueryFilter());
        Assertions.assertThat(assignedTasks).hasSize(0);

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

        listeners.add(new ObjectModel("mvel", "new org.kie.demo.taskassignment.util.TestPushTaskEventListener(org.kie.demo.taskassignment.planner.PushTaskEventListenerTest.tasks)"));

        return listeners;
    }
}
