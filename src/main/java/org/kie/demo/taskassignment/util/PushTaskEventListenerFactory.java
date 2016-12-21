package org.kie.demo.taskassignment.util;

import java.util.List;

import org.kie.demo.taskassignment.app.view.SolutionController;
import org.kie.demo.taskassignment.planner.domain.TaskPlanningEntity;

public class PushTaskEventListenerFactory {

    private static PushTaskEventListener registeredListener;

    public static PushTaskEventListener get(List<TaskPlanningEntity> tasks, SolutionController solutionController) {
        PushTaskEventListener listener = new PushTaskEventListener(tasks, solutionController);
        registeredListener = listener;
        return listener;
    }

    public static PushTaskEventListener getRegisteredListener() {
        return registeredListener;
    }

    public static void setRegisteredListener(PushTaskEventListener registeredListener) {
        PushTaskEventListenerFactory.registeredListener = registeredListener;
    }
}
