package hudson.plugins.parameterizedtrigger;

import hudson.util.EnumConverter;

import org.kohsuke.stapler.Stapler;

public class Plugin extends hudson.Plugin {

	@Override
	public void start() throws Exception {
		Stapler.CONVERT_UTILS.register(new EnumConverter(),
				ResultCondition.class);
	}

}
