package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedBuildParameters extends AbstractBuildParameters {

	private final String properties;
	private final String projectNameProperty;

	@DataBoundConstructor
	public PredefinedBuildParameters(String properties, String projectNameProperty) {
		this.properties = properties;
		this.projectNameProperty = projectNameProperty;
	}

	public PredefinedBuildParameters(String properties) {
		this(properties, null);
	}

	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException, InterruptedException {

		EnvVars env = getEnvironment(build, listener);

		Properties p = ParameterizedTriggerUtils.loadProperties(getProperties());

		List<ParameterValue> values = new ArrayList<ParameterValue>();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			//exclude projectNameProperty key=value from returned Action
			if (projectNameProperty != null &&
					projectNameProperty.equals(entry.getKey().toString())) {
				continue;
			}
			values.add(new StringParameterValue(entry.getKey().toString(),
					env.expand(entry.getValue().toString())));
		}

		return new ParametersAction(values);
	}

	public String getProperties() {
		return properties;
	}

	public String getProjectNameProperty() {
		return projectNameProperty;
	}

	public String getProjectName() throws IOException, InterruptedException {
		if (projectNameProperty != null) {
			Properties p = ParameterizedTriggerUtils.loadProperties(getProperties());
			return p.getProperty(projectNameProperty, null);
		} else {
			return null;
		}
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Predefined parameters";
		}
	}

}
