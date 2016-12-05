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

package org.kie.demo.taskassignment.test.insurance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.jbpm.casemgmt.api.model.instance.CaseFileInstance;
import org.jbpm.casemgmt.api.model.instance.CaseInstance;
import org.jbpm.casemgmt.api.model.instance.CaseStageInstance;
import org.jbpm.casemgmt.api.model.instance.CommentInstance;
import org.jbpm.document.Document;
import org.jbpm.document.service.impl.DocumentImpl;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.task.impl.model.UserImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.test.insurance.misc.ClaimReport;
import org.kie.demo.taskassignment.test.insurance.misc.PropertyDamageReport;
import org.kie.demo.taskassignment.test.util.AbstractCaseServicesBaseTest;
import org.kie.demo.taskassignment.test.util.CountDownListenerFactory;
import org.kie.internal.query.QueryContext;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.runtime.conf.ObjectModel;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.context.CaseContext;
import org.kie.scanner.MavenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CarInsuranceClaimCaseTest extends AbstractCaseServicesBaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CarInsuranceClaimCaseTest.class);
    private static final String TEST_DOC_STORAGE = "target/docs";
    
    private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();
    
    private static final String CAR_INSURANCE_CLAIM_PROC_ID = "insurance-claims.CarInsuranceClaimCase";
    
    protected static final String CAR_INS_CASE_ID = "CAR_INS-0000000001";

    public static Map<Long, TaskPlanningEntity> tasks = new HashMap<>();
    
    @Before
    public void prepare() {
        System.setProperty("org.jbpm.document.storage", TEST_DOC_STORAGE);
        deleteFolder(TEST_DOC_STORAGE);
        configureServices();
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, VERSION);
        List<String> processes = new ArrayList<String>();
        processes.add("org/kie/demo/taskassignment/test/insurance/CarInsuranceClaimCase.bpmn2");
        processes.add("org/kie/demo/taskassignment/test/insurance/insurance-rules.drl");
        
        
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
        close();
        CountDownListenerFactory.clear();
    }
    
    @Test
    public void testCarInsuranceClaimCase() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case
        String caseId = startAndAssertCaseInstance(deploymentId, "john", "mary");
        try {
            // let's verify case is created
            assertCaseInstance(deploymentId, CAR_INS_CASE_ID);
            // let's look at what stages are active
            assertBuildClaimReportStage();            
            // since the first task assigned to insured is with auto start it should be already active            
            // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
            long taskId = assertBuildClaimReportAvailableForBothRoles();            
            // let's provide claim report with initial data            
            // claim report should be stored in case file data
            provideAndAssertClaimReport(taskId);            
            // now we have another task for insured to provide property damage report
            taskId = assertPropertyDamageReportAvailableForBothRoles();            
            // let's provide the property damage report
            provideAndAssertPropertyDamageReport(taskId);  
            // let's complete the stage by explicitly stating that claimReport is done
            caseService.addDataToCaseFile(CAR_INS_CASE_ID, "claimReportDone", true);
            // we should be in another stage - Claim assessment
            assertClaimAssesmentStage();
            // let's trigger claim offer calculation
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Calculate claim", null);            
            // now we have another task for insured as claim was calculated            
            // let's accept the calculated claim 
            assertAndAcceptClaimOffer();            
            // there should be no process instances for the case
            Collection<ProcessInstanceDesc> caseProcesInstances = caseRuntimeDataService.getProcessInstancesForCase(CAR_INS_CASE_ID, Arrays.asList(ProcessInstance.STATE_ACTIVE),  new QueryContext());
            assertEquals(0, caseProcesInstances.size());   
            caseId = null;
            
        } catch (Exception e) {
            logger.error("Unexpected error {}", e.getMessage(), e);
            fail("Unexpected exception " + e.getMessage());
        } finally {
            if (caseId != null) {
                caseService.cancelCase(caseId);
            }
        }
    }
    
    @Test
    public void testCarInsuranceClaimCaseWithPoliceReport() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case and start it
        String caseId = startAndAssertCaseInstance(deploymentId, "john", "mary");
        try {
            // let's verify case is created
            assertCaseInstance(deploymentId, CAR_INS_CASE_ID);
            // let's look at what stages are active
            assertBuildClaimReportStage();            
            // since the first task assigned to insured is with auto start it should be already active            
            // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
            long taskId = assertBuildClaimReportAvailableForBothRoles();            
            // let's provide claim report with initial data            
            // claim report should be stored in case file data
            provideAndAssertClaimReport(taskId);            
            // now we have another task for insured to provide property damage report
            taskId = assertPropertyDamageReportAvailableForBothRoles();    
            // let's attach police report as document
            attachAndAssertPoliceReport();            
            // let's provide the property damage report
            provideAndAssertPropertyDamageReport(taskId);            
            // let's complete the stage by explicitly stating that claimReport is done
            caseService.addDataToCaseFile(CAR_INS_CASE_ID, "claimReportDone", true);            
            // we should be in another stage - Claim assessment
            assertClaimAssesmentStage();
            // let's trigger claim offer calculation
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Calculate claim", null);            
            // now we have another task for insured as claim was calculated            
            // let's accept the calculated claim 
            assertAndAcceptClaimOffer();            
            // there should be no process instances for the case
            Collection<ProcessInstanceDesc> caseProcesInstances = caseRuntimeDataService.getProcessInstancesForCase(CAR_INS_CASE_ID, Arrays.asList(ProcessInstance.STATE_ACTIVE),  new QueryContext());
            assertEquals(0, caseProcesInstances.size());   
            caseId = null;
            
        } catch (Exception e) {
            logger.error("Unexpected error {}", e.getMessage(), e);
            fail("Unexpected exception " + e.getMessage());
        } finally {
            if (caseId != null) {
                caseService.cancelCase(caseId);
            }
        }
    }
    
    @Test//(timeout=10000)
    public void testCarInsuranceClaimCaseWithContactByInsured() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case
        String caseId = startAndAssertCaseInstance(deploymentId, "john", "mary");
        try {
            // let's verify case is created
            assertCaseInstance(deploymentId, CAR_INS_CASE_ID);
            // let's look at what stages are active
            assertBuildClaimReportStage();            
            // since the first task assigned to insured is with auto start it should be already active            
            // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
            long taskId = assertBuildClaimReportAvailableForBothRoles();            
            // let's provide claim report with initial data            
            // claim report should be stored in case file data
            provideAndAssertClaimReport(taskId);            
            // now we have another task for insured to provide property damage report
            taskId = assertPropertyDamageReportAvailableForBothRoles();            
            // let's provide the property damage report
            provideAndAssertPropertyDamageReport(taskId); 
            
            // before completing claim report, let's call insurance company with some questions
            // when call is answered insurance representative gets a task
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Contacted by insured", null); 
            attachAndAssertPoliceReport(false, null);
            
            // still not satisfied, let's call insurance company with these questions again and ask for callback in 2 sec
            // when call is answered insurance representative gets a task
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Contacted by insured", null); 
            attachAndAssertPoliceReport(true, "2s");
            
            // let's complete the stage by explicitly stating that claimReport is done
            caseService.addDataToCaseFile(CAR_INS_CASE_ID, "claimReportDone", true);
            // we should be in another stage - Claim assessment
            assertClaimAssesmentStage();
            // let's trigger claim offer calculation
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Calculate claim", null);            
            // now we have another task for insured as claim was calculated            
            // let's accept the calculated claim 
            assertAndAcceptClaimOffer();            
            // there should be no process instances for the case
            Collection<ProcessInstanceDesc> caseProcesInstances = caseRuntimeDataService.getProcessInstancesForCase(CAR_INS_CASE_ID, Arrays.asList(ProcessInstance.STATE_ACTIVE),  new QueryContext());
            assertEquals(0, caseProcesInstances.size());   
            caseId = null;
            
        } catch (Exception e) {
            logger.error("Unexpected error {}", e.getMessage(), e);
            fail("Unexpected exception " + e.getMessage());
        } finally {
            if (caseId != null) {
                caseService.cancelCase(caseId);
            }
        }
    }
    
    @Test
    public void testCarInsuranceClaimCaseWithNegotiations() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case
        String caseId = startAndAssertCaseInstance(deploymentId, "john", "mary");
        try {
            // let's verify case is created
            assertCaseInstance(deploymentId, CAR_INS_CASE_ID);
            // let's look at what stages are active
            assertBuildClaimReportStage();            
            // since the first task assigned to insured is with auto start it should be already active            
            // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
            long taskId = assertBuildClaimReportAvailableForBothRoles();            
            // let's provide claim report with initial data            
            // claim report should be stored in case file data
            provideAndAssertClaimReport(taskId);            
            // now we have another task for insured to provide property damage report
            taskId = assertPropertyDamageReportAvailableForBothRoles();            
            // let's provide the property damage report
            provideAndAssertPropertyDamageReport(taskId);  
            // let's complete the stage by explicitly stating that claimReport is done
            caseService.addDataToCaseFile(CAR_INS_CASE_ID, "claimReportDone", true);
            // we should be in another stage - Claim assessment
            assertClaimAssesmentStage();
            // let's trigger claim offer calculation
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Calculate claim", null);            
            // now we have another task for insured as claim was calculated            
            // let's negotiate few times the calculated claim offer 
            assertAndNegotiateClaimOffer(3);            
            // there should be no process instances for the case
            Collection<ProcessInstanceDesc> caseProcesInstances = caseRuntimeDataService.getProcessInstancesForCase(CAR_INS_CASE_ID, Arrays.asList(ProcessInstance.STATE_ACTIVE),  new QueryContext());
            assertEquals(0, caseProcesInstances.size());   
            caseId = null;
            
        } catch (Exception e) {
            logger.error("Unexpected error {}", e.getMessage(), e);
            fail("Unexpected exception " + e.getMessage());
        } finally {
            if (caseId != null) {
                caseService.cancelCase(caseId);
            }
        }
    }
    
    @Test
    public void testCarInsuranceClaimCaseWithAssessorInvolved() {
        String deploymentId = deployAndAssert();
        // let's assign users to roles so they can be participants in the case
        String caseId = startAndAssertCaseInstance(deploymentId, "john", "mary");
        try {
            // let's verify case is created
            assertCaseInstance(deploymentId, CAR_INS_CASE_ID);
            // let's look at what stages are active
            assertBuildClaimReportStage();            
            // let's now add assessor to the case as we will need his/her opinion
            caseService.assignToCaseRole(CAR_INS_CASE_ID, "assessor", new UserImpl("krisv"));
            // since the first task assigned to insured is with auto start it should be already active            
            // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
            long taskId = assertBuildClaimReportAvailableForBothRoles();            
            // let's provide claim report with initial data            
            // claim report should be stored in case file data
            provideAndAssertClaimReport(taskId);            
            // now we have another task for insured to provide property damage report
            taskId = assertPropertyDamageReportAvailableForBothRoles();            
            // let's provide the property damage report
            provideAndAssertPropertyDamageReport(taskId);                        
            // let's complete the stage by explicitly stating that claimReport is done
            caseService.addDataToCaseFile(CAR_INS_CASE_ID, "claimReportDone", true);
            // we should be in another stage - Claim assessment
            assertClaimAssesmentStage();                       
            // now krisv should have a task assigned as assessor
            assertAndRunClaimAssessment();            
            // now we have another task for insured as claim was calculated            
            // let's accept the calculated claim 
            assertAndAcceptClaimOffer();            
            // there should be no process instances for the case
            Collection<ProcessInstanceDesc> caseProcesInstances = caseRuntimeDataService.getProcessInstancesForCase(CAR_INS_CASE_ID, Arrays.asList(ProcessInstance.STATE_ACTIVE),  new QueryContext());
            assertEquals(0, caseProcesInstances.size());   
            caseId = null;
            
        } catch (Exception e) {
            logger.error("Unexpected error {}", e.getMessage(), e);
            fail("Unexpected exception " + e.getMessage());
        } finally {
            if (caseId != null) {
                caseService.cancelCase(caseId);
            }
        }
    }


    /*
     * Helper methods
     */
    protected String deployAndAssert() {
        assertNotNull(deploymentService);        
        DeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        
        deploymentService.deploy(deploymentUnit);
        RuntimeEnvironment environment = ((InternalRuntimeManager)deploymentService.getDeployedUnit(deploymentUnit.getIdentifier()).getRuntimeManager()).getEnvironment();
        UserGroupCallback callback = environment.getUserGroupCallback();
        units.add(deploymentUnit);
        
        return deploymentUnit.getIdentifier();
    }

    protected void assertComment(CommentInstance comment, String author, String content) {
        assertNotNull(comment);
        assertEquals(author, comment.getAuthor());
        assertEquals(content, comment.getComment());
    }
    
    protected void assertTask(TaskSummary task, String actor, String name, Status status) {
        assertNotNull(task);            
        assertEquals(name, task.getName());
        assertEquals(actor, task.getActualOwnerId());
        assertEquals(status, task.getStatus());
    }
    
    protected String startAndAssertCaseInstance(String deploymentId, String insured, String insuranceRepresentative) {
        Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
        roleAssignments.put("insured", new UserImpl(insured));
        roleAssignments.put("insuranceRepresentative", new UserImpl(insuranceRepresentative));
        
        // start new instance of a case with data and role assignment
        Map<String, Object> data = new HashMap<>();
        CaseFileInstance caseFile = caseService.newCaseFileInstance(deploymentId, CAR_INSURANCE_CLAIM_PROC_ID, data, roleAssignments);
        String caseId = caseService.startCase(deploymentId, CAR_INSURANCE_CLAIM_PROC_ID, caseFile);
        assertNotNull(caseId);
        assertEquals(CAR_INS_CASE_ID, caseId);

        /*RuntimeEnvironment environment = ((EventService<TaskLifeCycleEventListener>)deploymentService.getDeployedUnit(units.get(0).getIdentifier()).
                getRuntimeManager().getRuntimeEngine(CaseContext.get("CAR_INS-0000000001")).getTaskService());*/

        TaskService taskService = deploymentService.getDeployedUnit(units.get(0).getIdentifier()).
                getRuntimeManager().getRuntimeEngine(CaseContext.get("CAR_INS-0000000001")).getTaskService();
        
        return caseId;
    }
    
    protected void assertCaseInstance(String deploymentId, String caseId) {
        CaseInstance cInstance = caseService.getCaseInstance(caseId);
        assertNotNull(cInstance);
        assertEquals(caseId, cInstance.getCaseId());
        assertEquals(deploymentId, cInstance.getDeploymentId());
    }
    
    protected void assertBuildClaimReportStage() {
        Collection<CaseStageInstance> activeStages = caseRuntimeDataService.getCaseInstanceStages(CAR_INS_CASE_ID, true, new QueryContext());
        assertEquals(1, activeStages.size());
        CaseStageInstance stage = activeStages.iterator().next();
        assertEquals("Build claim report", stage.getName());
    }
    
    protected void assertClaimAssesmentStage() {
        Collection<CaseStageInstance> activeStages = caseRuntimeDataService.getCaseInstanceStages(CAR_INS_CASE_ID, true, new QueryContext());
        assertEquals(1, activeStages.size());
        CaseStageInstance stage = activeStages.iterator().next();
        assertEquals("Claim assesment", stage.getName());
    }
    
    protected long assertBuildClaimReportAvailableForBothRoles() {        
        return assertTasksForBothRoles("Provide accident information", "john", "mary", Status.Ready);
    }
    
    protected long assertPropertyDamageReportAvailableForBothRoles() {           
        return assertTasksForBothRoles("File property damage claim", "john", "mary", Status.Ready);
    }
    
    protected long assertTasksForBothRoles(String taskName, String actor1, String actor2, Status status) {
        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner(actor1, new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), null, taskName, status);
        // the same task can be claimed by insuranceRepresentative in case claim is reported over phone
        userTaskService.getTask(tasks.get(0).getId()).getPeopleAssignments().getPotentialOwners().forEach(
                organizationalEntity -> System.out.println("<<<<< " + organizationalEntity.getId())
        );
        tasks = runtimeDataService.getTasksAssignedAsPotentialOwner(actor2, new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), null, taskName, status);
        
        return tasks.get(0).getId();
    }
    
    protected void provideAndAssertClaimReport(Long taskId) {
        ClaimReport claimReport = new ClaimReport();
        claimReport.setName("John Doe");
        claimReport.setAddress("Main street, NY");
        claimReport.setAccidentDescription("It happened so sudden...");
        claimReport.setAccidentDate(new Date());
        
        Map<String, Object> params = new HashMap<>();
        params.put("claimReport_", claimReport);
        userTaskService.completeAutoProgress(taskId, "john", params);
        
        // claim report should be stored in case file data
        CaseFileInstance caseFile = caseService.getCaseFileInstance(CAR_INS_CASE_ID);
        assertNotNull(caseFile);
        ClaimReport caseClaimReport = (ClaimReport) caseFile.getData("claimReport");
        assertNotNull(caseClaimReport);
    }
    
    protected void provideAndAssertPropertyDamageReport(Long taskId) {
        PropertyDamageReport damageReport = new PropertyDamageReport("Car is completely destroyed", 1000.0);
        Map<String, Object> params = new HashMap<>();
        params.put("propertyDamageReport_", damageReport);
        userTaskService.completeAutoProgress(taskId, "john", params);
        
        // property damage report should be stored in case file data
        CaseFileInstance caseFile = caseService.getCaseFileInstance(CAR_INS_CASE_ID);
        assertNotNull(caseFile);
        PropertyDamageReport casePropertyDamageReport = (PropertyDamageReport) caseFile.getData("propertyDamageReport");
        assertNotNull(casePropertyDamageReport);
    }
    
    protected void assertAndAcceptClaimOffer() {
        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("john", new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), "john", "Present calculated claim", Status.Reserved);
        
        // let's accept the calculated claim 
        Map<String, Object> params = new HashMap<>();
        params.put("accepted", true);
        userTaskService.completeAutoProgress(tasks.get(0).getId(), "john", params);
    }
    
    protected void attachAndAssertPoliceReport() {
        caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Submit police report", null);

        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("john", new QueryFilter());
        assertNotNull(tasks);
        assertEquals(2, tasks.size());            
        assertTask(tasks.get(0), null, "Submit police report", Status.Ready);
        assertTask(tasks.get(1), null, "File property damage claim", Status.Ready);
        
        byte[] docContent = "police report content".getBytes();
        DocumentImpl document = new DocumentImpl(UUID.randomUUID().toString(), "car-accident-police-report.txt", docContent.length, new Date());
        document.setContent(docContent);

        Map<String, Object> params = new HashMap<>();
        params.put("policeReport_", document);
        userTaskService.completeAutoProgress(tasks.get(0).getId(), "john", params);
        
        // police report should be stored in case file data
        CaseFileInstance caseFile = caseService.getCaseFileInstance(CAR_INS_CASE_ID);
        assertNotNull(caseFile);
        Document policeReport = (Document) caseFile.getData("policeReport");
        assertNotNull(policeReport);
        assertEquals("car-accident-police-report.txt", policeReport.getName());
    }
    
    protected void attachAndAssertPoliceReport(boolean callback, String callbackAfter) {        

        List<TaskSummary> tasks = runtimeDataService.getTasksOwned("mary", new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), "mary", "Contacted by insured", Status.Reserved);
        
        Map<String, Object> params = new HashMap<>();
        params.put("callback_", callback);
        if (callback) {
            params.put("callbackAfter_", callbackAfter);    
        }
        userTaskService.completeAutoProgress(tasks.get(0).getId(), "mary", params);
        
        if (callback) {
            CountDownListenerFactory.getExisting("carInsuranceCase").waitTillCompleted();
            tasks = runtimeDataService.getTasksOwned("mary", new QueryFilter());
            assertNotNull(tasks);
            assertEquals(1, tasks.size());            
            assertTask(tasks.get(0), "mary", "Requested callback", Status.Reserved);
            
            userTaskService.completeAutoProgress(tasks.get(0).getId(), "mary", null);
        }
    }
    
    protected void assertAndNegotiateClaimOffer(int numberOfNegotiations) {
        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("john", new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), "john", "Present calculated claim", Status.Reserved);
        
        // let's accept the calculated claim 
        Map<String, Object> params = new HashMap<>();
        params.put("accepted", false);
        userTaskService.completeAutoProgress(tasks.get(0).getId(), "john", params);
        
        Collection<CaseStageInstance> activeStages = caseRuntimeDataService.getCaseInstanceStages(CAR_INS_CASE_ID, true, new QueryContext());
        assertEquals(1, activeStages.size());
        CaseStageInstance stage = activeStages.iterator().next();
        assertEquals("Escalate rejected claim", stage.getName());
        
        while (numberOfNegotiations > 0) {
            params.clear();
            params.put("Offer", 1000);
            
            caseService.triggerAdHocFragment(CAR_INS_CASE_ID, "Negotiation meeting", params);
            
            tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("john", new QueryFilter());
            assertNotNull(tasks);
            assertEquals(1, tasks.size());            
            assertTask(tasks.get(0), null, "Negotiation meeting", Status.Ready);
            
            boolean accepted = false;
            if (numberOfNegotiations == 1) {
                accepted = true;
            }
            
            params.put("accepted", accepted);
            userTaskService.completeAutoProgress(tasks.get(0).getId(), "john", params);
            
            numberOfNegotiations--;
        }
    }
    
    
    private void assertAndRunClaimAssessment() {
        List<TaskSummary> tasks = runtimeDataService.getTasksAssignedAsPotentialOwner("krisv", new QueryFilter());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());            
        assertTask(tasks.get(0), "krisv", "Assessor evaluation", Status.Reserved);
        
        long taskId = tasks.get(0).getId();
        
        Map<String, Object> taskInput = userTaskService.getTaskInputContentByTaskId(taskId);
        assertNotNull(taskInput);
        assertTrue(taskInput.containsKey("_claimReport"));
        
        ClaimReport claimReport = (ClaimReport) taskInput.get("_claimReport");
        claimReport.setAmount(20000.0);
        claimReport.setCalculated(Boolean.TRUE);
        Map<String, Object> params = new HashMap<>();
        params.put("claimReport_", claimReport);
        userTaskService.completeAutoProgress(taskId, "krisv", params);
    }

    @Override
    protected List<ObjectModel> getProcessListeners() {
        List<ObjectModel> listeners = super.getProcessListeners();
        
        listeners.add(new ObjectModel("mvel", "org.kie.demo.taskassignment.test.util.CountDownListenerFactory.get(\"carInsuranceCase\", \"wait before callback\", 1)"));
        
        return listeners;
    }
    
}