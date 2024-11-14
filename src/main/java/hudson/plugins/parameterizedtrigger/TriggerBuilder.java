/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, InfraDNA, Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ListMultimap;
import hudson.*;
import hudson.console.HyperlinkNote;
import hudson.console.ModelHyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependencyGraph;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import jenkins.model.DependencyDeclarer;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link Builder} that triggers other projects and optionally waits for their completion.
 *
 * @author Kohsuke Kawaguchi
 */
public class TriggerBuilder extends Builder implements DependencyDeclarer {

    private static final Logger LOGGER = Logger.getLogger(TriggerBuilder.class.getName());

    private final ArrayList<BlockableBuildTriggerConfig> configs;

    @DataBoundConstructor
    public TriggerBuilder(List<BlockableBuildTriggerConfig> configs) {
        this.configs = new ArrayList<>(Util.fixNull(configs));
    }

    public TriggerBuilder(BlockableBuildTriggerConfig... configs) {
        this(Arrays.asList(configs));
    }

    public List<BlockableBuildTriggerConfig> getConfigs() {
        return configs;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        boolean buildStepResult = true;

        try {
            for (BlockableBuildTriggerConfig config : configs) {
                ListMultimap<Job, QueueTaskFuture<AbstractBuild>> futures = config.perform3(build, launcher, listener);
                // Only contains resolved projects
                List<Job> projectList =
                        config.getJobs(build.getRootBuild().getProject().getParent(), env);

                // Get the actual defined projects
                StringTokenizer tokenizer = new StringTokenizer(config.getProjects(env), ",");

                if (tokenizer.countTokens() == 0) {
                    throw new AbortException("Build aborted. No projects to trigger. Check your configuration!");
                } else if (tokenizer.countTokens() != projectList.size()) {

                    int nbrOfResolved = tokenizer.countTokens() - projectList.size();

                    // Identify the unresolved project(s)
                    Set<String> unsolvedProjectNames = new TreeSet<>();
                    while (tokenizer.hasMoreTokens()) {
                        unsolvedProjectNames.add(tokenizer.nextToken().trim());
                    }
                    for (Job project : projectList) {
                        unsolvedProjectNames.remove(project.getFullName());
                    }

                    // Present the undefined project(s) in error message
                    StringBuilder missingProject = new StringBuilder();
                    for (String projectName : unsolvedProjectNames) {
                        missingProject.append(" > ");
                        missingProject.append(projectName);
                        missingProject.append("\n");
                    }

                    throw new AbortException("Build aborted. Can't trigger undefined projects. " + nbrOfResolved
                            + " of the below project(s) can't be resolved:\n" + missingProject.toString()
                            + "Check your configuration!");
                } else {
                    // handle non-blocking configs
                    if (futures.isEmpty()) {
                        listener.getLogger().println("Triggering projects: " + getProjectListAsString(projectList));
                        for (Job p : projectList) {
                            BuildInfoExporterAction.addBuildInfoExporterAction(build, p.getFullName());
                        }
                        continue;
                    }
                    // handle blocking configs
                    for (Job p : projectList) {
                        // handle non-buildable projects
                        if (!config.canBeScheduled(p)) {
                            User user = User.current();
                            String userName = user != null ? ModelHyperlinkNote.encodeTo(user) : "unknown";
                            listener.getLogger()
                                    .println("Skipping "
                                            + HyperlinkNote.encodeTo('/' + p.getUrl(), p.getFullDisplayName())
                                            + ". The project is either disabled,"
                                            + " or the authenticated user " + userName
                                            + " has no Item.BUILD permissions,"
                                            + " or the configuration has not been saved yet.");
                            continue;
                        }
                        for (QueueTaskFuture<AbstractBuild> future : futures.get(p)) {
                            try {
                                if (future == null) {
                                    listener.getLogger()
                                            .println("Skipping " + ModelHyperlinkNote.encodeTo(p)
                                                    + ". The project was not triggered for some reason.");
                                    continue;
                                }

                                listener.getLogger()
                                        .println("Waiting for the completion of "
                                                + HyperlinkNote.encodeTo('/' + p.getUrl(), p.getFullDisplayName()));
                                Run startedRun;
                                try {
                                    startedRun = future.waitForStart();
                                } catch (InterruptedException x) {
                                    listener.getLogger()
                                            .println("Build aborting: cancelling queued project "
                                                    + HyperlinkNote.encodeTo('/' + p.getUrl(), p.getFullDisplayName()));
                                    future.cancel(true);
                                    throw x; // rethrow so that the triggering project get flagged as cancelled
                                }

                                listener.getLogger()
                                        .println(HyperlinkNote.encodeTo(
                                                        '/' + startedRun.getUrl(), startedRun.getFullDisplayName())
                                                + " started.");

                                Run completedRun = future.get();
                                Result completedResult = completedRun.getResult();
                                listener.getLogger()
                                        .println(HyperlinkNote.encodeTo(
                                                        '/' + completedRun.getUrl(), completedRun.getFullDisplayName())
                                                + " completed. Result was " + completedResult);
                                BuildInfoExporterAction.addBuildInfoExporterAction(
                                        build,
                                        completedRun.getParent().getFullName(),
                                        completedRun.getNumber(),
                                        completedResult);

                                if (buildStepResult && config.getBlock().mapBuildStepResult(completedResult)) {
                                    Result r = config.getBlock().mapBuildResult(completedResult);
                                    if (r != null) { // The blocking job is not a success
                                        build.setResult(r);
                                    }
                                } else {
                                    buildStepResult = false;
                                }
                            } catch (CancellationException x) {
                                throw new AbortException(p.getFullDisplayName() + " aborted.");
                            }
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            throw new IOException(e); // can't happen, I think.
        }

        return buildStepResult;
    }

    // Public but restricted so we can add tests without completely changing the tests package
    @Restricted(value = org.kohsuke.accmod.restrictions.NoExternalUse.class)
    public String getProjectListAsString(List<Job> projectList) {
        StringBuilder projectListString = new StringBuilder();
        for (Iterator<Job> iterator = projectList.iterator(); iterator.hasNext(); ) {
            Job project = iterator.next();
            projectListString.append(HyperlinkNote.encodeTo('/' + project.getUrl(), project.getFullDisplayName()));
            if (iterator.hasNext()) {
                projectListString.append(", ");
            }
        }
        return projectListString.toString();
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return Collections.singletonList(new SubProjectsAction(project, configs));
    }

    private boolean canDeclare(AbstractProject owner) {
        // See HUDSON-5679 -- dependency graph is also not used when triggered from a promotion
        return !owner.getClass().getName().equals("hudson.plugins.promoted_builds.PromotionProcess");
    }

    @Override
    public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
        if (!canDeclare(owner)) return;

        for (BuildTriggerConfig config : configs) {
            List<AbstractProject> projectList = config.getProjectList(owner.getParent(), null);
            for (AbstractProject project : projectList) {
                graph.addDependency(new TriggerBuilderDependency(owner, project, config));
            }
        }
    }

    public static class TriggerBuilderDependency extends ParameterizedDependency {
        public TriggerBuilderDependency(
                AbstractProject upstream, AbstractProject downstream, BuildTriggerConfig config) {
            super(upstream, downstream, config);
        }

        @Override
        public boolean shouldTriggerBuild(AbstractBuild build, TaskListener listener, List<Action> actions) {
            return false;
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Trigger/call builds on other projects";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
