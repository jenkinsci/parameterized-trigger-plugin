package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class CurrentBuildParameters extends AbstractBuildParameters {

	@DataBoundConstructor
	public CurrentBuildParameters() {
	}

	@Override
	public Action getAction(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		ParametersAction action = build.getAction(ParametersAction.class);
		if (action == null) {
			listener.getLogger().println("[parameterized-trigger] current build has no parameters");
			throw new IOException("current build has no parameters");
		} else {
			List<ParameterValue> values = new ArrayList<ParameterValue>();
			values.addAll(action.getParameters());
			return new ParametersAction(values);
		}
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {

		@Override
		public String getDisplayName() {
			return "Current build parameters";
		}

	}

}
