package org.kie.demo.taskassignment.app.view;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller that manages the create custom task dialog
 */
public class CustomTaskDialogController {

    @FXML
    private TextField taskNameTextField;

    @FXML
    private ChoiceBox<String> skillNeededChoiceBox;

    @FXML
    private Spinner<Integer> baseDurationSpinner;

    @FXML
    private ChoiceBox<String> priorityChoiceBox;

    @FXML
    private Spinner<Integer> numberOfTasksSpinner;

    private List<CheckBox> userCheckBoxes = new ArrayList<>();

    private List<CheckBox> groupCheckBoxes = new ArrayList<>();

    private Stage dialogStage;

    private boolean createClicked = false;


    public TextField getTaskNameTextField() {
        return taskNameTextField;
    }

    public void setTaskNameTextField(TextField taskNameTextField) {
        this.taskNameTextField = taskNameTextField;
    }

    public ChoiceBox<String> getSkillNeededChoiceBox() {
        return skillNeededChoiceBox;
    }

    public void setSkillNeededChoiceBox(ChoiceBox skillNeededChoiceBox) {
        this.skillNeededChoiceBox = skillNeededChoiceBox;
    }

    public Spinner<Integer> getBaseDurationSpinner() {
        return baseDurationSpinner;
    }

    public void setBaseDurationSpinner(Spinner<Integer> baseDurationSpinner) {
        this.baseDurationSpinner = baseDurationSpinner;
    }

    public ChoiceBox<String> getPriorityChoiceBox() {
        return priorityChoiceBox;
    }

    public void setPriorityChoiceBox(ChoiceBox<String> priorityChoiceBox) {
        this.priorityChoiceBox = priorityChoiceBox;
    }

    public Spinner<Integer> getNumberOfTasksSpinner() {
        return numberOfTasksSpinner;
    }

    public void setNumberOfTasksSpinner(Spinner numberOfTasksSpinner) {
        this.numberOfTasksSpinner = numberOfTasksSpinner;
    }

    public List<CheckBox> getUserCheckBoxes() {
        return userCheckBoxes;
    }

    public void setUserCheckBoxes(List<CheckBox> userCheckBoxes) {
        this.userCheckBoxes = userCheckBoxes;
    }

    public List<CheckBox> getGroupCheckBoxes() {
        return groupCheckBoxes;
    }

    public void setGroupCheckBoxes(List<CheckBox> groupCheckBoxes) {
        this.groupCheckBoxes = groupCheckBoxes;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isCreateClicked() {
        return createClicked;
    }

    public void setCreateClicked(boolean createClicked) {
        this.createClicked = createClicked;
    }

    @FXML
    public void handleCreate() {
        if (isInputValid()) {
            createClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        StringBuilder sb = new StringBuilder();

        if (taskNameTextField.getText() == null || taskNameTextField.getText().length() == 0) {
            sb.append("Name of the task cannot be empty.\n");
        }

        // Collect all checked users to a list
        List<String> usersToAssign = userCheckBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());

        // Collect all checked groups to a list
        List<String> groupsToAssign = groupCheckBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());

        if (usersToAssign.size() == 0 && groupsToAssign.size() == 0) {
            sb.append("Assign the task to at least one user or group.");
        }

        if (sb.length() == 0) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(dialogStage);
            alert.setTitle("Incomplete input");
            alert.setHeaderText("Please, correct your input.");
            alert.setContentText(sb.toString());

            alert.showAndWait();
            return false;
        }
    }
}
