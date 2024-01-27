package hudson.plugins.parameterizedtrigger;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParametersAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectSpecificParametersActionFactory {

    private final List<ITransformProjectParametersAction> transforms;

    public ProjectSpecificParametersActionFactory(ITransformProjectParametersAction... transforms) {
        super();
        this.transforms = new ArrayList<ITransformProjectParametersAction>(Arrays.asList(transforms));
    }

    public final List<Action> getProjectSpecificBuildActions(List<Action> baseActions, Job<?, ?> project) {
        List<Action> actions = new ArrayList<>();
        ParametersAction pa = getParametersAction(baseActions);

        // Copy everything except the ParametersAction
        for (Action a : baseActions) {
            if (!(a instanceof ParametersAction)) {
                actions.add(a);
            }
        }

        for (ITransformProjectParametersAction transform : transforms) {
            pa = transform.transformParametersAction(pa, project);
        }

        actions.add(pa);

        return actions;
    }

    private static ParametersAction getParametersAction(List<Action> actions) {
        for (Action a : actions) {
            if (a instanceof ParametersAction) {
                return (ParametersAction) a;
            }
        }

        return new ParametersAction();
    }
}
