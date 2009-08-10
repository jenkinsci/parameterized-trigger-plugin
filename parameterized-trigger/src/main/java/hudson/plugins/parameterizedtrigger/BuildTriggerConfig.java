package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Items;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	private ParametersAction getDefaultParameters(AbstractProject<?,?> project) {
		ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
		if (property == null) {
			return null;
		}
		
		List<ParameterValue> parameters = new ArrayList<ParameterValue>();
		for (ParameterDefinition pd: property.getParameterDefinitions()) {
			parameters.add(pd.getDefaultParameterValue());
		}
		
		return new ParametersAction(parameters);
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
						
						List<Action> list = new ArrayList<Action>(actions);
						
						ParametersAction defaultParameters = getDefaultParameters(project);
						if (defaultParameters != null) {
							list.add(0, defaultParameters);
						}
						
						project.scheduleBuild(0,
								new Cause.UpstreamCause(build),
								(Action[]) list.toArray(new Action[list.size()]));
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
