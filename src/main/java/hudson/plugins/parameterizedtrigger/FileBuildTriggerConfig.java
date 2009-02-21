package hudson.plugins.parameterizedtrigger;

import hudson.FilePath;
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
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FileBuildTriggerConfig extends BuildTriggerConfig {

	private final String projectsValue;
	private final String propertiesFile;
	private final ResultCondition condition;
	private final boolean includeCurrentParameters;

	@DataBoundConstructor
	public FileBuildTriggerConfig(String projectsValue, String propertiesFile,
			ResultCondition condition, boolean includeCurrentParameters) {
		this.projectsValue = projectsValue;
		this.propertiesFile = propertiesFile;
		this.condition = condition;
		this.includeCurrentParameters = includeCurrentParameters;
	}

	public void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		if (condition.isMet(build.getResult())) {
			FilePath f = build.getProject().getWorkspace()
					.child(propertiesFile);
			if (!f.exists()) {
				listener.getLogger().println(
						"Could not trigger downstream project, as properties file"
								+ propertiesFile + " did not exist.");
				return;
			}

			Properties p = new Properties();
			InputStream is = f.read();
			try {
				p.load(is);
				is.close();
			} finally {
				is.close();
			}

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
							.toString(), entry.getValue().toString()));
				}

                project.scheduleBuild(0, new Cause.UpstreamCause(build), new ParametersAction(values));
			}

		}

	}

	public List<AbstractProject> getProjects() {
		return Items.fromNameList(projectsValue, AbstractProject.class);
	}

	public String getPropertiesFile() {
		return propertiesFile;
	}

	public ResultCondition getCondition() {
		return condition;
	}

	public String getProjectsValue() {
		return projectsValue;
	}

	public boolean isIncludeCurrentParameters() {
		return includeCurrentParameters;
	}

	public static Descriptor<BuildTriggerConfig> DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {
		public DescriptorImpl() {
			super(FileBuildTriggerConfig.class);
		}

		@Override
		public String getDisplayName() {
			return "Get properties from property file";
		}

		public ResultCondition[] getPossibleResultConditions() {
			return ResultCondition.values();
		}

	}

	public Descriptor<BuildTriggerConfig> getDescriptor() {
		return DESCRIPTOR;
	}

}
