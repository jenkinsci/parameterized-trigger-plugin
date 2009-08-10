package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Items;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class BuildTriggerConfig {

	private final List<AbstractBuildParameters> configs;

	private final String projects;
	private final ResultCondition condition;

	public BuildTriggerConfig(String projects, ResultCondition condition,
			List<AbstractBuildParameters> configs) {
		this.configs = configs;
		this.projects = projects;
		this.condition = condition;
	}

	public BuildTriggerConfig(String projects, ResultCondition condition,
			AbstractBuildParameters... configs) {
		this(projects, condition, Arrays.asList(configs));
	}

	public List<AbstractBuildParameters> getConfigs() {
		return configs;
	}

	public String getProjects() {
		return projects;
	}

	public ResultCondition getCondition() {
		return condition;
	}

	public List<AbstractProject> getProjectList() {
		return Items.fromNameList(projects, AbstractProject.class);
	}

	public void perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		try {
			if (condition.isMet(build.getResult())) {
				List<Action> actions = new ArrayList<Action>();
				for (AbstractBuildParameters config : configs) {
					Action a = config.getAction(build, launcher, listener);
					if (a != null) {
						actions.add(a);
					}
				}

				if (!actions.isEmpty()) {
					for (AbstractProject project : getProjectList()) {
						project.scheduleBuild(0,
								new Cause.UpstreamCause(build),
								(Action[]) actions.toArray(new Action[actions
										.size()]));
					}
				}
			}
		} catch (DontTriggerException e) {
			// don't trigger on this configuration
			return;
		}
	}

	@Override
	public String toString() {
		return "BuildTriggerConfig [projects=" + projects + ", condition="
				+ condition + ", configs=" + configs + "]";
	}

}
