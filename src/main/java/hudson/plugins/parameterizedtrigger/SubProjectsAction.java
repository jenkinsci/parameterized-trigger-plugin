package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableList;
import hudson.model.AbstractProject;
import hudson.model.Action;

import java.util.List;

public class SubProjectsAction implements Action {

    private AbstractProject<?,?> project;
    private List<BlockableBuildTriggerConfig> configs;

    public SubProjectsAction(AbstractProject<?,?> project, List<BlockableBuildTriggerConfig> configs) {
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
        return ImmutableList.of();
    }

    public AbstractProject<?,?> getProject() {
        return project;
    }

    private boolean isFirst() {
        return project.getAction(SubProjectsAction.class) == this;
    }
}
