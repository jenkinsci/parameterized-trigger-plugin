package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * {@link BuildTriggerConfig} that supports blocking of the execution.
 * @author Kohsuke Kawaguchi
 */
public class BlockableBuildTriggerConfig extends BuildTriggerConfig {
    private final BlockingBehaviour block;

    @DataBoundConstructor
    public BlockableBuildTriggerConfig(String projects, BlockingBehaviour block, List<AbstractBuildParameters> configs) {
        super(projects, ResultCondition.ALWAYS, configs);
        this.block = block;
    }

    public BlockableBuildTriggerConfig(String projects, BlockingBehaviour block, AbstractBuildParameters... configs) {
        super(projects, ResultCondition.ALWAYS, configs);
        this.block = block;
    }

    public BlockingBehaviour getBlock() {
        return block;
    }

    @Override
    public List<Future<AbstractBuild>> perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        List<Future<AbstractBuild>> r = super.perform(build, launcher, listener);
        if (block==null) return Collections.emptyList();
        return r;
    }

    @Extension
    public static class DescriptorImpl extends BuildTriggerConfig.DescriptorImpl {
    }
}
