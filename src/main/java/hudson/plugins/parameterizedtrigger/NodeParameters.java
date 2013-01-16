/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import java.io.IOException;
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
		String nodename = build.getBuiltOnStr();
		// master does not return a node name so add it explicitly.
		if(nodename == "") {
			nodename = "master";
		}
		listener.getLogger().println("current node is " + nodename);
		return new NodeAction(nodename);
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Build on the same node";
		}
	}
}
