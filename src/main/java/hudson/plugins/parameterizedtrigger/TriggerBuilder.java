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
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
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
                ListMultimap<AbstractProject, Future<AbstractBuild>> futures = config.perform2(build, launcher, listener);
                // Only contains resolved projects
                List<AbstractProject> projectList = config.getProjectList(build.getRootBuild().getProject().getParent(),env);

                // Get the actual defined projects
                StringTokenizer tokenizer = new StringTokenizer(config.getProjects(), ",");

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
                        for (Future<AbstractBuild> future : futures.get(p)) {
                            try {
                                listener.getLogger().println("Waiting for the completion of " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
                                AbstractBuild b = future.get();
                                listener.getLogger().println(HyperlinkNote.encodeTo('/'+ b.getUrl(), b.getFullDisplayName()) + " completed. Result was "+b.getResult());
                                BuildInfoExporterAction.addBuildInfoExporterAction(build, b.getProject().getFullName(), b.getNumber(), b.getResult());

                                if(buildStepResult && config.getBlock().mapBuildStepResult(b.getResult())) {
                                    build.setResult(config.getBlock().mapBuildResult(b.getResult()));
                                } else {
                                    buildStepResult = false;
                                }
                            } catch (CancellationException x) {
                                throw new AbortException(p.getFullDisplayName() +" aborted.");
                            }
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            throw new IOException2(e); // can't happen, I think.
        }

        return buildStepResult;
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
}
