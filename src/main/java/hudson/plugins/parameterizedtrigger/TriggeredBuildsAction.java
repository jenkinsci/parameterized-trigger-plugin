package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.Lists;
import hudson.model.AbstractBuild;

import java.util.List;
/**
 * Action added to individual builds to track what projects/builds
 * were started from the trigger builds on other projects buildstep
 *
 * Provides a section on the build page indicating the triggered projects/builds.
 * see summary.groovy
 *
 * @author wolfs
 */
public class TriggeredBuildsAction {
    private AbstractBuild<?, ?> parentBuild;

    public TriggeredBuildsAction(AbstractBuild<?,?> parentBuild) {
        super();
        this.parentBuild = parentBuild;
    }
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    public List<AbstractBuild<?, ?>> getTriggeredBuilds() {
        List<BuildInfoExporterAction> actions = parentBuild.getActions(BuildInfoExporterAction.class);
        List<AbstractBuild<?,?>> triggeredBuilds = Lists.newArrayList();
        for (BuildInfoExporterAction action : actions) {
            triggeredBuilds.add(action.getTriggeredBuild());
        }
        return triggeredBuilds;
    }
}
