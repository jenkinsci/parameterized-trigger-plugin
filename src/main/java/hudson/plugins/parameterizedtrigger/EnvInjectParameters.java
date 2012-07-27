package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.TaskListener;


import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jenkinsci.lib.envinject.service.EnvInjectActionRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class EnvInjectParameters extends AbstractBuildParameters {

	@DataBoundConstructor
	public EnvInjectParameters() {
	}

	@Override
	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException {

        Action action = (new EnvInjectActionRetriever()).getEnvInjectAction(build);

		if (action == null) {
			listener.getLogger().println("[parameterized-trigger] Current build does not have Env Inject parameters.");
			return null;
		} else {
            return action;
		}
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {

		@Override
		public String getDisplayName() {
			return Messages.EnvInjectParameters_DisplayName();
		}
	}
}
