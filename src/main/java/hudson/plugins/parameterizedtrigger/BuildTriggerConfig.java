package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
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

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.Properties;

public abstract class BuildTriggerConfig implements Describable<BuildTriggerConfig> {

	protected String projectsValue;
	protected String batchCondition;

	public abstract void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException;

	protected static String resolveParametersInString(AbstractBuild<?, ?> build, BuildListener listener, String input) {
		try {
			return build.getEnvironment(listener).expand(input);
		}
		catch (Exception e) {
			listener.getLogger().println("Failed to resolve parameters in string \""+
					input+"\" due to following error:\n"+e.getMessage());
		}
		return input;
	}

	protected boolean checkBatchCondition(AbstractBuild<?, ?> build, Launcher launcher,	BuildListener listener) {
		if (batchCondition == null || batchCondition == "") {
			return true;
		}
		listener.getLogger().println("Checking batch condition for projects \""+projectsValue+"\".");
		try {
			CommandInterpreter batchRunner;
			if (launcher.isUnix()) {
				batchRunner = new Shell(batchCondition);
			}
			else {
				batchRunner = new BatchFile(batchCondition);
			}
			boolean result = batchRunner.perform(build, launcher, listener) ? true : false;
			if (!result) {
				listener.getLogger().println("Batch condition for projects \""+projectsValue+
						"\" isn't met. Projects are not triggered.");
			}
			else {
				listener.getLogger().println("Batch condition for projects \""+projectsValue+"\" is met.");
			}
			return result;
		} catch (InterruptedException e) {
			listener.getLogger().println("Failed to check batch condition \""+batchCondition+"\"");
			return false;
		}
	}
	
	public List<ParameterValue> createParametersList(AbstractBuild<?, ?> build, AbstractProject project,
			boolean includeCurrentParameters, Properties specifiedParameters, BuildListener listener) {
		Hashtable<String, ParameterValue> parameters = new Hashtable<String, ParameterValue>();
		// Add downstream project default parameters.
		ParametersDefinitionProperty parametersProperty =
				(ParametersDefinitionProperty)project.getProperty(ParametersDefinitionProperty.class);
		if (parametersProperty != null) {
			for (ParameterDefinition parameterDefinition : parametersProperty.getParameterDefinitions()) {
				ParameterValue parameter = parameterDefinition.getDefaultParameterValue();
				parameters.put(parameter.getName(), parameter);
			}
		}
		// Add current project parameters.
		if (includeCurrentParameters) {
			ParametersAction action = build.getAction(ParametersAction.class);
			if (action != null) {
				for (ParameterValue parameter : action.getParameters()) {
					parameters.put(parameter.getName(), parameter);
				}
			}
		}
		// Add parameters that have been specified explicitly.
		for (Map.Entry<Object, Object> entry : specifiedParameters.entrySet()) {
			ParameterValue parameter = new StringParameterValue(entry.getKey()
					.toString(), resolveParametersInString(build, listener, entry.getValue().toString()));
			parameters.put(parameter.getName(), parameter);
		}
		
		return new ArrayList(parameters.values());
	}

	public String getBatchCondition() {
		return batchCondition;
	}

	@Override
	public Descriptor<BuildTriggerConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
	}
	
}
