package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class PredefinedBuildParameters extends AbstractBuildParameters {

	private final String properties;

	@DataBoundConstructor
	public PredefinedBuildParameters(String properties) {
		this.properties = properties;
	}

	public Action getAction(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		String resolved = build.getEnvironment(listener).expand(properties);

		Properties p = new Properties();
		p.load(new StringInputStream(resolved));

		List<ParameterValue> values = new ArrayList<ParameterValue>();
		for (Map.Entry<Object, Object> entry : p.entrySet()) {
			values.add(new StringParameterValue(entry.getKey().toString(),
					entry.getValue().toString()));
		}

		return new ParametersAction(values);
	}

	public String getProperties() {
		return properties;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Predefined parameters";
		}
	}

}
