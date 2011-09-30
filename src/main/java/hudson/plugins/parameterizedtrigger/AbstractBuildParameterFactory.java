package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.List;

/**
 * Generates Build Parameters. These will can be used in the TriggerBuilder to trigger the same projects with many different
 * parameters.
 */
public abstract class AbstractBuildParameterFactory implements Describable<AbstractBuildParameterFactory> {

    public abstract List<AbstractBuildParameters> getParameters(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException;

    @Override
    public Descriptor<AbstractBuildParameterFactory> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }
}
