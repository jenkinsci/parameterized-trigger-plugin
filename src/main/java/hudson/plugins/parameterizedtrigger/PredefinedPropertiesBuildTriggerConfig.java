package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedPropertiesBuildTriggerConfig extends BuildTriggerConfig {

	private final String properties;
	private final ResultCondition condition;
	private final boolean includeCurrentParameters;

	@DataBoundConstructor
	public PredefinedPropertiesBuildTriggerConfig(String projectsValue, String properties,
			ResultCondition condition, String batchCondition, boolean includeCurrentParameters) {
		this.projectsValue = projectsValue;
		this.properties = properties;
		this.condition = condition;
		this.batchCondition = batchCondition;
		this.includeCurrentParameters = includeCurrentParameters;
	}

	public void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		if (condition.isMet(build.getResult()) && checkBatchCondition(build, launcher, listener)) {
			Properties p = new Properties();
			p.load(new StringInputStream(properties));

			for (AbstractProject project : getProjects()) {
				List<ParameterValue> parameters = createParametersList(build, project,
						includeCurrentParameters, p, listener);
                project.scheduleBuild(0, new Cause.UpstreamCause(build), new ParametersAction(parameters));
			}

		}

	}

	public List<AbstractProject> getProjects() {
		return Items.fromNameList(projectsValue, AbstractProject.class);
	}

	public String getProperties() {
		return properties;
	}

	public ResultCondition getCondition() {
		return condition;
	}

	public boolean isIncludeCurrentParameters() {
		return includeCurrentParameters;
	}

	public String getProjectsValue() {
		return projectsValue;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {
		@Override
		public String getDisplayName() {
			return "Use predefined properties";
		}

		public ResultCondition[] getPossibleResultConditions() {
			return ResultCondition.values();
		}

	}

}
