package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows a list of a dynamic downstream builds
 */
public class DynamicBuildAction implements RunAction2 {

    private final Map<String, Integer> buildsMap;

    public DynamicBuildAction(Map<String, Integer> buildsMap) {
        this.buildsMap = buildsMap;
    }

    @Override
    public void onAttached(Run<?, ?> run) {

    }

    @Override
    public void onLoad(Run<?, ?> run) {

    }

    public List<AbstractBuild<?, ?>> getBuilds() {
        List<AbstractBuild<?, ?>> builds = new ArrayList<>();
        Jenkins j = Jenkins.getInstance();
        for (Map.Entry<String, Integer> entry : buildsMap.entrySet()) {
            Job<?, ?> job = j.getItemByFullName(entry.getKey(), Job.class);
            if (null != job && job instanceof AbstractProject) {
                AbstractProject project = (AbstractProject) job;
                AbstractBuild build = project.getBuildByNumber(entry.getValue());
                if (null != build) {
                    builds.add(build);
                }
            }
        }

        return builds;
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
