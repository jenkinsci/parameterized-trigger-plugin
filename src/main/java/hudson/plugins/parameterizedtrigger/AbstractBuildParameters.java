package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;

import java.io.IOException;

public abstract class AbstractBuildParameters extends AbstractDescribableImpl<AbstractBuildParameters> {

    public abstract Action getAction(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException;

    public static class DontTriggerException extends Exception {}
}
