package org.kie.demo.taskassignment.app.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import org.kie.api.task.model.Status;
import org.kie.demo.taskassignment.app.MainApp;
import org.kie.demo.taskassignment.planner.domain.Priority;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;
import org.kie.demo.taskassignment.planner.domain.User;

public class TaskAnchorPane extends AnchorPane {


    public static List<TaskPlanningEntity> tasks = new ArrayList<>();

    public static final int HEADER_ROW_HEIGHT = 50;
    public static final int HEADER_COLUMN_WIDTH = 150;
    public static final int ROW_HEIGHT = 50;
    public static final int TIME_COLUMN_WIDTH = 60;

    private MainApp mainApp;

    public TaskAnchorPane() {
    }

    public TaskAnchorPane(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void showStartScreen() {
        getChildren().clear();
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        AnchorPane.setBottomAnchor(hBox, 0.0);
        AnchorPane.setLeftAnchor(hBox, 0.0);
        AnchorPane.setRightAnchor(hBox, 0.0);
        AnchorPane.setTopAnchor(hBox, 0.0);
        hBox.getChildren().add(new ImageView("file:src/main/resources/images/StartScreenExplanation.png"));
        getChildren().add(hBox);
    }

    public void updatePane (TaskAssigningSolution taskAssigningSolution, boolean activeButtons) {
        getChildren().clear();
        List<User> users = taskAssigningSolution.getUserList();
        Map<User, Integer> userIndexMap = new HashMap<>(users.size());
        int userIndex = 0;
        for (User user : users) {
            Button userButton = new Button(user.getName());
            userButton.setOpacity(1);
            userButton.setLayoutX(0);
            userButton.setLayoutY(HEADER_ROW_HEIGHT + userIndex * ROW_HEIGHT);
            userButton.setPrefWidth(HEADER_COLUMN_WIDTH);
            userButton.setPrefHeight(ROW_HEIGHT);
            userButton.setStyle("-fx-border-color: black");
            userButton.setOnAction(event -> mainApp.showAndHandleUserDialog(user));
            userButton.setDisable(!activeButtons);
            getChildren().add(userButton);

            userIndexMap.put(user, userIndex);
            userIndex++;
        }

        int panelWidth = HEADER_COLUMN_WIDTH;
        int unassignedIndex = users.size();
        for (TaskPlanningEntity task : taskAssigningSolution.getTaskList()) {
            Button taskButton = createTaskButton(task);
            int x;
            int y;
            if (task.getUser() != null) {
                x = HEADER_COLUMN_WIDTH + task.getStartTime();
                y = HEADER_ROW_HEIGHT + userIndexMap.get(task.getUser()) * ROW_HEIGHT;
            } else {
                if (task.getStartTime() == null) {
                    x = HEADER_COLUMN_WIDTH;
                } else {
                    x = HEADER_COLUMN_WIDTH + task.getStartTime();
                }
                y = HEADER_ROW_HEIGHT + unassignedIndex * ROW_HEIGHT;
                unassignedIndex++;
            }
            if (x + task.getRealDuration() > panelWidth) {
                panelWidth = x + task.getRealDuration();//(int)taskButton.getWidth();
            }
            taskButton.setLayoutX(x);
            taskButton.setLayoutY(y);
            taskButton.setDisable(!activeButtons);
            getChildren().add(taskButton);
        }
        for (int x = HEADER_COLUMN_WIDTH; x < panelWidth; x += TIME_COLUMN_WIDTH) {
            // Use 10 hours per day
            int minutes = (x - HEADER_COLUMN_WIDTH) % (10 * 60);
            // Start at 8:00
            int hours = 8 + (minutes / 60);
            minutes %= 60;
            Label timeLabel = new Label((hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes);
            timeLabel.setStyle("-fx-border-color: black");
            timeLabel.setLayoutX(x);
            timeLabel.setLayoutY(0);
            timeLabel.setPrefWidth(TIME_COLUMN_WIDTH);
            timeLabel.setPrefHeight(ROW_HEIGHT);
            getChildren().add(timeLabel);
        }
    }

    private Button createTaskButton(TaskPlanningEntity task) {
        Button taskButton =  new Button();
        // TODO taskButton.setOnAction();
        taskButton.setOnAction(event -> mainApp.showAndHandleTaskDialog(task));
        taskButton.setText(task.getName());
        taskButton.setOpacity(1);
        taskButton.setPadding(Insets.EMPTY);
        taskButton.setStyle(getButtonColour(task));
        // If the task is claimed, show locker icon
        if (task.getStatus().equals(Status.Reserved)) {
            taskButton.setGraphic(new ImageView("file:src/main/resources/images/LockerIcon_16x16.png"));
            taskButton.setContentDisplay(ContentDisplay.TOP);
        }
        //taskButton.setBackground(new Background());
        //taskButton.setStyle("-fx-base: aliceblue");
        //TextAlignment alignment = new TextAlignment()
        //taskButton.setTextAlignment();
        //taskButton.setVerticalTextPosition(SwingConstants.TOP);
        taskButton.setPrefWidth(task.getRealDuration());
        taskButton.setMaxWidth(task.getRealDuration());
        taskButton.setPrefHeight(ROW_HEIGHT);
        return taskButton;
    }

    private String getButtonColour(TaskPlanningEntity task) {
        String colour = null;

        switch (task.getPriority()) {
            case LOW:
                colour = "-fx-base: rgba(56, 218, 0, 0.3)";
                break;
            case MEDIUM:
                colour = "-fx-base: rgba(255, 254, 7, 0.3)";
                break;
            case HIGH:
                colour = "-fx-base: rgba(255, 28, 0, 0.3)";
                break;
        }
        return colour;
    }
}
