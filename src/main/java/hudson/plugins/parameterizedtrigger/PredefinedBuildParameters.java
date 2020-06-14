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
import hudson.model.TextParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedBuildParameters extends AbstractBuildParameters {

	private final String properties;
	private final boolean textParamValueOnNewLine;


	@DataBoundConstructor
	public PredefinedBuildParameters(String properties, boolean textParamValueOnNewLine) {
		this.properties = properties;
		this.textParamValueOnNewLine = textParamValueOnNewLine;
	}

	public PredefinedBuildParameters(String properties) {
		this(properties, false);
	}

	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException, InterruptedException {

		EnvVars env = getEnvironment(build, listener);

		Properties p = ParameterizedTriggerUtils.loadProperties(getProperties());

		List<ParameterValue> values = new ArrayList<>();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			// support multi-line parameters correctly
			String s = entry.getValue().toString();
			if(textParamValueOnNewLine && s.contains("\n")) {
				values.add(new TextParameterValue(entry.getKey().toString(), env.expand(s)));
			} else {
				values.add(new StringParameterValue(entry.getKey().toString(), env.expand(s)));
			}
		}

		return new ParametersAction(values);
	}

	public String getProperties() {
		return properties;
	}

	public boolean getTextParamValueOnNewLine() {
		return textParamValueOnNewLine;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Predefined parameters";
		}
	}

}
