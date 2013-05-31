package hudson.plugins.parameterizedtrigger;

import hudson.model.Descriptor;

/**
 * {@link Descriptor} for {@link AbstractBuildParameterFactory}.
 *
 * <p>
 * At this point this really is just a place holder for future enhancements.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractBuildParameterFactoryDescriptor extends Descriptor<AbstractBuildParameterFactory> {
    public AbstractBuildParameterFactoryDescriptor(Class<? extends AbstractBuildParameterFactory> clazz) {
        super(clazz);
    }

    public AbstractBuildParameterFactoryDescriptor() {
    }
}
