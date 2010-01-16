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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

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

	private static ParametersAction mergeParameters(ParametersAction base, ParametersAction overlay) {
		LinkedHashMap<String,ParameterValue> params = new LinkedHashMap<String,ParameterValue>();
		for (ParameterValue param : base.getParameters())
			params.put(param.getName(), param);
		for (ParameterValue param : overlay.getParameters())
			params.put(param.getName(), param);
		return new ParametersAction(params.values().toArray(new ParameterValue[params.size()]));
	}

	private static ParametersAction getDefaultParameters(AbstractProject<?,?> project) {
		ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
		if (property == null) {
			return null;
		}
		
		List<ParameterValue> parameters = new ArrayList<ParameterValue>();
		for (ParameterDefinition pd : property.getParameterDefinitions()) {
			ParameterValue param = pd.getDefaultParameterValue();
			if (param != null) parameters.add(param);
		}
		
		return new ParametersAction(parameters);
	}
	
	List<Action> getBaseActions(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException, InterruptedException, DontTriggerException {
		List<Action> actions = new ArrayList<Action>();
		ParametersAction params = null;
		for (AbstractBuildParameters config : configs) {
			Action a = config.getAction(build, listener);
			if (a instanceof ParametersAction) {
				params = params == null ? (ParametersAction)a
					: mergeParameters(params, (ParametersAction)a);
			} else if (a != null) {
				actions.add(a);
			}
		}
		if (params != null) actions.add(params);
		return actions;
	}

        List<Action> getBuildActions(List<Action> baseActions, AbstractProject project) {
		List<Action> actions = new ArrayList<Action>(baseActions);

		ParametersAction defaultParameters = getDefaultParameters(project);
		if (defaultParameters != null) {
			Action a = null;
			for (ListIterator<Action> it = actions.listIterator(); it.hasNext();)
				if ((a = it.next()) instanceof ParametersAction) {
					it.set(mergeParameters(defaultParameters, (ParametersAction)a));
					break;
                                }
			if (!(a instanceof ParametersAction))
				actions.add(defaultParameters);
		}
		return actions;
	}

	/**
	 * @deprecated since 2.3 with Hudson 1.341+
	 * (see {@link BuildTrigger#buildDependencyGraph(AbstractProject, hudson.model.DependencyGraph)})
	 */
	@Deprecated
	public void perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		try {
			if (condition.isMet(build.getResult())) {
				List<Action> actions = getBaseActions(build, listener);
				if (!actions.isEmpty()) {
					for (AbstractProject project : getProjectList()) {
						List<Action> list = getBuildActions(actions, project);
						
						project.scheduleBuild(project.getQuietPeriod(),
								new Cause.UpstreamCause((Run)build),
								list.toArray(new Action[list.size()]));
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
