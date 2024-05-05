package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.console.ModelHyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependencyGraph;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import jenkins.model.DependencyDeclarer;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class BuildTrigger extends Notifier implements DependencyDeclarer {

    private final ArrayList<BuildTriggerConfig> configs;

    @DataBoundConstructor
    public BuildTrigger(List<BuildTriggerConfig> configs) {
        this.configs = new ArrayList<>(Util.fixNull(configs));
    }

    public BuildTrigger(BuildTriggerConfig... configs) {
        this(Arrays.asList(configs));
    }

    public List<BuildTriggerConfig> getConfigs() {
        return configs;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return Collections.singletonList(new DynamicProjectAction(configs));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        Map<String, AbstractBuild> downstreamMap = new HashMap<>();
        Map<String, Integer> buildMap = new HashMap<>();
        boolean hasEnvVariables = false;

        HashSet<BuildTriggerConfig> alreadyFired = new HashSet<>();

        // If this project has non-abstract projects, we need to fire them
        for (BuildTriggerConfig config : configs) {
            boolean hasNonAbstractProject = false;
            hasEnvVariables = hasEnvVariables || hasEnvVariables(config, build.getEnvironment(listener));

            List<Job> jobs =
                    config.getJobs(build.getRootBuild().getProject().getParent(), build.getEnvironment(listener));

            for (Job j : jobs) {
                if (!(j instanceof AbstractProject)) {
                    hasNonAbstractProject = true;
                    break;
                }
            }
            // Fire this config's projects if not already fired
            if (hasNonAbstractProject) {
                config.perform(build, launcher, listener);
                alreadyFired.add(config);
            }
        }

        if (canDeclare(build.getProject()) && !hasEnvVariables) {
            // job will get triggered by dependency graph, so we have to capture buildEnvironment NOW before
            // hudson.model.AbstractBuild.AbstractBuildExecution#cleanUp is called and reset
            EnvVars env = build.getEnvironment(listener);
            build.addAction(new CapturedEnvironmentAction(env));
        } else { // Not using dependency graph
            for (BuildTriggerConfig config : configs) {
                if (!alreadyFired.contains(config)) {
                    // config.perform(build, launcher, listener);
                    List<QueueTaskFuture<AbstractBuild>> futures = config.perform(build, launcher, listener);
                    for (QueueTaskFuture future : futures) {
                        AbstractBuild abstractBuild = null;
                        try {
                            abstractBuild = (AbstractBuild) future.get();
                            if (null != abstractBuild) {
                                downstreamMap.put(abstractBuild.getProject().getFullName(), abstractBuild);
                            }
                        } catch (ExecutionException e) {
                            listener.getLogger().println("Failed to execute downstream build");
                        }
                    }

                    String[] projects =
                            config.getProjects(build.getEnvironment(listener)).split(",");
                    String[] vars = config.getProjects().split(",");
                    for (int i = 0; i < projects.length; i++) {
                        if (vars[i].trim().contains("$")) {
                            AbstractBuild abstractBuild = downstreamMap.get(projects[i]);
                            if (null != abstractBuild) {
                                listener.getLogger().println(makeLogEntry(projects[i].trim()));
                                buildMap.put(abstractBuild.getProject().getFullName(), abstractBuild.getNumber());
                            }
                        }
                    }
                }
                DynamicBuildAction action = new DynamicBuildAction(buildMap);
                build.addAction(action);
            }
        }

        return true;
    }

    private String makeLogEntry(String name) {
        String url = name;
        url = Jenkins.get().getRootUrl() + "job/" + url.replaceAll("/", "/job/");
        name = name.replaceAll("/", " » ");
        String link = ModelHyperlinkNote.encodeTo(url, name);
        StringBuilder sb = new StringBuilder();
        sb.append("Triggering a new build of ");
        sb.append(link);
        return sb.toString();
    }

    private boolean hasEnvVariables(BuildTriggerConfig config, EnvVars env) {
        return !config.getProjects().equalsIgnoreCase(config.getProjects(env));
    }

    @Override
    public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
        if (!canDeclare(owner)) return;

        for (BuildTriggerConfig config : configs) {
            List<AbstractProject> projectList = config.getProjectList(owner.getParent(), null);
            for (AbstractProject project : projectList) {
                if (config.isTriggerFromChildProjects() && owner instanceof ItemGroup) {
                    ItemGroup<Item> parent = (ItemGroup) owner;
                    for (Item item : parent.getItems()) {
                        if (item instanceof AbstractProject) {
                            AbstractProject child = (AbstractProject) item;
                            ParameterizedDependency.add(child, project, config, graph);
                        }
                    }
                } else {
                    ParameterizedDependency.add(owner, project, config, graph);
                }
            }
        }
    }

    private boolean canDeclare(AbstractProject owner) {
        // See HUDSON-5679 -- dependency graph is also not used when triggered from a promotion
        return !owner.getClass().getName().equals("hudson.plugins.promoted_builds.PromotionProcess");
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getDisplayName() {
            return "Trigger parameterized build on other projects";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
