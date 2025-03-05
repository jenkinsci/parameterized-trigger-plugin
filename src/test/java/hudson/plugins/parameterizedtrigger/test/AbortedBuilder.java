package hudson.plugins.parameterizedtrigger.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * A Builder which simulates an aborted build.
 * To use for testing behaviour of Build Results.
 */
public class AbortedBuilder extends Builder {
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Simulating an aborted build");
        build.setResult(Result.ABORTED);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public DescriptorImpl() {}

        @NonNull
        @Override
        public String getDisplayName() {
            return "Make build aborted";
        }

        @Override
        public AbortedBuilder newInstance(StaplerRequest2 req, @NonNull JSONObject data) {
            return new AbortedBuilder();
        }
    }
}
