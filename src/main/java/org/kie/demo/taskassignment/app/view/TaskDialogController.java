package org.kie.demo.taskassignment.app.view;

import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.kie.api.task.model.Status;
import org.kie.demo.taskassignment.app.services.Services;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;

/**
 * Controller that manages the task dialog
 */
public class TaskDialogController {

    @FXML
    private Label idLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label usersLabel;
    @FXML
    private Label groupsLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label priorityLabel;
    @FXML
    private Label skillLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label endTimeLabel;

    private TaskPlanningEntity taskPlanningEntity;

    private Stage dialogStage;

    private boolean claimClicked = false;
    private boolean completeClicked = false;

    public TaskPlanningEntity getTaskPlanningEntity() {
        return taskPlanningEntity;
    }

    public void setTaskPlanningEntity(TaskPlanningEntity taskPlanningEntity) {
        this.taskPlanningEntity = taskPlanningEntity;

        idLabel.setText(taskPlanningEntity.getId().toString());
        nameLabel.setText(taskPlanningEntity.getName());
        String users = taskPlanningEntity.getPotentialUserOwners().stream().collect(Collectors.joining(", "));
        usersLabel.setText(users.isEmpty() ? "NONE" : users);
        String groups = taskPlanningEntity.getPotentialGroupOwners().stream().map(Group::getName).collect(Collectors.joining(", "));
        groupsLabel.setText(groups.isEmpty() ? "NONE" : groups);
        durationLabel.setText(String.valueOf(taskPlanningEntity.getRealDuration()));
        statusLabel.setText(taskPlanningEntity.getStatus().toString());
        priorityLabel.setText(taskPlanningEntity.getPriority().toString());
        skillLabel.setText(taskPlanningEntity.getSkill());

        Integer startTime = taskPlanningEntity.getStartTime();
        // If the start time is set, we can calculate the end time as well
        if (startTime != null) {
            startTimeLabel.setText(convertTime(taskPlanningEntity.getStartTime()));
            endTimeLabel.setText(convertTime(taskPlanningEntity.getEndTime()));
        } else {
            startTimeLabel.setText("N/A");
            endTimeLabel.setText("N/A");
        }
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isClaimClicked() {
        return claimClicked;
    }

    public boolean isCompleteClicked() {
        return completeClicked;
    }

    @FXML
    public void handleClaimTask() {
        if (taskPlanningEntity.getStatus().equals(Status.Reserved)) {
            showTaskAlreadyClaimedAlert();
        } else if (taskPlanningEntity.getUser() == null) {
            showTaskNotAssignedYetAlert();
        } else {
            claimClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    public void handleCompleteTask() {
        if (taskPlanningEntity.getUser() == null) {
            showTaskNotAssignedYetAlert();
        } else {
            completeClicked = true;
            dialogStage.close();
        }
    }

    private void showTaskAlreadyClaimedAlert() {
        Alert alert = createAlert("Task already claimed", "This task has been already claimed.", "Please select another task to claim.");
        alert.showAndWait();
    }

    private void showTaskNotAssignedYetAlert() {
        Alert alert = createAlert("Task not assigned yet", "This task has not been assigned yet.", "Please start the planning first.");
        alert.showAndWait();
    }

    private Alert createAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert;
    }

    private String convertTime(Integer time) {
        // 10 hours a day
        int minutes = time % (10 * 60);
        int hours = 8 + (minutes / 60);
        minutes %= 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
