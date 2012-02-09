package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildTrigger extends Notifier implements DependecyDeclarer, MatrixAggregatable {

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
		// In Hudson 1.341+ builds will be triggered via DependencyGraph
		if (canDeclare(build.getProject())) return true;

		for (BuildTriggerConfig config : configs) {
			config.perform(build, launcher, listener);
		}

		return true;
	}

	@Override
	public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
		// Can only add dependencies in Hudson 1.341 or higher
		if (!canDeclare(owner)) return;

		for (BuildTriggerConfig config : configs)
			for (AbstractProject project : config.getProjectList(owner.getParent(),null))
				ParameterizedDependency.add(owner, project, config, graph);
	}

	private boolean canDeclare(AbstractProject owner) {
		// Inner class added in Hudson 1.341
        String ownerClassName = owner.getClass().getName();
		return DependencyGraph.class.getClasses().length > 0
                        // See HUDSON-5679 -- dependency graph is also not used when triggered from a promotion
                        && !ownerClassName.equals("hudson.plugins.promoted_builds.PromotionProcess");
 	}

	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new MatrixAggregator(build, launcher, listener) {
			@Override
			public boolean endBuild() throws InterruptedException, IOException {
				return hudson.tasks.BuildTrigger.execute(build, listener);
			}
		};
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
