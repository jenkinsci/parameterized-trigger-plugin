package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

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
			String resolvedPropertiesFile = resolveParametersInString(build, listener, propertiesFile);
			FilePath f = build.getProject().getWorkspace()
					.child(resolvedPropertiesFile);
			if (!f.exists()) {
				listener.getLogger().println(
						"Could not trigger downstream project, as properties file"
								+ resolvedPropertiesFile + " did not exist.");
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
							.toString(), resolveParametersInString(build, listener, entry.getValue().toString())));
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

	@Extension
	public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {

		@Override
		public String getDisplayName() {
			return "Get properties from property file";
		}

		public ResultCondition[] getPossibleResultConditions() {
			return ResultCondition.values();
		}

	}

}
