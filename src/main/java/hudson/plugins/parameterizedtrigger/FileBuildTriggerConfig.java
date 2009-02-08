package hudson.plugins.parameterizedtrigger;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.ParameterValue;
import hudson.model.ParameterizedProjectTask;
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

	@DataBoundConstructor
	public FileBuildTriggerConfig(String projectsValue, String propertiesFile,
			ResultCondition condition) {
		this.projectsValue = projectsValue;
		this.propertiesFile = propertiesFile;
		this.condition = condition;
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

	public String getPropertiesFile() {
		return propertiesFile;
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
