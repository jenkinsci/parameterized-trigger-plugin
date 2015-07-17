package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParametersAction;

import java.util.List;
import java.util.ArrayList;

public interface ITransformProjectParametersAction {
    /**
     * Called if there's an existing ParametersAction to transform.
    */
    public abstract ParametersAction transformParametersAction(ParametersAction a, Job<?,?> project);
}
