package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.scm.RevisionParameterAction;
import hudson.scm.SubversionSCM.SvnInfo;
import hudson.scm.SubversionTagAction;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class SubversionRevisionBuildParameters extends AbstractBuildParameters {

    private boolean includeUpstreamParameters = false;

    public SubversionRevisionBuildParameters() {
        this(false);
    }

    @DataBoundConstructor
    public SubversionRevisionBuildParameters(boolean includeUpstreamParameters) {
        this.includeUpstreamParameters = includeUpstreamParameters;
    }

    public boolean getIncludeUpstreamParameters() {
        return includeUpstreamParameters;
    }

    @Override
    public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) {

        SubversionTagAction tagAction = build.getAction(SubversionTagAction.class);
        RevisionParameterAction revisionAction = build.getAction(RevisionParameterAction.class);

        List<SvnInfo> infos = new ArrayList<>();

        if (tagAction == null) {
            listener.getLogger()
                    .println(Plugin.LOG_TAG + " no SubversionTagAction found -- is this project an SVN project ?");
        } else {
            infos.addAll(tagAction.getTags().keySet());
        }

        if (includeUpstreamParameters) {
            if (revisionAction == null) {
                listener.getLogger()
                        .println(
                                Plugin.LOG_TAG
                                        + " no RevisionParameterAction found -- project did not have SVN parameters passed to it?");
            } else {
                infos.addAll(revisionAction.getRevisions());
            }
        }
        // if infos is empty don't return an action.
        return (infos.size() == 0) ? null : new RevisionParameterAction(infos);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {

        @Override
        public String getDisplayName() {
            return "Subversion revision";
        }
    }
}
