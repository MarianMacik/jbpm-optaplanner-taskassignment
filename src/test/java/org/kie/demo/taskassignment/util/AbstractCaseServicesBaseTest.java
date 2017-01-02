/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.demo.taskassignment.util;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jbpm.casemgmt.api.CaseRuntimeDataService;
import org.jbpm.casemgmt.api.CaseService;
import org.jbpm.casemgmt.api.generator.CaseIdGenerator;
import org.jbpm.casemgmt.impl.CaseRuntimeDataServiceImpl;
import org.jbpm.casemgmt.impl.CaseServiceImpl;
import org.jbpm.casemgmt.impl.event.CaseConfigurationDeploymentListener;
import org.jbpm.casemgmt.impl.generator.TableCaseIdGenerator;
import org.jbpm.casemgmt.impl.marshalling.CaseMarshallerFactory;
import org.jbpm.kie.services.impl.FormManagerServiceImpl;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
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
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.audit.TaskAuditServiceFactory;
import org.jbpm.services.task.identity.DBUserGroupCallbackImpl;
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
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.conf.DeploymentDescriptorBuilder;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCaseServicesBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCaseServicesBaseTest.class);

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

    protected void close() {
        if (emf != null) {
            emf.close();
        }
        EntityManagerFactoryManager.get().clear();
        closeDataSource();
    }

    protected void configureServices() {
        buildDatasource();
        emf = EntityManagerFactoryManager.get().getOrCreate("org.jbpm.domain");
        identityProvider = new TestIdentityProvider();

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

    protected void insertUsersAndGroupsToDB() {
        UserJSONParser parser = new UserJSONParser(emf);
        File file = new File("src/test/resources/org/kie/demo/taskassignment/db/testUsers.json");

        parser.insertUsersFromFile(file);
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

    public static void cleanupSingletonSessionId() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (tempDir.exists()) {

            String[] jbpmSerFiles = tempDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {

                    return name.endsWith("-jbpmSessionId.ser");
                }
            });
            for (String file : jbpmSerFiles) {
                logger.debug("Temp dir to be removed {} file {}", tempDir, file);
                new File(tempDir, file).delete();
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

    protected List<ObjectModel> getProcessListeners() {
        return new ArrayList<>();
    }

    protected List<ObjectModel> getTaskEventListeners() {
        return new ArrayList<>();
    }

}
