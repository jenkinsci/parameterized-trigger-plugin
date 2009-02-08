package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;

import java.io.IOException;

public abstract class BuildTriggerConfig implements Describable<BuildTriggerConfig> {

	public abstract void trigger(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException;

}
