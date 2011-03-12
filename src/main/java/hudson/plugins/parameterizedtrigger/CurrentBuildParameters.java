package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.FileParameterValue;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class CurrentBuildParameters extends AbstractBuildParameters {

	@DataBoundConstructor
	public CurrentBuildParameters() {
	}

	@Override
	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException {

		ParametersAction action = build.getAction(ParametersAction.class);
		if (action == null) {
			listener.getLogger().println("[parameterized-trigger] Current build has no parameters.");
			return null;
		} else {
			List<ParameterValue> values = new ArrayList<ParameterValue>(action.getParameters().size());
			for (ParameterValue value : action.getParameters())
				// FileParameterValue is currently not reusable, so omit these:
				if (!(value instanceof FileParameterValue))
					values.add(value);
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
