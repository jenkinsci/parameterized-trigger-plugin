package hudson.plugins.parameterizedtrigger;

import org.kohsuke.stapler.Stapler;

import hudson.tasks.BuildStep;
import hudson.util.EnumConverter;

public class Plugin extends hudson.Plugin {

	@Override
	public void start() throws Exception {
		Stapler.CONVERT_UTILS.register(new EnumConverter(),
				ResultCondition.class);
	}

}
