package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.ParameterValue;
import hudson.model.ParameterizedProjectTask;
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedPropertiesBuildTriggerConfig extends BuildTriggerConfig {

	private final String projectsValue;
	private final String properties;
	private final ResultCondition condition;

	@DataBoundConstructor
	public PredefinedPropertiesBuildTriggerConfig(String projectsValue, String properties,
			ResultCondition condition) {
		this.projectsValue = projectsValue;
		this.properties = properties;
		this.condition = condition;
	}

	public void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		if (condition.isMet(build.getResult())) {
			Properties p = new Properties();
			p.load(new StringReader(properties));

			for (AbstractProject project : getProjects()) {
				List<ParameterValue> values = new ArrayList<ParameterValue>();
				for (Map.Entry<Object, Object> entry : p.entrySet()) {
					values.add(new StringParameterValue(entry.getKey()
							.toString(), entry.getValue().toString()));
				}

				Hudson.getInstance().getQueue().add(
						new ParameterizedProjectTask(project, values), 0);
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

	public String getProjectsValue() {
		return projectsValue;
	}

	public static Descriptor<BuildTriggerConfig> DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {
		public DescriptorImpl() {
			super(PredefinedPropertiesBuildTriggerConfig.class);
		}

		@Override
		public String getDisplayName() {
			return "Use predefined properties";
		}

		public ResultCondition[] getPossibleResultConditions() {
			return ResultCondition.values();
		}

	}

	public Descriptor<BuildTriggerConfig> getDescriptor() {
		return DESCRIPTOR;
	}

}
