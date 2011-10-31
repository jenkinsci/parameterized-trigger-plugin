package hudson.plugins.parameterizedtrigger;

import hudson.model.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractBuildParameterFactoryDescriptor extends Descriptor<AbstractBuildParameterFactory> {
    public AbstractBuildParameterFactoryDescriptor(Class<? extends AbstractBuildParameterFactory> clazz) {
        super(clazz);
    }

    public AbstractBuildParameterFactoryDescriptor() {
    }
}
