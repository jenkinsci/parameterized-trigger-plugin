package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.model.Job;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class BuildTrigger extends Notifier implements DependecyDeclarer {

	private final ArrayList<BuildTriggerConfig> configs;

    @DataBoundConstructor
	public BuildTrigger(List<BuildTriggerConfig> configs) {
		this.configs = new ArrayList<BuildTriggerConfig>(Util.fixNull(configs));
	}

	public BuildTrigger(BuildTriggerConfig... configs) {
		this(Arrays.asList(configs));
	}

	public List<BuildTriggerConfig> getConfigs() {
		return configs;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override @SuppressWarnings("deprecation")
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		HashSet<BuildTriggerConfig> alreadyFired = new HashSet<BuildTriggerConfig>();

		// If this project has non-abstract projects, we need to fire them
		for (BuildTriggerConfig config : configs) {
			boolean hasNonAbstractProject = false;

			List<Job> jobs = config.getProjectList(build.getRootBuild().getProject().getParent(), build.getEnvironment(listener));
			for (Job j : jobs) {
				if (!(j instanceof AbstractProject)) {
					hasNonAbstractProject = true;
					break;
				}
			}
			// Fire this config's projects if not already fired
			if (hasNonAbstractProject) {
				config.perform(build, launcher, listener);
				alreadyFired.add(config);
			}
		}

        if (canDeclare(build.getProject())) {
            // job will get triggered by dependency graph, so we have to capture buildEnvironment NOW before
            // hudson.model.AbstractBuild.AbstractBuildExecution#cleanUp is called and reset
            EnvVars env = build.getEnvironment(listener);
            build.addAction(new CapturedEnvironmentAction(env));
        } else {  // Not using dependency graph
            for (BuildTriggerConfig config : configs) {
				if (!alreadyFired.contains(config)) {
					config.perform(build, launcher, listener);
				}
            }
        }

		return true;
	}

	@Override
	public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
		// Can only add dependencies in Hudson 1.341 or higher
		if (!canDeclare(owner)) return;

		for (BuildTriggerConfig config : configs) {
			List<AbstractProject> projectList = Util.filter(config.getProjectList(owner.getParent(), null), AbstractProject.class);
			for (AbstractProject project : projectList)
				ParameterizedDependency.add(owner, project, config, graph);
		}
	}

	private boolean canDeclare(AbstractProject owner) {
        // In Hudson 1.341+ builds will be triggered via DependencyGraph
        // Inner class added in Hudson 1.341
        String ownerClassName = owner.getClass().getName();
		return DependencyGraph.class.getClasses().length > 0
                        // See HUDSON-5679 -- dependency graph is also not used when triggered from a promotion
                        && !ownerClassName.equals("hudson.plugins.promoted_builds.PromotionProcess");
 	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public String getDisplayName() {
			return "Trigger parameterized build on other projects";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

	}
}
