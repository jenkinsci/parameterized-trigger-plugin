package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.Lists;
import hudson.model.AbstractBuild;

import java.util.List;

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
