package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

		// FIXME check for non-abstract project downstream and fire directly from perform method
		// If not canDeclare, fire for all of them

		/*for (BuildTriggerConfig config : configs) {
			List<Job> jobs = config.getProjectList(build.getRootBuild().getProject().getParent(), build.getEnvironment(listener));
			for (Job j : jobs) {
				if (!(j instanceof AbstractProject)) {
					config.perform(build, launcher, listener); 
					// FIXME fire directly not via perform!
				}
			}
		}*/

        if (canDeclare(build.getProject())) {
            // job will get triggered by dependency graph, so we have to capture buildEnvironment NOW before
            // hudson.model.AbstractBuild.AbstractBuildExecution#cleanUp is called and reset
            EnvVars env = build.getEnvironment(listener);
            build.addAction(new CapturedEnvironmentAction(env));
        } else {  // Not using dependency graph
            for (BuildTriggerConfig config : configs) {
				//FIXME don't fire if fired above?
				config.perform(build, launcher, listener);
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
