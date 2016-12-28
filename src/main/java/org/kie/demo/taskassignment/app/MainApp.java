package org.kie.demo.taskassignment.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kie.api.task.model.Status;
import org.kie.demo.taskassignment.app.services.Services;
import org.kie.demo.taskassignment.app.view.CaseController;
import org.kie.demo.taskassignment.app.view.CustomTaskDialogController;
import org.kie.demo.taskassignment.app.view.SolutionController;
import org.kie.demo.taskassignment.app.view.TaskAnchorPane;
import org.kie.demo.taskassignment.app.view.TaskDialogController;
import org.kie.demo.taskassignment.app.view.UserDialogController;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.Skill;
import org.kie.demo.taskassignment.planner.domain.SkillLevel;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

public class MainApp extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;
    private SplitPane myRootLayout;
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
     * The data as an observable list of Persons.
     */
//    private ObservableList<Person> personData = FXCollections.observableArrayList();

    /**
     * Constructor
     */
    public MainApp() {
//        testSolution = new TaskAssigningSolution();
//
//
//        Group testGroup = new Group("testGroup");
//        List<User> users = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            User user = new User("User " + i);
//            Map<String, Skill> skills = new HashMap<>();
//            skills.put("TestSkill", new Skill("TestSkill", SkillLevel.EXPERT));
//            user.setSkills(skills);
//            user.getGroups().add(testGroup);
//            users.add(user);
//        }
//
//        testSolution.setUserList(users);
//
//        List<TaskPlanningEntity> tasks = new ArrayList<>();
//        int taskNumber = 1;
//        for(User user : users) {
//            int startTime = 0;
//            for (int i = 0; i < 8; i++) {
//                TaskPlanningEntity task = new TaskPlanningEntity();
//                //task.setUser(user);
//                task.setName("Task " + taskNumber);
//                task.getPotentialGroupOwners().add(testGroup);
//                taskNumber++;
//                task.setSkill("TestSkill");
//                int baseDuration = 60 + new Random().nextInt(61);
//                task.setBaseDuration(baseDuration);
//                //task.setStartTime(startTime);
//                //startTime = startTime + task.getRealDuration();
//                task.setPriority(Priority.MEDIUM);
//                task.setStatus(Status.Created);
//                tasks.add(task);
//            }
//        }
//        for (int i = 0; i < 10; i++) {
//            TaskPlanningEntity task = new TaskPlanningEntity();
//            task.setName("Task " + taskNumber);
//            taskNumber++;
//            task.setSkill("TestSkill");
//            task.getPotentialGroupOwners().add(testGroup);
//            int baseDuration = 60 + new Random().nextInt(61);
//            task.setBaseDuration(baseDuration);
//            //task.setStartTime(0);
//            task.setPriority(Priority.HIGH);
//            task.setStatus(Status.Reserved);
//            tasks.add(task);
//        }
//
//
//        testSolution.setTaskList(tasks);


        services = new Services();


        // Add some sample data
//        personData.add(new Person("Hans", "Muster"));
//        personData.add(new Person("Ruth", "Mueller"));
//        personData.add(new Person("Heinz", "Kurz"));
//        personData.add(new Person("Cornelia", "Meier"));
//        personData.add(new Person("Werner", "Meyer"));
//        personData.add(new Person("Lydia", "Kunz"));
//        personData.add(new Person("Anna", "Best"));
//        personData.add(new Person("Stefan", "Meier"));
//        personData.add(new Person("Martin", "Mueller"));
    }

    /**
     * Returns the data as an observable list of Persons.
     * @return
     */
//    public ObservableList<Person> getPersonData() {
//        return personData;
//    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("AddressApp");
        registerOnCloseListener();
        //this.primaryStage.getIcons().add(new Image("file:src/main/resources/images/address_book_32.png"));

        initRootLayout();


        showPersonOverview();

        showCaseOverview();

    }

    /**
     * Initializes the root layout and tries to load the last opened
     * person file.
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class
                    .getResource("view/MyRootLayout.fxml"));
            myRootLayout = (SplitPane) loader.load();
            solutionController = loader.getController();
            solutionController.setMainApp(this);
            //solutionController.setSolution(testSolution);

            // Show the scene containing the root layout.
            Scene scene = new Scene(myRootLayout);
            primaryStage.setScene(scene);

            // Give the controller access to the main app.
//            RootLayoutController controller = loader.getController();
//            controller.setMainApp(this);

            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the person overview inside the root layout.
     */
    public void showPersonOverview() {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/PersonOverview.fxml"));
            AnchorPane personOverview = (AnchorPane) loader.load();
//            Rectangle rect = new Rectangle(100, 100, personOverview.getPrefWidth()/2, 300);
//            rootLayout.widthProperty().addListener(observable -> {
//                rootLayout.getChildren().remove(rect);
//                Rectangle newrect = new Rectangle(100, 100, personOverview.widthProperty().get(), 300);
//                newrect.setFill(Color.YELLOW);
//                rootLayout.getChildren().add(newrect);
//            });
//
//            rect.setFill(Color.YELLOW);
//            personOverview.getChildren().add(rect);

            // Set person overview into the center of root layout.
            ScrollPane sp = new ScrollPane();
            TaskAnchorPane taskAnchorPane = new TaskAnchorPane(this);
            taskAnchorPane.prefHeightProperty().bind(sp.prefHeightProperty());
            taskAnchorPane.prefWidthProperty().bind(sp.prefWidthProperty());
            this.taskAnchorPane = taskAnchorPane;
            //updatePane();
            taskAnchorPane.showStartScreen();



//            Button butt = new Button("Press me!");
//
//            butt.setLayoutX(100);
//            butt.setLayoutY(10);
//            taskAnchorPane.getChildren().add(butt);
//
//
//            butt.setOnAction(new EventHandler<ActionEvent>() {
//                private double nextX = 100;
//                @Override
//                public void handle(ActionEvent event) {
//                    nextX = nextX + 50;
//                    Button button = new Button("New Button!");
//                    button.setLayoutY(10);
//                    button.setLayoutX(nextX);
//                    taskAnchorPane.getChildren().add(button);
//                    //taskAnchorPane.getChildren().add(new Button("Press me! 2nd edition"));
//                }
//            });
            sp.setContent(taskAnchorPane);
            sp.setFitToHeight(true);
            sp.setFitToWidth(true);
            ((BorderPane)myRootLayout.getItems().get(0)).setCenter(sp);

            // Give the controller access to the main app.
//            PersonOverviewController controller = loader.getController();
//            controller.setMainApp(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCaseOverview() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(MainApp.class.getResource("view/CaseOverview.fxml"));
        try {
            caseOverview = (BorderPane) loader.load();
            caseController = loader.getController();
            caseController.setMainApp(this);
            caseController.setServices(services);
            myRootLayout.getItems().add(0, caseOverview);
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
            //dialogStage.getIcons().add(new Image("file:src/main/resources/images/address_book_32.png"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Set the person into the controller.
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
            //dialogStage.getIcons().add(new Image("file:src/main/resources/images/address_book_32.png"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Set the person into the controller.
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
            //dialogStage.getIcons().add(new Image("file:src/main/resources/images/address_book_32.png"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            // Set the person into the controller.
            UserDialogController controller = loader.getController();
            //controller.setDialogStage(dialogStage);
            controller.setUserFields(user);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void updatePane() {
        TaskAssigningSolution solution = solutionController.getSolution();
        Score score = solutionController.getScore();
        taskAnchorPane.updatePane(solution, true);
        solutionController.refreshScoreField();
        // refreshScoreField(score);
    }

    // TODO maybe remove this method, exactly the same as updatePane()
    public void bestSolutionChanged() {
        TaskAssigningSolution solution = solutionController.getSolution();
        Score score = solutionController.getScore();
        taskAnchorPane.updatePane(solution, false);
        solutionController.refreshScoreField();
        // refreshScoreField(score);
    }

    public void setCaseControllerButtonsDisable(boolean disable) {
        // Only disable buttons if they are already loaded
        if (caseController != null) {
            caseController.setButtonsDisable(disable);
        }
    }

    // We have to be sure the solver is not running after the mainApp is closed
    private void registerOnCloseListener() {
        primaryStage.setOnCloseRequest(event -> solutionController.handleTerminateSolvingEarlyAction());
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
//        File outputFile = new File("SavedSolution.xml");
//        XStreamSolutionFileIO<TaskAssigningSolution> xStreamSolutionFileIO = new XStreamSolutionFileIO<>(TaskAssigningSolution.class);
//        xStreamSolutionFileIO.write(solution, outputFile);
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