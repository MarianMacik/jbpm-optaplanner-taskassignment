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
import org.kie.api.task.model.TaskSummary;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;
import org.kie.demo.taskassignment.planner.domain.UserServiceUtil;
import org.kie.demo.taskassignment.test.util.AbstractCaseServicesBaseTest;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.scanner.MavenRepository;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAssigningTest extends AbstractCaseServicesBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssigningTest.class);
    private static final String TEST_DOC_STORAGE = "target/docs";

    // Overridden from parent
    protected static final String GROUP_ID = "org.kie.demo";
    protected static final String ARTIFACT_ID = "taskassignment-cases";
    protected static final String VERSION = "1.0.0-SNAPSHOT";

    private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();

    private List<String> activeCaseIds = new ArrayList<>();

    private static final String TASKS_TO_ASSIGN_PROC_ID = "OptaplannerTasks.TasksToAssign";

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
    public void testTaskAssigning(){
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.createFromXmlResource("org/kie/demo/taskassignment/planner/TaskAssigningSolverConfig.xml");
        Solver<TaskAssigningSolution> solver = solverFactory.buildSolver();

        TaskAssigningSolution solution = new TaskAssigningSolution();
        solution.setTaskList(tasks);
        solution.setUserList(users);

        String deploymentId = deployAndAssert();


        for (int i = 0; i < 50; i++) {
            String caseId = startCase(deploymentId, TASKS_TO_ASSIGN_PROC_ID);
            activeCaseIds.add(caseId);
        }

        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("marian", new QueryFilter());

        // Marian will claim a few tasks, so these tasks can only be assigned to him
//        for (int i = 0; i < 6; i++) {
//            userTaskService.claim(tasks.get(i).getId(), "marian");
//            System.out.println("Marian claimed taskId " + tasks.get(i).getId());
//        }

        TaskAssigningSolution solvedSolution = solver.solve(solution);


        // print planned tasks for each user and try to claim, start and complete them in the engine,
        // if this proceeds without an exception, the test is successful
        solvedSolution.getUserList().forEach(user -> {
            System.out.println("Tasks for user " + user.getName() + " :");
            TaskPlanningEntity actualTask = user.getNextTask();
            while (actualTask != null) {
                System.out.println(actualTask.getName() + " " + actualTask.getId() + " - startTime: " + actualTask.getStartTime() + " + duration: "
                        + actualTask.getBaseDuration() + " x " + actualTask.getUser().getSkills().get(actualTask.getSkill()).getSkillLevel().getDurationMultiplier() + " = endTime: " + actualTask.getEndTime());
                userTaskService.completeAutoProgress(actualTask.getId(), user.getName(), new HashMap<>());
                actualTask = actualTask.getNextTask();
            }
        });




        activeCaseIds.forEach(caseId -> caseService.cancelCase(caseId));


//        if (caseId != null) {
//            caseService.cancelCase(caseId);
//        }


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

    protected String startCase(String deploymentId, String caseDefinitionId) {
        // let's assign users to roles so they can be participants in the case

        Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
        roleAssignments.put("manager", new UserImpl("john"));
        roleAssignments.put("supplier", new UserImpl("marian"));
        roleAssignments.put("SUPP", new GroupImpl("suppliers"));


        // start new instance of a case with data and role assignment
        Map<String, Object> data = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, caseDefinitionId, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, caseDefinitionId, caseFile);

        return caseId;
    }

    @Override
    protected List<ObjectModel> getTaskEventListeners() {
        List<ObjectModel> listeners = super.getTaskEventListeners();

        listeners.add(new ObjectModel("mvel", "new org.kie.demo.taskassignment.test.util.PushTaskEventListener(org.kie.demo.taskassignment.planner.TaskAssigningTest.tasks)"));

        return listeners;
    }
}
