package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@link BuildTriggerConfig} that supports blocking of the execution.
 * @author Kohsuke Kawaguchi
 */
public class BlockableBuildTriggerConfig extends BuildTriggerConfig {
    private final BlockingBehaviour block;
    public boolean buildAllNodesWithLabel;

    public BlockableBuildTriggerConfig(String projects, BlockingBehaviour block, List<AbstractBuildParameters> configs) {
        super(projects, ResultCondition.ALWAYS, false, configs);
        this.block = block;
    }

    @DataBoundConstructor
    public BlockableBuildTriggerConfig(String projects, BlockingBehaviour block, List<AbstractBuildParameterFactory> configFactories,List<AbstractBuildParameters> configs) {
        super(projects, ResultCondition.ALWAYS, false, configFactories, configs, false);
        this.block = block;
    }

    public BlockingBehaviour getBlock() {
        return block;
    }

    @Override
    public List<QueueTaskFuture<AbstractBuild>> perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        List<QueueTaskFuture<AbstractBuild>> r = super.perform(build, launcher, listener);
        if (block==null) return Collections.emptyList();
        return r;
    }

    @Override
    public ListMultimap<AbstractProject, QueueTaskFuture<AbstractBuild>> perform2(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ListMultimap<AbstractProject, QueueTaskFuture<AbstractBuild>> futures = super.perform2(build, launcher, listener);
        if(block==null) return ArrayListMultimap.create();
        return futures;
    }

    @Override
    public ListMultimap<Job, QueueTaskFuture<AbstractBuild>> perform3(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ListMultimap<Job, QueueTaskFuture<AbstractBuild>> futures = super.perform3(build, launcher, listener);
        if(block==null) return ArrayListMultimap.create();
        return futures;
    }

    @Override
    protected QueueTaskFuture schedule(AbstractBuild<?, ?> build, Job project, List<Action> list, TaskListener listener) throws InterruptedException, IOException {
        if (block!=null) {
            while (true) {
                // add DifferentiatingAction to make sure this doesn't get merged with something else,
                // which is most likely unintended. Might make sense to do it at BuildTriggerConfig for all.
                list = ImmutableList.<Action>builder().addAll(list).add(new DifferentiatingAction()).build();

                // if we fail to add the item to the queue, wait and retry.
                // it also means we have to force quiet period = 0, or else it'll never leave the queue
                QueueTaskFuture f = schedule(build, project, 0, list, listener);
                // When a project is disabled or the configuration is not yet saved f will always be null and we're caught in a loop, therefore we need to check for it
                if (f != null || !canBeScheduled(project)){
                    return f;
                }
                Thread.sleep(1000);
            }
        } else {
            return super.schedule(build,project,list,listener);
        }
    }

    public Collection<Node> getNodes() {
        return Jenkins.get().getLabel("asrt").getNodes();
    }

    @Extension
    public static class DescriptorImpl extends BuildTriggerConfig.DescriptorImpl {
    }
}
