package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.DescriptorList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class BuildTrigger extends Notifier implements DependecyDeclarer {

	private final List<BuildTriggerConfig> configs;

	public BuildTrigger(List<BuildTriggerConfig> configs) {
		this.configs = configs;
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		for (BuildTriggerConfig config : configs) {
			config.perform(build, launcher, listener);
		}

		return true;
	}

	@Override
	public void buildDependencyGraph(AbstractProject owner,
			DependencyGraph graph) {
		
		for (BuildTriggerConfig config: configs) {
			for (AbstractProject project: config.getProjectList()) {
				graph.addDependency(owner, project);
			}
		}
		
	}

	public Object readResolve() {
		System.out.println(configs);
		return this;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject json)
				throws FormException {
			List<BuildTriggerConfig> result = new ArrayList<BuildTriggerConfig>();
			Object c = json.get("configs");
			if (c instanceof JSONObject) {
				JSONObject j = (JSONObject) c;
				result.add(bindBuildTrigger(req, j));
			}
			if (c instanceof JSONArray) {
				JSONArray a = (JSONArray) c;
				for (Object o : a) {
					if (o instanceof JSONObject) {
						JSONObject j = (JSONObject) o;
						result.add(bindBuildTrigger(req, j));
					}
				}
			}
			return new BuildTrigger(result);
		}

		private BuildTriggerConfig bindBuildTrigger(StaplerRequest req,
				JSONObject o) throws FormException {
			return new BuildTriggerConfig(o.getString("projects"),
					ResultCondition.valueOf(o.getString("condition")),
					newInstancesFromHeteroList(req, o, "configs", CONFIGS));
		}

		@Override
		public String getHelpFile() {
			return "/plugin/parameterized-trigger/help/plugin.html";
		}

		@Override
		public String getDisplayName() {
			return "Trigger parameterized build on other projects";
		}

		public DescriptorList<AbstractBuildParameters> getBuilderConfigDescriptors() {
			return CONFIGS;
		}

		public ResultCondition[] getPossibleResultConditions() {
			return ResultCondition.values();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

	}
	
	public static final DescriptorList<AbstractBuildParameters> CONFIGS = new DescriptorList<AbstractBuildParameters>(
			AbstractBuildParameters.class);

}
