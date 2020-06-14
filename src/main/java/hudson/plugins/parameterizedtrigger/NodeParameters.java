/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.TaskListener;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Chris johnson
 */
public class NodeParameters extends AbstractBuildParameters{

	@DataBoundConstructor
	public NodeParameters() {
	}

	@Override
	public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
		String nodeName = build.getBuiltOnStr();
		Label nodeLabel;
		// master does not return a node name so add it explicitly.
		if( StringUtils.isEmpty( nodeName ) ) {
			nodeLabel = Jenkins.get().getSelfLabel();
		} else {
			nodeLabel = Label.get(nodeName);
		}
		listener.getLogger().println("Returning node parameter for " + nodeLabel.getDisplayName());
		return new NodeAction(nodeLabel);
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Build on the same node";
		}
	}
}
