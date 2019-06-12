package hudson.plugins.parameterizedtrigger;

import hudson.model.Job;
import hudson.model.ParametersAction;

public interface ITransformProjectParametersAction {
    /**
     * Called if there's an existing ParametersAction to transform.
    */
    public abstract ParametersAction transformParametersAction(ParametersAction a, Job<?,?> project);
}
