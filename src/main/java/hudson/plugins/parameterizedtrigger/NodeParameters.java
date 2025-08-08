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
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Chris johnson
 */
public class NodeParameters extends AbstractBuildParameters {

    @DataBoundConstructor
    public NodeParameters() {}

    @Override
    public Action getAction(AbstractBuild<?, ?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException {
        String nodeName = build.getBuiltOnStr();
        Label nodeLabel;
        String nodeDisplayName;
        // Controller does not return a node name so add it explicitly.
        if (nodeName == null || nodeName.isEmpty()) {
            nodeLabel = Jenkins.get().getSelfLabel();
            nodeDisplayName = nodeLabel.getDisplayName();
        } else {
            nodeLabel = Label.get(nodeName);
            nodeDisplayName = nodeLabel != null ? nodeLabel.getDisplayName() : "null label of " + nodeName;
        }
        listener.getLogger().println("Returning node parameter for " + nodeDisplayName);
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
