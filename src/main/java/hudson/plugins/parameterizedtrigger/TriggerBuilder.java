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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import hudson.*;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.model.Job;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.Build;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.util.RunList;
import hudson.util.IOException2;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.stapler.DataBoundConstructor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.lang.InterruptedException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * {@link Builder} that triggers other projects and optionally waits for their completion.
 *
 * @author Kohsuke Kawaguchi
 */
public class TriggerBuilder extends Builder {

    private final ArrayList<BlockableBuildTriggerConfig> configs;

    @DataBoundConstructor
    public TriggerBuilder(List<BlockableBuildTriggerConfig> configs) {
        this.configs = new ArrayList<BlockableBuildTriggerConfig>(Util.fixNull(configs));
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        boolean buildStepResult = true;

        try {
            for (BlockableBuildTriggerConfig config : configs) {
                ListMultimap<Job, Future<Run>> futures = config.perform3(build, launcher, listener);
                // Only contains resolved projects
                List<Job> projectList = config.getJobs(build.getRootBuild().getProject().getParent(), env);

                // Get the actual defined projects
                StringTokenizer tokenizer = new StringTokenizer(config.getProjects(env), ",");

                if (tokenizer.countTokens() == 0) {
                    throw new AbortException("Build aborted. No projects to trigger. Check your configuration!");
                } else if (tokenizer.countTokens() != projectList.size()) {

                    int nbrOfResolved = tokenizer.countTokens()-projectList.size();

                    // Identify the unresolved project(s)
                    Set<String> unsolvedProjectNames = new TreeSet<String>();
                    while (tokenizer.hasMoreTokens()) {
                        unsolvedProjectNames.add(tokenizer.nextToken().trim());
                    }
                    for (Job project : projectList) {
                        unsolvedProjectNames.remove(project.getFullName());
                    }

                    // Present the undefined project(s) in error message
                    StringBuffer missingProject = new StringBuffer();
                    for (String projectName : unsolvedProjectNames) {
                        missingProject.append(" > ");
                        missingProject.append(projectName);
                        missingProject.append("\n");
                    }

                    throw new AbortException("Build aborted. Can't trigger undefined projects. "+nbrOfResolved+" of the below project(s) can't be resolved:\n" + missingProject.toString() + "Check your configuration!");
                } else {
                    //handle non-blocking configs
                    if(futures.isEmpty()){
                        listener.getLogger().println("Triggering projects: " + getProjectListAsString(projectList));
                        for(Job p : projectList) {
                            BuildInfoExporterAction.addBuildInfoExporterAction(build, p.getFullName());
                        }
                        continue;
                    }
                    //handle blocking configs
                    for (Job p : projectList) {
                        //handle non-buildable projects
                        if(!p.isBuildable()){
                            listener.getLogger().println("Skipping " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()) + ". The project is either disabled or the configuration has not been saved yet.");
                            continue;
                        }
                        for (Future<Run> future : futures.get(p)) {
                            try {
                                if (future != null ) {
                                    listener.getLogger().println("Waiting for the completion of " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
                                    Run b = future.get();
                                    listener.getLogger().println(HyperlinkNote.encodeTo('/' + b.getUrl(), b.getFullDisplayName()) + " completed. Result was " + b.getResult());
                                    BuildInfoExporterAction.addBuildInfoExporterAction(build, b.getParent().getFullName(), b.getNumber(), b.getResult());

                                    if (buildStepResult && config.getBlock().mapBuildStepResult(b.getResult())) {
                                        build.setResult(config.getBlock().mapBuildResult(b.getResult()));
                                    } else {
                                        buildStepResult = false;
                                    }
                                } else {
                                    listener.getLogger().println("Skipping " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()) + ". The project was not triggered by some reason.");
                                }
                            } catch (CancellationException x) {
                                throw new AbortException(p.getFullDisplayName() +" aborted.");
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException y) {
            for (BlockableBuildTriggerConfig config : configs) {
                Queue buildQueue = Jenkins.getInstance().getQueue();
                for (Queue.Item queueItem : buildQueue.getItems()) {
                    List<Cause> causes = queueItem.getCauses();
                    boolean isBuildCause = false;
                    for (Cause c : causes) {
                        if (c == null) {
                            continue;
                        }
                        if (!(c instanceof UpstreamCause)) {
                            continue;
                        }
                        UpstreamCause upstreamCause = (UpstreamCause) c;
                        if (upstreamCause.pointsTo(build)) {
                            isBuildCause = true;
                            break;
                        }
                    }
                    if (isBuildCause) {
                        try {
                            queueItem.doCancelQueue();
                        } catch (javax.servlet.ServletException ex) {
                            // pass
                        }
                    }
                }

                List<Job> projectList = config.getJobs(build.getRootBuild().getProject().getParent(), env);
                for (Job p : projectList) {
                    Item item = Jenkins.getInstance().getItem(p.getFullName(), p, Item.class);
                    for (Job job : item.getAllJobs()) {
                        RunList runList = job.getNewBuilds();
                        for (Iterator<Build> it = runList.iterator(); it.hasNext(); ) {
                            Build latestBuild = it.next();
                            UpstreamCause cause = (UpstreamCause) latestBuild.getCause(UpstreamCause.class);
                            if (cause == null) {
                                continue;
                            }
                            if (cause.pointsTo(build)) {
                                try {
                                    listener.getLogger().println("Aborting " + HyperlinkNote.encodeTo('/' + latestBuild.getUrl(), latestBuild.getFullDisplayName()));
                                    latestBuild.doStop();
                                } catch (javax.servlet.ServletException ex) {
                                    // pass, build has already completed or aborted
                                }
                            }
                        }
                    }
                }
            }
            throw new InterruptedException();
        } catch (ExecutionException e) {
            throw new IOException2(e); // can't happen, I think.
        }

        return buildStepResult;
    }

    // Public but restricted so we can add tests without completely changing the tests package
    @Restricted(value=org.kohsuke.accmod.restrictions.NoExternalUse.class)
    public String getProjectListAsString(List<Job> projectList){
        StringBuilder projectListString = new StringBuilder();
        for (Iterator<Job> iterator = projectList.iterator(); iterator.hasNext();) {
            Job project = iterator.next();
            projectListString.append(HyperlinkNote.encodeTo('/'+ project.getUrl(), project.getFullDisplayName()));
            if(iterator.hasNext()){
                projectListString.append(", ");
            }
        }
        return projectListString.toString();
    }


    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return ImmutableList.of(new SubProjectsAction(project, configs));
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
