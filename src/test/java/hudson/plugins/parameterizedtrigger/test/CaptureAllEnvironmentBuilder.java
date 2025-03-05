package hudson.plugins.parameterizedtrigger.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author wolfs
 */
public class CaptureAllEnvironmentBuilder extends Builder {
    private final Map<String, EnvVars> envVars = new HashMap<>();

    public Map<String, EnvVars> getEnvVars() {
        return envVars;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        envVars.put(build.getId(), build.getEnvironment(listener));
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        @Override
        public Builder newInstance(StaplerRequest2 req, @NonNull JSONObject data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDisplayName() {
            return "Capture Environment Variables";
        }
    }
}
