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
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedPropertiesBuildTriggerConfig extends BuildTriggerConfig {

	private final String projectsValue;
	private final String properties;
	private final ResultCondition condition;
	private final boolean includeCurrentParameters;

	@DataBoundConstructor
	public PredefinedPropertiesBuildTriggerConfig(String projectsValue, String properties,
			ResultCondition condition, boolean includeCurrentParameters) {
		this.projectsValue = projectsValue;
		this.properties = properties;
		this.condition = condition;
		this.includeCurrentParameters = includeCurrentParameters;
	}

	public void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		if (condition.isMet(build.getResult())) {
			Properties p = new Properties();
			p.load(new StringInputStream(properties));

			for (AbstractProject project : getProjects()) {
				List<ParameterValue> values = new ArrayList<ParameterValue>();
				if (includeCurrentParameters) {
					ParametersAction action = build.getAction(ParametersAction.class);
					if (action != null) {
						values.addAll(action.getParameters());
					}
				}
				for (Map.Entry<Object, Object> entry : p.entrySet()) {
					values.add(new StringParameterValue(entry.getKey()
							.toString(), resolveParametersInString(build, listener, entry.getValue().toString())));
				}

                project.scheduleBuild(0, new Cause.UpstreamCause(build), new ParametersAction(values));
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
