package org.kie.demo.taskassignment.app.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.demo.taskassignment.app.MainApp;
import org.kie.demo.taskassignment.app.services.Services;
import org.kie.demo.taskassignment.planner.domain.User;
import org.kie.demo.taskassignment.util.UserServiceUtil;

/**
 * Controller that loads scenarios and manages them
 */
public class CaseController {

    private static final String TASKS_TO_ASSIGN_CASE_ID = "OptaplannerTasks.TasksToAssign";
    private static final String TASKS_TO_ASSIGN2_CASE_ID = "OptaplannerTasks.TasksToAssign2";
    private static final String TASKS_TO_ASSIGN3_CASE_ID = "OptaplannerTasks.TasksToAssign3";
    private static final String TASKS_TO_ASSIGN4_CASE_ID = "OptaplannerTasks.TasksToAssign4";

    private MainApp mainApp;

    private Services services;

    @FXML
    private Button smallScenarioButton;

    @FXML
    private Button mediumScenarioButton;

    @FXML
    private Button largeScenarioButton;

    @FXML
    private Button createCustomTaskButton;

    public MainApp getMainApp() {
        return mainApp;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public CaseController() {
    }

    @FXML
    private void initialize() {
        createCustomTaskButton.setDisable(true);
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Button getCreateCustomTaskButton() {

        return createCustomTaskButton;
    }

    public void setCreateCustomTaskButton(Button createCustomTaskButton) {
        this.createCustomTaskButton = createCustomTaskButton;
    }

    @FXML
    public void handleLoadSmallScenario() {
        setSolutionLoading(true);
        Task<Void> scenarioTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    services.resetCurrentScenario();
                    // delete tasks in task list, event listener deletes tasks only from working solution task list
                    MainApp.tasks.clear();
                    mainApp.clearSolution();
                    // also disable buttons before doing this task
                    services.insertSmallScenarioUsers();
                    List<User> users = services.getAllUsers();

                    Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
                    roleAssignments.put("manager", new UserImpl("John"));
                    roleAssignments.put("supplier", new UserImpl("Marian"));
                    roleAssignments.put("suppliers", new GroupImpl("suppliersGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("managers", new GroupImpl("managersGroup"));
                    roleAssignments.put("officeEmployees", new GroupImpl("employeesGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN2_CASE_ID, roleAssignments);
                    }

                    mainApp.setUsers(users);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                mainApp.setSolution();
                setSolutionLoading(false);
            }
        };
        //scenarioTask.setOnSucceeded(event -> );
        new Thread(scenarioTask).start();
        //createCustomTaskButton.setDisable(false);
        // re-enable buttons after the solution is loaded
    }

    @FXML
    public void handleLoadMediumScenario() {
        setSolutionLoading(true);
        Task<Void> scenarioTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    services.resetCurrentScenario();
                    // delete tasks in task list, event listener deletes tasks only from working solution task list
                    MainApp.tasks.clear();
                    mainApp.clearSolution();

                    services.insertMediumScenarioUsers();
                    List<User> users = services.getAllUsers();

                    Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
                    roleAssignments.put("manager", new UserImpl("John"));
                    roleAssignments.put("supplier", new UserImpl("Marian"));
                    roleAssignments.put("suppliers", new GroupImpl("suppliersGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("managers", new GroupImpl("managersGroup"));
                    roleAssignments.put("officeEmployees", new GroupImpl("employeesGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN2_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("headManager", new UserImpl("Mark"));
                    roleAssignments.put("managers", new GroupImpl("managersGroup"));
                    roleAssignments.put("economists", new GroupImpl("economistsGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN3_CASE_ID, roleAssignments);
                    }

                    mainApp.setUsers(users);
                } catch (Exception e ) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                mainApp.setSolution();
                setSolutionLoading(false);
            }
        };
        new Thread(scenarioTask).start();
    }

    @FXML
    public void handleLoadLargeScenario() {
        setSolutionLoading(true);
        Task<Void> scenarioTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    services.resetCurrentScenario();
                    // delete tasks in task list, event listener deletes tasks only from working solution task list
                    MainApp.tasks.clear();
                    mainApp.clearSolution();

                    services.insertLargeScenarioUsers();
                    List<User> users = services.getAllUsers();

                    Map<String, OrganizationalEntity> roleAssignments = new HashMap<>();
                    roleAssignments.put("manager", new UserImpl("John"));
                    roleAssignments.put("supplier", new UserImpl("Marian"));
                    roleAssignments.put("suppliers", new GroupImpl("suppliersGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("managers", new GroupImpl("managersGroup"));
                    roleAssignments.put("officeEmployees", new GroupImpl("employeesGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN2_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("headManager", new UserImpl("Mark"));
                    roleAssignments.put("managers", new GroupImpl("managersGroup"));
                    roleAssignments.put("economists", new GroupImpl("economistsGroup"));
                    for (int i = 0; i < 10; i++) {
                        services.startCase(TASKS_TO_ASSIGN3_CASE_ID, roleAssignments);
                    }

                    roleAssignments.clear();
                    roleAssignments.put("marketers", new GroupImpl("marketersGroup"));
                    roleAssignments.put("organizers", new GroupImpl("organizersGroup"));
                    roleAssignments.put("designers", new GroupImpl("designersGroup"));
                    for (int i = 0; i < 20; i++) {
                        services.startCase(TASKS_TO_ASSIGN4_CASE_ID, roleAssignments);
                    }

                    mainApp.setUsers(users);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                mainApp.setSolution();
                setSolutionLoading(false);
            }
        };
        new Thread(scenarioTask).start();
    }

    @FXML
    public void handleCreateCustomTaskDialog() {
        mainApp.showAndHandleCustomTaskDialog();
    }

    public void setButtonsDisable(boolean disable) {
        smallScenarioButton.setDisable(disable);
        mediumScenarioButton.setDisable(disable);
        largeScenarioButton.setDisable(disable);
        createCustomTaskButton.setDisable(disable);
    }

    private void setSolutionLoading(boolean loading) {
        setButtonsDisable(loading);
        double progress = loading ? ProgressIndicator.INDETERMINATE_PROGRESS : 0;
        String progressBarText = loading ? "Loading..." : null;

        MainApp.getSolutionController().getProgressBar().setProgress(progress);
        MainApp.getSolutionController().getProgressBarText().setText(progressBarText);
        MainApp.getSolutionController().getSolveButton().setDisable(loading);
        MainApp.getSolutionController().getTerminateSolvingEarlyButton().setDisable(loading);

    }



}
