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
    /**
     * Let N be the length of the list returned by this method, and each item in this list X1, X2, X3, ... XN.
     *
     * This causes the parameterized trigger to trigger the configured project N times, each with Xi for i=1...N.
     * If there is another {@link AbstractBuildParameterFactory} configured and that returns Y1, Y2, ... YM,
     * then the project will be invoked MxN times, with all the possible combinations of Xi and Yj.
     *
     * @param build
     *      The build which the parameterized trigger is configured and executing.
     * @param listener
     *      Connected to the build output.
     *      
     * @return can be empty but never null.
     */
    public abstract List<AbstractBuildParameters> getParameters(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException;

    @Override
    public AbstractBuildParameterFactoryDescriptor getDescriptor() {
        return (AbstractBuildParameterFactoryDescriptor)super.getDescriptor();
    }
}
