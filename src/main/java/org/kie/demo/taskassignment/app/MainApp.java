package org.kie.demo.taskassignment.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kie.demo.taskassignment.app.services.Services;
import org.kie.demo.taskassignment.app.view.CaseController;
import org.kie.demo.taskassignment.app.view.CustomTaskDialogController;
import org.kie.demo.taskassignment.app.view.SolutionController;
import org.kie.demo.taskassignment.app.view.TaskAnchorPane;
import org.kie.demo.taskassignment.app.view.TaskDialogController;
import org.kie.demo.taskassignment.app.view.UserDialogController;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;

public class MainApp extends Application {

    private Stage primaryStage;
    private SplitPane rootLayout;
    private BorderPane caseOverview;


    private TaskAssigningSolution testSolution;

    private Services services;

    // this has to be static because of PushTaskEventListener
    public static SolutionController solutionController;
    private CaseController caseController;
    private TaskAnchorPane taskAnchorPane;

    public static List<TaskPlanningEntity> tasks = new ArrayList<>();

    private List<User> users;

    /**
     * Constructor of the app
     */
    public MainApp() {
        services = new Services();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("jBPM Task Assignment using OptaPlanner");
        registerOnCloseListener();

        initRootLayout();

        showTaskOverview();

        showCaseOverview();

    }

    /**
     * Initializes the root layout
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class
                    .getResource("view/RootLayout.fxml"));
            rootLayout = (SplitPane) loader.load();
            solutionController = loader.getController();
            solutionController.setMainApp(this);

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);

            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showTaskOverview() {

        ScrollPane sp = new ScrollPane();
        TaskAnchorPane taskAnchorPane = new TaskAnchorPane(this);
        taskAnchorPane.prefHeightProperty().bind(sp.prefHeightProperty());
        taskAnchorPane.prefWidthProperty().bind(sp.prefWidthProperty());
        this.taskAnchorPane = taskAnchorPane;
        taskAnchorPane.showStartScreen();

        sp.setContent(taskAnchorPane);
        sp.setFitToHeight(true);
        sp.setFitToWidth(true);
        ((BorderPane) rootLayout.getItems().get(0)).setCenter(sp);

    }

    public void showCaseOverview() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(MainApp.class.getResource("view/CaseOverview.fxml"));
        try {
            caseOverview = (BorderPane) loader.load();
            caseController = loader.getController();
            caseController.setMainApp(this);
            caseController.setServices(services);
            rootLayout.getItems().add(0, caseOverview);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAndHandleTaskDialog(TaskPlanningEntity taskPlanningEntity) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/TaskDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Task Info");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            TaskDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setTaskPlanningEntity(taskPlanningEntity);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            if (controller.isClaimClicked()) {
                services.getUserTaskService().claim(taskPlanningEntity.getId(), taskPlanningEntity.getUser().getName());
            } else if (controller.isCompleteClicked()) {
                services.getUserTaskService().completeAutoProgress(taskPlanningEntity.getId(), taskPlanningEntity.getUser().getName(), new HashMap<>());
            }

            updatePane();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAndHandleCustomTaskDialog() {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/CustomTaskDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create a custom task");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            CustomTaskDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.getSkillNeededChoiceBox().getItems().addAll(services.getAllSkillNames());
            controller.getSkillNeededChoiceBox().getSelectionModel().select(0);

            List<String> userNames = services.getAllUserNames();
            GridPane userGridPane = createCheckBoxes(userNames, 5, controller.getUserCheckBoxes());
            ((GridPane)((BorderPane)page.getChildren().get(0)).getCenter()).add(userGridPane, 1, 4);

            List<String> groupNames = services.getAllGroupNames();
            GridPane groupGridPane = createCheckBoxes(groupNames, 3, controller.getGroupCheckBoxes());
            ((GridPane)((BorderPane)page.getChildren().get(0)).getCenter()).add(groupGridPane, 1, 6);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            if (controller.isCreateClicked()) {
                // create a new task

                // Collect all checked users to a list
                List<String> usersToAssign = controller.getUserCheckBoxes().stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());;

                // Collect all checked groups to a list
                List<String> groupsToAssign = controller.getGroupCheckBoxes().stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());;

                String taskName = controller.getTaskNameTextField().getText();
                String skillNeeded = controller.getSkillNeededChoiceBox().getSelectionModel().getSelectedItem();
                Integer baseDuration = controller.getBaseDurationSpinner().getValue();
                String priority = controller.getPriorityChoiceBox().getSelectionModel().getSelectedItem();
                Integer numberOfTasks = controller.getNumberOfTasksSpinner().getValue();

                for (int i = 0; i < numberOfTasks; i++) {
                    services.startCustomTask(taskName, skillNeeded, baseDuration, priority, usersToAssign, groupsToAssign);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAndHandleUserDialog(User user) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/UserDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("User Info");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            UserDialogController controller = loader.getController();
            controller.setUserFields(user);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void updatePane() {
        TaskAssigningSolution solution = solutionController.getSolution();
        taskAnchorPane.updatePane(solution, true);
        solutionController.refreshScoreField();
    }

    public void bestSolutionChanged() {
        TaskAssigningSolution solution = solutionController.getSolution();
        taskAnchorPane.updatePane(solution, false);
        solutionController.refreshScoreField();
    }

    public void setCaseControllerButtonsDisable(boolean disable) {
        // Only disable buttons if they are already loaded
        if (caseController != null) {
            caseController.setButtonsDisable(disable);
        }
    }

    // We have to be sure the solver is not running after the mainApp is closed
    private void registerOnCloseListener() {
        primaryStage.setOnCloseRequest(event -> {solutionController.handleTerminateSolvingEarlyAction();
                                                 services.close();});
    }

    private GridPane createCheckBoxes(List<String> labels, int offset, List<CheckBox> checkBoxList) {
        Insets checkBoxInsets = new Insets(3, 3, 3, 3);
        GridPane checkBoxGridPane = new GridPane();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            CheckBox checkBox = new CheckBox(label);
            checkBox.setPadding(checkBoxInsets);
            checkBoxGridPane.add(checkBox, i/offset, i%offset);
            checkBoxList.add(checkBox);
        }
        return checkBoxGridPane;
    }

    public void setSolution() {
        TaskAssigningSolution solution = new TaskAssigningSolution();
        solution.setTaskList(tasks);
        solution.setUserList(users);
        solutionController.setSolution(solution);
        updatePane();
    }

    public void clearSolution() {
        solutionController.setSolutionLoaded(false);
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static SolutionController getSolutionController() {
        return solutionController;
    }

    public static void setSolutionController(SolutionController solutionController) {
        MainApp.solutionController = solutionController;
    }

    public static void main(String[] args) {
        launch(args);
    }
}