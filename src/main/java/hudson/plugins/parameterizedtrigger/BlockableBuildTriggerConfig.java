package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Run;
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

    @Override
    protected Future schedule(AbstractBuild<?, ?> build, AbstractProject project, List<Action> list) throws InterruptedException, IOException {
        if (block!=null) {
            while (true) {
                // if we fail to add the item to the queue, wait and retry.
                // it also means we have to force quiet period = 0, or else it'll never leave the queue
                Future f = project.scheduleBuild2(0, new UpstreamCause((Run) build), list.toArray(new Action[list.size()]));
                if (f!=null)    return f;
                Thread.sleep(1000);
            }
        } else {
            return super.schedule(build,project,list);
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildTriggerConfig.DescriptorImpl {
    }
}
