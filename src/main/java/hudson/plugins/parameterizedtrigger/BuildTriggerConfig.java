package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Future;

public class BuildTriggerConfig implements Describable<BuildTriggerConfig> {

	private final List<AbstractBuildParameters> configs;

	private String projects;
	private final ResultCondition condition;

    @DataBoundConstructor
	public BuildTriggerConfig(String projects, ResultCondition condition,
			List<AbstractBuildParameters> configs) {
		this.configs = Util.fixNull(configs);
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
     * Note that with Hudson 1.341, trigger should be using
	 * {@link BuildTrigger#buildDependencyGraph(AbstractProject, hudson.model.DependencyGraph)}.
	 */
	public List<Future<AbstractBuild>> perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		try {
			if (condition.isMet(build.getResult())) {
				List<Action> actions = getBaseActions(build, listener);

                List<Future<AbstractBuild>> futures = new ArrayList<Future<AbstractBuild>>();
                for (AbstractProject project : getProjectList()) {
                    List<Action> list = getBuildActions(actions, project);

                    futures.add(project.scheduleBuild2(project.getQuietPeriod(),
                            new UpstreamCause((Run) build),
                            list.toArray(new Action[list.size()])));
                }
                return futures;
			}
		} catch (DontTriggerException e) {
			// don't trigger on this configuration
		}
        return Collections.emptyList();
	}

        public boolean onJobRenamed(String oldName, String newName) {
            boolean changed = false;
            String[] list = projects.split(",");
            for (int i = 0; i < list.length; i++) {
                if (list[i].trim().equals(oldName)) {
                    list[i] = newName;
                    changed = true;
                }
            }
            if (changed) {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < list.length; i++) {
                    if (list[i] == null) continue;
                    if (buf.length() > 0) buf.append(',');
                    buf.append(list[i]);
                }
                projects = buf.toString();
            }
            return changed;
        }

        public boolean onDeleted(String oldName) {
            return onJobRenamed(oldName, null);
        }

    public Descriptor<BuildTriggerConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Override
	public String toString() {
		return getClass().getName()+" [projects=" + projects + ", condition="
				+ condition + ", configs=" + configs + "]";
	}

    @Extension
    public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

        public List<Descriptor<AbstractBuildParameters>> getBuilderConfigDescriptors() {
            return Hudson.getInstance().getDescriptorList(AbstractBuildParameters.class);
        }
    }
}
