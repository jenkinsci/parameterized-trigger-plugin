package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractProject;
import hudson.model.Action;
import java.util.Collections;
import java.util.List;

/**
 * Action added Projects to track what projects are
 * were triggered from the trigger builds on other projects buildstep.
 *
 * Provides a section on the project page indicating the triggered projects.
 * see jobMain.groovy
 *
 * @author wolfs
 */
public class SubProjectsAction implements Action {

    private final AbstractProject<?, ?> project;
    private final List<BlockableBuildTriggerConfig> configs;

    public SubProjectsAction(AbstractProject<?, ?> project, List<BlockableBuildTriggerConfig> configs) {
        this.project = project;
        this.configs = configs;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public List<BlockableBuildTriggerConfig> getConfigs() {
        return configs;
    }

    public List<SubProjectsAction> getSubProjectActions() {
        if (isFirst()) {
            return project.getActions(SubProjectsAction.class);
        }
        return Collections.emptyList();
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }

    private boolean isFirst() {
        return project.getAction(SubProjectsAction.class) == this;
    }
}
