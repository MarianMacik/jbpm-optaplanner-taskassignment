package org.kie.demo.taskassignment.app.services;

import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jbpm.casemgmt.api.CaseRuntimeDataService;
import org.jbpm.casemgmt.api.CaseService;
import org.jbpm.casemgmt.api.generator.CaseIdGenerator;
import org.jbpm.casemgmt.api.model.instance.CaseFileInstance;
import org.jbpm.casemgmt.impl.CaseRuntimeDataServiceImpl;
import org.jbpm.casemgmt.impl.CaseServiceImpl;
import org.jbpm.casemgmt.impl.event.CaseConfigurationDeploymentListener;
import org.jbpm.casemgmt.impl.generator.TableCaseIdGenerator;
import org.jbpm.casemgmt.impl.marshalling.CaseMarshallerFactory;
import org.jbpm.kie.services.impl.FormManagerServiceImpl;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.ProcessServiceImpl;
import org.jbpm.kie.services.impl.RuntimeDataServiceImpl;
import org.jbpm.kie.services.impl.UserTaskServiceImpl;
import org.jbpm.kie.services.impl.bpmn2.BPMN2DataServiceImpl;
import org.jbpm.kie.services.impl.query.QueryServiceImpl;
import org.jbpm.kie.services.impl.security.DeploymentRolesManager;
import org.jbpm.runtime.manager.impl.RuntimeManagerFactoryImpl;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorImpl;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.audit.TaskAuditServiceFactory;
import org.jbpm.services.task.identity.DBUserGroupCallbackImpl;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.User;
import org.kie.demo.taskassignment.util.TestIdentityProvider;
import org.kie.demo.taskassignment.util.UserJSONParser;
import org.kie.demo.taskassignment.util.UserServiceUtil;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.conf.DeploymentDescriptorBuilder;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.scanner.MavenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that initializes all services of the jBPM engine and DB connection
 */
public class Services {

    private static final Logger logger = LoggerFactory.getLogger(Services.class);

    protected static final String GROUP_ID = "org.kie.demo";
    protected static final String ARTIFACT_ID = "taskassignment-cases";
    protected static final String VERSION = "1.0.0-SNAPSHOT";
    private static final String ONE_TASK_CASE_ID = "OptaplannerTasks.OneTask";

    protected PoolingDataSource ds;

    protected EntityManagerFactory emf;
    protected DeploymentService deploymentService;
    protected DefinitionService bpmn2Service;
    protected RuntimeDataService runtimeDataService;
    protected ProcessService processService;
    protected UserTaskService userTaskService;
    protected QueryService queryService;

    protected TaskService taskService;

    protected CaseRuntimeDataService caseRuntimeDataService;
    protected CaseService caseService;

    protected TestIdentityProvider identityProvider;
    protected CaseIdGenerator caseIdGenerator;

    private UserJSONParser userJSONParser;

    private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();

    private List<String> activeCaseIds = new ArrayList<>();

    private String deploymentId;

    public void close() {
        if (emf != null) {
            emf.close();
        }
        EntityManagerFactoryManager.get().clear();
        closeDataSource();
    }

    public Services() {
        configureServices();
        // users are inserted when the scenario is loaded
        deployCases();
    }

    private void configureServices() {
        buildDatasource();
        emf = EntityManagerFactoryManager.get().getOrCreate("org.jbpm.domain");
        identityProvider = new TestIdentityProvider();

        userJSONParser = new UserJSONParser(emf);
        UserServiceUtil.setEmf(emf);

        // build definition service
        bpmn2Service = new BPMN2DataServiceImpl();

        DeploymentRolesManager deploymentRolesManager = new DeploymentRolesManager();

        queryService = new QueryServiceImpl();
        ((QueryServiceImpl) queryService).setIdentityProvider(identityProvider);
        ((QueryServiceImpl) queryService).setCommandService(new TransactionalCommandService(emf));
        ((QueryServiceImpl) queryService).init();

        // build deployment service
        deploymentService = new KModuleDeploymentService();
        ((KModuleDeploymentService) deploymentService).setBpmn2Service(bpmn2Service);
        ((KModuleDeploymentService) deploymentService).setEmf(emf);
        ((KModuleDeploymentService) deploymentService).setIdentityProvider(identityProvider);
        ((KModuleDeploymentService) deploymentService).setManagerFactory(new RuntimeManagerFactoryImpl());
        ((KModuleDeploymentService) deploymentService).setFormManagerService(new FormManagerServiceImpl());

        UserGroupCallback userGroupCallback = new DBUserGroupCallbackImpl(buildUserGroupCallbackProperties());

        taskService = HumanTaskServiceFactory.newTaskServiceConfigurator().entityManagerFactory(emf).userGroupCallback(userGroupCallback).getTaskService();

        // build runtime data service
        runtimeDataService = new RuntimeDataServiceImpl();
        ((RuntimeDataServiceImpl) runtimeDataService).setCommandService(new TransactionalCommandService(emf));
        ((RuntimeDataServiceImpl) runtimeDataService).setIdentityProvider(identityProvider);
        ((RuntimeDataServiceImpl) runtimeDataService).setTaskService(taskService);
        ((RuntimeDataServiceImpl) runtimeDataService).setDeploymentRolesManager(deploymentRolesManager);
        ((RuntimeDataServiceImpl) runtimeDataService).setTaskAuditService(TaskAuditServiceFactory.newTaskAuditServiceConfigurator().setTaskService(taskService).getTaskAuditService());
        ((KModuleDeploymentService) deploymentService).setRuntimeDataService(runtimeDataService);

        // build process service
        processService = new ProcessServiceImpl();
        ((ProcessServiceImpl) processService).setDataService(runtimeDataService);
        ((ProcessServiceImpl) processService).setDeploymentService(deploymentService);

        // build user task service
        userTaskService = new UserTaskServiceImpl();
        ((UserTaskServiceImpl) userTaskService).setDataService(runtimeDataService);
        ((UserTaskServiceImpl) userTaskService).setDeploymentService(deploymentService);

        // build case id generator
        caseIdGenerator = new TableCaseIdGenerator(new TransactionalCommandService(emf));

        // build case runtime data service
        caseRuntimeDataService = new CaseRuntimeDataServiceImpl();
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setCaseIdGenerator(caseIdGenerator);
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setRuntimeDataService(runtimeDataService);
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setCommandService(new TransactionalCommandService(emf));
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setIdentityProvider(identityProvider);
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setDeploymentRolesManager(deploymentRolesManager);

        // build case service
        caseService = new CaseServiceImpl();
        ((CaseServiceImpl) caseService).setCaseIdGenerator(caseIdGenerator);
        ((CaseServiceImpl) caseService).setCaseRuntimeDataService(caseRuntimeDataService);
        ((CaseServiceImpl) caseService).setProcessService(processService);
        ((CaseServiceImpl) caseService).setDeploymentService(deploymentService);
        ((CaseServiceImpl) caseService).setRuntimeDataService(runtimeDataService);

        CaseConfigurationDeploymentListener configurationListener = new CaseConfigurationDeploymentListener();

        // set runtime data service as listener on deployment service
        ((KModuleDeploymentService) deploymentService).addListener((RuntimeDataServiceImpl) runtimeDataService);
        ((KModuleDeploymentService) deploymentService).addListener((BPMN2DataServiceImpl) bpmn2Service);
        ((KModuleDeploymentService) deploymentService).addListener((QueryServiceImpl) queryService);
        ((KModuleDeploymentService) deploymentService).addListener((CaseRuntimeDataServiceImpl) caseRuntimeDataService);
        ((KModuleDeploymentService) deploymentService).addListener(configurationListener);
    }

    public void resetCurrentScenario() {
        for(String caseId : activeCaseIds) {
            caseService.cancelCase(caseId);
        }
        activeCaseIds.clear();
        deleteUsersFromDB();
    }

    public void insertSmallScenarioUsers() {
        File file = new File("src/main/resources/org/kie/demo/taskassignment/db/users.json");

        userJSONParser.insertUsersFromFile(file);
    }

    public void insertMediumScenarioUsers() {
        // Insert small scenario as well
        insertSmallScenarioUsers();
        File file = new File("src/main/resources/org/kie/demo/taskassignment/db/users2.json");
        userJSONParser.insertUsersFromFile(file);
    }

    public void insertLargeScenarioUsers() {
        // Insert medium scenario as well
        insertMediumScenarioUsers();
        File file = new File("src/main/resources/org/kie/demo/taskassignment/db/users3.json");
        userJSONParser.insertUsersFromFile(file);
    }

    public List<User> getAllUsers() {
        return UserServiceUtil.getAllUsers();
    }

    public List<String> getAllUserNames() {
        return UserServiceUtil.getAllUserNames();
    }

    public List<String> getAllGroupNames() {
        return UserServiceUtil.getAllGroupNames();
    }

    public List<String> getAllSkillNames() {
        return UserServiceUtil.getAllSkillNames();
    }


    public void deployCases() {
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        List<String> processes = new ArrayList<String>();
        processes.add("org/kie/demo/taskassignment/planner/TasksToAssign.bpmn2");
        processes.add("org/kie/demo/taskassignment/planner/TasksToAssign2.bpmn2");
        processes.add("org/kie/demo/taskassignment/planner/TasksToAssign3.bpmn2");
        processes.add("org/kie/demo/taskassignment/planner/TasksToAssign4.bpmn2");
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

        // Perform the actual deployment on the engine
        DeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);

        deploymentService.deploy(deploymentUnit);

        units.add(deploymentUnit);
        deploymentId = deploymentUnit.getIdentifier();
    }

    public String startCase(String caseDefinitionId, Map<String, OrganizationalEntity> roleAssignments) {
        // start new instance of a case with data and role assignment
        Map<String, Object> data = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, caseDefinitionId, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, caseDefinitionId, caseFile);

        activeCaseIds.add(caseId);

        return caseId;
    }

    public void startCustomTask(String taskName, String skillNeeded, Integer baseDuration, String priority, List<String> usersToAssign, List<String> groupsToAssign) {
        Map<String, Object> data = new HashMap<>();
        data.put("TaskName", taskName);
        data.put("Skill", skillNeeded);
        data.put("BaseDuration", baseDuration);
        Integer priorityNumber = 0;
        switch (Priority.valueOf(priority)) {
            case LOW:
                priorityNumber = 1;
                break;
            case MEDIUM:
                priorityNumber = 2;
                break;
            case HIGH:
                priorityNumber = 3;
                break;
        }
        data.put("Priority", priorityNumber);

        // assignments will be given later
        Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, ONE_TASK_CASE_ID, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, ONE_TASK_CASE_ID, caseFile);
        activeCaseIds.add(caseId);
        // assign users to a users role
        usersToAssign.forEach(user -> caseService.assignToCaseRole(caseId, "users", new UserImpl(user)));

        // assign groups to a groups role
        groupsToAssign.forEach(group -> caseService.assignToCaseRole(caseId, "groups", new GroupImpl(group)));

        // now we will trigger task, so it should be pushed to a solution
        caseService.triggerAdHocFragment(caseId, "#{caseFile_TaskName}", null);
    }

    protected String getPom(ReleaseId releaseId, ReleaseId... dependencies) {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" + "  <modelVersion>4.0.0</modelVersion>\n" + "\n" + "  <groupId>" + releaseId.getGroupId() + "</groupId>\n" + "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" + "  <version>" + releaseId.getVersion() + "</version>\n" + "\n";
        if (dependencies != null && dependencies.length > 0) {
            pom += "<dependencies>\n";
            for (ReleaseId dep : dependencies) {
                pom += "<dependency>\n";
                pom += "  <groupId>" + dep.getGroupId() + "</groupId>\n";
                pom += "  <artifactId>" + dep.getArtifactId() + "</artifactId>\n";
                pom += "  <version>" + dep.getVersion() + "</version>\n";
                pom += "</dependency>\n";
            }
            pom += "</dependencies>\n";
        }
        pom += "</project>";
        return pom;
    }

    protected InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, List<String> resources) {
        return createKieJar(ks, releaseId, resources, null);
    }

    protected InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, List<String> resources, Map<String, String> extraResources) {

        KieFileSystem kfs = createKieFileSystemWithKProject(ks);
        kfs.writePomXML(getPom(releaseId));
        // set the deployment descriptor so we use per case runtime strategy
        DeploymentDescriptor customDescriptor = new DeploymentDescriptorImpl("org.jbpm.domain");
        DeploymentDescriptorBuilder ddBuilder = customDescriptor.getBuilder().runtimeStrategy(RuntimeStrategy.PER_CASE).addMarshalingStrategy(new ObjectModel("mvel", CaseMarshallerFactory.builder().withDoc().toString()));

        for (ObjectModel listener : getProcessListeners()) {
            ddBuilder.addEventListener(listener);
        }

        for (ObjectModel taskListener : getTaskEventListeners()) {
            ddBuilder.addTaskEventListener(taskListener);
        }


        if (extraResources == null) {
            extraResources = new HashMap<String, String>();
        }
        extraResources.put("src/main/resources/" + DeploymentDescriptor.META_INF_LOCATION, customDescriptor.toXml());

        for (String resource : resources) {
            kfs.write("src/main/resources/KBase-test/" + resource, ResourceFactory.newClassPathResource(resource));
        }
        if (extraResources != null) {
            for (Map.Entry<String, String> entry : extraResources.entrySet()) {
                kfs.write(entry.getKey(), ResourceFactory.newByteArrayResource(entry.getValue().getBytes()));
            }
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        if (!kieBuilder.buildAll().getResults().getMessages().isEmpty()) {
            for (Message message : kieBuilder.buildAll().getResults().getMessages()) {
                logger.error("Error Message: ({}) {}", message.getPath(), message.getText());
            }
            throw new RuntimeException("There are errors builing the package, please check your knowledge assets!");
        }

        return (InternalKieModule) kieBuilder.getKieModule();
    }

    protected KieFileSystem createKieFileSystemWithKProject(KieServices ks) {
        KieModuleModel kproj = ks.newKieModuleModel();

        KieBaseModel kieBaseModel1 = kproj.newKieBaseModel("KBase-test").setDefault(true).addPackage("*").setEqualsBehavior(EqualityBehaviorOption.EQUALITY).setEventProcessingMode(EventProcessingOption.STREAM);

        KieSessionModel ksessionModel = kieBaseModel1.newKieSessionModel("ksession-test");

        ksessionModel.setDefault(true).setType(KieSessionModel.KieSessionType.STATEFUL).setClockType(ClockTypeOption.get("realtime"));

        ksessionModel.newWorkItemHandlerModel("Log", "new org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler()");
        ksessionModel.newWorkItemHandlerModel("Service Task", "new org.jbpm.bpmn2.handler.ServiceTaskHandler(\"name\")");

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kproj.toXML());
        return kfs;
    }

    protected void buildDatasource() {
        ds = new PoolingDataSource();
        ds.setUniqueName("jdbc/testDS1");

        //NON XA CONFIGS
        ds.setClassName("org.h2.jdbcx.JdbcDataSource");
        ds.setMaxPoolSize(3);
        ds.setAllowLocalTransactions(true);
        ds.getDriverProperties().put("user", "sa");
        ds.getDriverProperties().put("password", "sasa");
        ds.getDriverProperties().put("URL", "jdbc:h2:mem:mydb");

        ds.init();
    }

    protected void closeDataSource() {
        if (ds != null) {
            ds.close();
        }
    }

    protected Properties buildUserGroupCallbackProperties() {
        Properties props = new Properties();
        props.setProperty(DBUserGroupCallbackImpl.DS_JNDI_NAME, "jdbc/testDS1");
        props.setProperty(DBUserGroupCallbackImpl.PRINCIPAL_QUERY, "select name from Users where name = ?");
        props.setProperty(DBUserGroupCallbackImpl.ROLES_QUERY, "select name from Groups where name = ?");
        props.setProperty(DBUserGroupCallbackImpl.USER_ROLES_QUERY, "select g.name from Users as u inner join " +
                "Users_Groups as ug inner join " +
                "Groups as g on u.id=ug.user_id and ug.group_id=g.id where u.name = ?");

        return props;
    }

    private void deleteUsersFromDB() {

        EntityManager em = emf.createEntityManager();
        try {
            UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            utx.begin();
            em.joinTransaction();

            em.createQuery("DELETE FROM UserGroupEntity").executeUpdate();
            em.createQuery("DELETE FROM UserSkillEntity").executeUpdate();
            em.createQuery("DELETE FROM GroupEntity").executeUpdate();
            em.createQuery("DELETE FROM SkillEntity").executeUpdate();
            em.createQuery("DELETE FROM UserEntity").executeUpdate();

            utx.commit();
        } catch (Exception e) {
            throw new RuntimeException("Problem deleting users from DB", e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    public void setDeploymentService(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    public void setBpmn2Service(DefinitionService bpmn2Service) {
        this.bpmn2Service = bpmn2Service;
    }

    public void setRuntimeDataService(RuntimeDataService runtimeDataService) {
        this.runtimeDataService = runtimeDataService;
    }

    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }

    public void setUserTaskService(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    public void setQueryService(QueryService queryService) {
        this.queryService = queryService;
    }

    public void setIdentityProvider(TestIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    public void setCaseRuntimeDataService(CaseRuntimeDataService caseRuntimeDataService) {
        this.caseRuntimeDataService = caseRuntimeDataService;
    }

    public DeploymentService getDeploymentService() {
        return deploymentService;
    }

    public DefinitionService getBpmn2Service() {
        return bpmn2Service;
    }

    public RuntimeDataService getRuntimeDataService() {
        return runtimeDataService;
    }

    public ProcessService getProcessService() {
        return processService;
    }

    public UserTaskService getUserTaskService() {
        return userTaskService;
    }

    public QueryService getQueryService() {
        return queryService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public CaseRuntimeDataService getCaseRuntimeDataService() {
        return caseRuntimeDataService;
    }

    public CaseService getCaseService() {
        return caseService;
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    protected List<ObjectModel> getProcessListeners() {
        return new ArrayList<>();
    }

    protected List<ObjectModel> getTaskEventListeners() {
        List<ObjectModel> listeners = new ArrayList<>();
        listeners.add(new ObjectModel("mvel", "org.kie.demo.taskassignment.util.PushTaskEventListenerFactory.get(org.kie.demo.taskassignment.app.MainApp.tasks, org.kie.demo.taskassignment.app.MainApp.solutionController)"));
        return listeners;
    }

}
