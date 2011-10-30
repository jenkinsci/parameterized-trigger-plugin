package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.List;

/**
 * Generates Build Parameters. These will can be used in the TriggerBuilder to trigger the same projects with many different
 * parameters.
 */
public abstract class AbstractBuildParameterFactory extends AbstractDescribableImpl<AbstractBuildParameterFactory> {

    public abstract List<AbstractBuildParameters> getParameters(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException;

    @Override
    public AbstractBuildParameterFactoryDescriptor getDescriptor() {
        return (AbstractBuildParameterFactoryDescriptor)super.getDescriptor();
    }
}
