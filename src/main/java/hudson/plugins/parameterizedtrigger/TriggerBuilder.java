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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;

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
                ListMultimap<AbstractProject, Future<AbstractBuild>> futures = config.perform2(build, launcher, listener);
                // Only contains resolved projects
                List<AbstractProject> projectList = config.getProjectList(build.getRootBuild().getProject().getParent(),env);

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
                    for (AbstractProject project : projectList) {
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
                        for(AbstractProject p : projectList) {
                            BuildInfoExporterAction.addBuildInfoExporterAction(build, p.getFullName());
                        }
                        continue;
                    }
                    //handle blocking configs
                    for (AbstractProject p : projectList) {
                        //handle non-buildable projects
                        if(!p.isBuildable()){
                            listener.getLogger().println("Skipping " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()) + ". The project is either disabled or the configuration has not been saved yet.");
                            continue;
                        }
                        List<Future<AbstractBuild>> projectFutures = futures.get(p);
                        int retryCount = 0;
                        while (retryCount <= config.getBlock().retryCount) {
                            retryCount++;
                            List<Future<AbstractBuild>> newFutures = new ArrayList<Future<AbstractBuild>>();
                            for (Future<AbstractBuild> future : projectFutures) {
                                try {
                                    listener.getLogger().println("Waiting for the completion of " + HyperlinkNote.encodeTo('/' + p.getUrl(), p.getFullDisplayName()));
                                    AbstractBuild b = future.get();
                                    listener.getLogger().println(HyperlinkNote.encodeTo('/' + b.getUrl(), b.getFullDisplayName()) + " completed. Result was " + b.getResult());
                                    BuildInfoExporterAction.addBuildInfoExporterAction(build, b.getProject().getFullName(), b.getNumber(), b.getResult());

                                    if (b.getResult().isWorseThan(Result.UNSTABLE)
                                            && config.getBlock().retryCount > 0
                                            && parseLog(b.getLogFile(), config.getBlock().retryPattern)) {
                                        List<Action> actions = copyBuildCausesAndAddUserCause(b);
                                        ParametersAction action = b.getAction(ParametersAction.class);
                                        actions.add(action);

                                        listener.getLogger().println(HyperlinkNote.encodeTo('/' + b.getUrl(), b.getFullDisplayName()) + " had result " + b.getResult()
                                                + ", log matched retry pattern " + config.getBlock().retryPattern + " and " + (config.getBlock().retryCount - retryCount)
                                                + " retries remain, so rebuilding.");
                                        newFutures.add(config.schedule(build, p, actions));
                                    } else if (buildStepResult && config.getBlock().mapBuildStepResult(b.getResult())) {
                                        build.setResult(config.getBlock().mapBuildResult(b.getResult()));
                                    } else {
                                        buildStepResult = false;
                                    }
                                } catch (CancellationException x) {
                                    throw new AbortException(p.getFullDisplayName() + " aborted.");
                                }
                            }
                            projectFutures = new ArrayList<Future<AbstractBuild>>(newFutures);
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            throw new IOException2(e); // can't happen, I think.
        }

        return buildStepResult;
    }

    private boolean parseLog(File logFile, String regexp) throws IOException {
        if (regexp == null || regexp.equals("")) {
            return false;
        }

        // Assume default encoding and text files
        String line;
        Pattern pattern = Pattern.compile(regexp);
        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the build causes and adds or replaces the {@link hudson.model.Cause.UserIdCause}. The result is a
     * list of all build causes from the original build (might be an empty list), plus a
     * {@link hudson.model.Cause.UserIdCause} for the user who started the rebuild.
     *
     * @param fromBuild the build to copy the causes from.
     * @return list with all original causes and a {@link hudson.model.Cause.UserIdCause}.
     */
    private List<Action> copyBuildCausesAndAddUserCause(AbstractBuild<?, ?> fromBuild) {
        List currentBuildCauses = fromBuild.getCauses();

        List<Action> actions = new ArrayList<Action>(currentBuildCauses.size());
        boolean hasUserCause = false;
        for (Object buildCause : currentBuildCauses) {
            if (buildCause instanceof Cause.UserIdCause) {
                hasUserCause = true;
                actions.add(new CauseAction(new Cause.UserIdCause()));
            } else {
                actions.add(new CauseAction((Cause)buildCause));
            }
        }
        if (!hasUserCause) {
            actions.add(new CauseAction(new Cause.UserIdCause()));
        }

        return actions;
    }



    private String getProjectListAsString(List<AbstractProject> projectList){
        StringBuilder projectListString = new StringBuilder();
        for (Iterator iterator = projectList.iterator(); iterator.hasNext();) {
            AbstractProject project = (AbstractProject) iterator.next();
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

    private static final Logger LOGGER = Logger.getLogger(TriggerBuilder.class.getName());

}
