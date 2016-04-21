package hudson.plugins.parameterizedtrigger;

import hudson.model.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of a downstream jobs that specified with parameters.
 */
public class DynamicProjectAction implements Action {

    private List<BuildTriggerConfig> configs;

    public DynamicProjectAction(List<BuildTriggerConfig> configs) {
        this.configs = configs;
    }

    public List<String> getProjects() {
        List<String> projects = new ArrayList<String>();
        for (BuildTriggerConfig config : configs) {
            for (String project : config.getProjects().split(",")) {
                if (project.trim().contains("$")) {
                    projects.add(project.trim());
                }
            }
        }
        return projects;
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
}
