package org.kie.demo.taskassignment.app.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import org.kie.demo.taskassignment.planner.domain.Group;
import org.kie.demo.taskassignment.planner.domain.Skill;
import org.kie.demo.taskassignment.planner.domain.User;

/**
 * Controller that manages the user dialog
 */
public class UserDialogController {

    @FXML
    private Label nameLabel;

    @FXML
    private Label groupLabel;

    @FXML
    private GridPane gridPane;





    public void setUserFields(User user) {
        nameLabel.setText(user.getName());
        groupLabel.setText(user.getGroups().stream().map(Group::getName).collect(Collectors.joining(", ")));
        groupLabel.setWrapText(true);

        double prefWidth = gridPane.getColumnConstraints().get(1).getPrefWidth();
        GridPane skillsGridPane = new GridPane();
        skillsGridPane.getColumnConstraints().add(new ColumnConstraints(prefWidth/2));

        List<Skill> skillEntries = new ArrayList<>(user.getSkills().values());
        for (int i = 0; i < skillEntries.size(); i++) {
            Skill skill = skillEntries.get(i);
            Insets insets = new Insets(3);

            //Label skillLabel = new Label(skill.getName() + " : " + skill.getSkillLevel());
            Label nameLabel = new Label(skill.getName() + ":");
            nameLabel.setPadding(insets);
            nameLabel.setPrefWidth(prefWidth/2);
            nameLabel.setAlignment(Pos.BASELINE_RIGHT);
            skillsGridPane.add(nameLabel, 0, i);
            GridPane.setHalignment(nameLabel, HPos.RIGHT);

            Label skillLevelLabel = new Label(skill.getSkillLevel().toString());
            skillLevelLabel.setPadding(insets);
            skillLevelLabel.setPrefWidth(prefWidth/2);
            skillsGridPane.add(skillLevelLabel, 1, i);
        }

        gridPane.add(skillsGridPane, 1, 2);
    }
}
