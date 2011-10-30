package hudson.plugins.parameterizedtrigger.test;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;

/**
 * @author wolfs
 */
public class CaptureAllEnvironmentBuilder extends Builder {
    private Map<String,EnvVars> envVars = Maps.newHashMap();

	public Map<String, EnvVars> getEnvVars() {
		return envVars;
	}

	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        envVars.put(build.getId(), build.getEnvironment(listener));
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            throw new UnsupportedOperationException();
        }

        public String getDisplayName() {
            return "Capture Environment Variables";
        }
    }
}
