package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescriptorList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class BuildTrigger extends Publisher {

	private final List<BuildTriggerConfig> configs;

	public BuildTrigger(List<BuildTriggerConfig> configs) {
		this.configs = configs;
	}

	public BuildTrigger(BuildTriggerConfig... configs) {
		this(Arrays.asList(configs));
	}

	public List<BuildTriggerConfig> getConfigs() {
		return configs;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		for (BuildTriggerConfig config : configs) {
			config.trigger(build, launcher, listener);
		}

		return true;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<Publisher> {

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject json)
				throws FormException {
			return new BuildTrigger(newInstancesFromHeteroList(req, json,
					"configs", CONFIGS));
		}

		@Override
		public String getHelpFile() {
			return "/plugin/parameterized-trigger/help/plugin.html";
		}

		@Override
		public String getDisplayName() {
			return "Trigger parameterized build on other projects";
		}

		public DescriptorList<BuildTriggerConfig> getBuilderConfigDescriptors() {
			return CONFIGS;
		}
	}

	public static final DescriptorList<BuildTriggerConfig> CONFIGS = new DescriptorList<BuildTriggerConfig>(
			BuildTriggerConfig.class);

}
