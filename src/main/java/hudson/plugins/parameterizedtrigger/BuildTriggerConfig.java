package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.IOException;

public abstract class BuildTriggerConfig implements Describable<BuildTriggerConfig> {

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
	
	@Override
	public Descriptor<BuildTriggerConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
	}
	
}
