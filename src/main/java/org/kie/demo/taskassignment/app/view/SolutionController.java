package org.kie.demo.taskassignment.app.view;

import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.kie.demo.taskassignment.app.MainApp;
import org.kie.demo.taskassignment.planner.domain.TaskAssigningSolution;
import org.optaplanner.core.api.score.FeasibilityScore;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;
import org.optaplanner.core.impl.solver.ProblemFactChange;

public class SolutionController {


    private MainApp mainApp;

    private volatile Solver<TaskAssigningSolution> solver;
    private String solutionFileName = null;
    private ScoreDirector<TaskAssigningSolution> guiScoreDirector;
    private boolean solutionLoaded = false;

    @FXML
    private Button solveButton;

    @FXML
    private Button terminateSolvingEarlyButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Text progressBarText;

    @FXML
    private TextField scoreTextField;


    public SolutionController() {
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.createFromXmlResource("org/kie/demo/taskassignment/planner/TaskAssigningSolverConfig.xml");
        solver = solverFactory.buildSolver();
        ScoreDirectorFactory<TaskAssigningSolution> scoreDirectorFactory = solver.getScoreDirectorFactory();
        guiScoreDirector = scoreDirectorFactory.buildScoreDirector();
        registerForBestSolutionChanges();
    }

    public MainApp getMainApp() {
        return mainApp;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public TaskAssigningSolution getSolution() {
        return guiScoreDirector.getWorkingSolution();
    }

    public void setSolution(TaskAssigningSolution solution) {
        guiScoreDirector.setWorkingSolution(solution);
        solutionLoaded = true;
    }

    public boolean isSolutionLoaded() {
        return solutionLoaded;
    }

    public void setSolutionLoaded(boolean solutionLoaded) {
        this.solutionLoaded = solutionLoaded;
    }

    public Score getScore() {
        return guiScoreDirector.calculateScore();
    }

    // probably not used
    public boolean isSolving() {
        return solver.isSolving();
    }

    public void registerForBestSolutionChanges() {
        solver.addEventListener(event -> {
            // Called on the Solver thread, so not on the Swing Event thread
            /*
             * Avoid ConcurrentModificationException when there is an unprocessed ProblemFactChange
             * because the paint method uses the same problem facts instances as the Solver's workingSolution
             * unlike the planning entities of the bestSolution which are cloned from the Solver's workingSolution
             */
            if (solver.isEveryProblemFactChangeProcessed()) {
                // The final is also needed for thread visibility
                final TaskAssigningSolution latestBestSolution = event.getNewBestSolution();
                Platform.runLater(() -> {
                    // Called on the Swing Event thread
                    // TODO by the time we process this event, a newer bestSolution might already be queued
                    guiScoreDirector.setWorkingSolution(latestBestSolution);
                    mainApp.bestSolutionChanged();
                });
            }
        });
    }

    public void doProblemFactChange(ProblemFactChange<TaskAssigningSolution> problemFactChange) {
//        if (solver.isSolving()) {
//            solver.addProblemFactChange(problemFactChange);
//        } else {
        problemFactChange.doChange(guiScoreDirector);
        guiScoreDirector.calculateScore();
        mainApp.updatePane();
    }

    private void setSolvingState(boolean solving) {

        // Probably disable opening new cases and opening predefined cases
//        importAction.setEnabled(!solving && solutionBusiness.hasImporter());
//        openAction.setEnabled(!solving);
//        saveAction.setEnabled(!solving);
//        exportAction.setEnabled(!solving && solutionBusiness.hasExporter());
//        solveAction.setEnabled(!solving);
        solveButton.setVisible(!solving);
        // terminateSolvingEarlyAction.setEnabled(solving);
        terminateSolvingEarlyButton.setVisible(solving);
        HBox hBox;
        if (solving) {
//            hBox = (HBox)solveButton.getParent();
//            hBox.getChildren().clear();
//            hBox.getChildren().add(terminateSolvingEarlyButton);
            terminateSolvingEarlyButton.requestFocus();
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressBarText.setText("Solving...");
        } else {
//            hBox = (HBox) terminateSolvingEarlyButton.getParent();
//            hBox.getChildren().clear();
//            hBox.getChildren().add(solveButton);
            solveButton.requestFocus();
            progressBar.setProgress(0);
            progressBarText.setText(null);
        }

//        solutionPanel.setEnabled(!solving);
//        progressBar.setIndeterminate(solving);
//        progressBar.setStringPainted(solving);
//        progressBar.setString(solving ? "Solving..." : null);
//        showConstraintMatchesDialogAction.setEnabled(!solving);
    }

    @FXML
    private void handleSolveAction() {
        if (guiScoreDirector.getWorkingSolution().getTaskList().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle("Nothing to plan");
            alert.setHeaderText("There are no tasks to plan.");
            alert.setContentText("Load one of the scenarios.");
            alert.showAndWait();
        } else {
            setSolvingState(true);
            mainApp.setCaseControllerButtonsDisable(true);
            SolveTask task = new SolveTask(guiScoreDirector.getWorkingSolution());
            new Thread(task).start();
        }
    }

    private class SolveTask extends Task<TaskAssigningSolution> {

        private final TaskAssigningSolution planningProblem;

        public SolveTask(TaskAssigningSolution planningProblem) {
            this.planningProblem = planningProblem;
        }

        @Override
        protected TaskAssigningSolution call() throws Exception {
            try {
                return solver.solve(planningProblem);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            try {
                TaskAssigningSolution bestSolution = get();
                guiScoreDirector.setWorkingSolution(bestSolution);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Solving was interrupted.", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Solving failed.", e.getCause());
            } finally {
                setSolvingState(false);
                mainApp.setCaseControllerButtonsDisable(false);
                mainApp.updatePane();
            }
        }
    }

    // Public so the onCloseRequest can use this as well
    @FXML
    public void handleTerminateSolvingEarlyAction() {
        progressBarText.setText("Terminating...");
        solver.terminateEarly();
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize(){
        setSolvingState(false);
        scoreTextField.setText("Score:");
        solveButton.setGraphic(new ImageView("file:src/main/resources/images/PlayIcon_16x16.png"));
        terminateSolvingEarlyButton.setGraphic(new ImageView("file:src/main/resources/images/StopIcon_16x16.png"));
        // By default, forbid the solving when nothing is loaded after the start of the app
        solveButton.setDisable(true);
    }

    public void refreshScoreField() {
        Score score = getScore();
        scoreTextField.setStyle(getScoreColor(score));
        scoreTextField.setText("Latest best score: " + score);
    }

    public Button getSolveButton() {
        return solveButton;
    }

    public void setSolveButton(Button solveButton) {
        this.solveButton = solveButton;
    }

    public Button getTerminateSolvingEarlyButton() {
        return terminateSolvingEarlyButton;
    }

    public void setTerminateSolvingEarlyButton(Button terminateSolvingEarlyButton) {
        this.terminateSolvingEarlyButton = terminateSolvingEarlyButton;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public Text getProgressBarText() {
        return progressBarText;
    }

    public void setProgressBarText(Text progressBarText) {
        this.progressBarText = progressBarText;
    }

    private String getScoreColor(Score score) {
        FeasibilityScore<?> feasibilityScore = (FeasibilityScore<?>) score;
        if(!feasibilityScore.isFeasible()) {
            return "-fx-text-inner-color: red";
        } else {
            return "-fx-text-inner-color: black";
        }
    }

}
