package hudson.plugins.parameterizedtrigger.matrix;

import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Build parameter that controls which subset of {@link MatrixConfiguration} a downstream build will run.
 *
 * @author Kohsuke Kawaguchi
 */
public class MatrixSubsetBuildParameters extends AbstractBuildParameters {
    private final String filter;

	@DataBoundConstructor
	public MatrixSubsetBuildParameters(String filter) {
        this.filter = filter;
	}

    public String getFilter() {
        return filter;
    }

    @Override
	public Action getAction(AbstractBuild<?,?> build, TaskListener listener) throws IOException, InterruptedException {
        return new MatrixSubsetAction(build.getEnvironment(listener).expand(filter));
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return Messages.MatrixSubsetBuildParameters_DisplayName();
		}
	}

}

