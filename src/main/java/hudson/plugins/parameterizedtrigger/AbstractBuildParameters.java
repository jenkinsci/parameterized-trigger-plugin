package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.TaskListener;

import java.io.IOException;

public abstract class AbstractBuildParameters extends AbstractDescribableImpl<AbstractBuildParameters> {

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
