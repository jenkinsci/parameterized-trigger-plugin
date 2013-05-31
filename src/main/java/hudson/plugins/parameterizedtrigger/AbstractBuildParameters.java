package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Queue.Task;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Extension point to produce an {@link Action} to invoke child projects with.
 *
 * Primarily that {@link Action} is {@link ParametersAction} to define additional build parameters to pass, but
 * it could be any other {@link Action}.
 *
 * @see Queue#schedule(Task, int, Action...)
 */
public abstract class AbstractBuildParameters extends AbstractDescribableImpl<AbstractBuildParameters> implements ExtensionPoint {

    /**
     *
     * @param build
     *      The current in-progress build that's about to trigger other projects.
     * @param listener
     *      Connected to the in-progress build of the {@code build} parameter.
     */
    public abstract Action getAction(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException;

    /**
     * Retrieve the build environment from the upstream build
     */
    public EnvVars getEnvironment(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException {

        CapturedEnvironmentAction capture = build.getAction(CapturedEnvironmentAction.class);
        if (capture != null) {
            return capture.getCapturedEnvironment();
        } else {
            return build.getEnvironment(listener);
        }
    }

    public static class DontTriggerException extends Exception {}
}
