/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.plugins.parameterizedtrigger.test;

import com.google.common.collect.ArrayListMultimap;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.Shell;
import hudson.util.RunList;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;

import hudson.matrix.TextAxis;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.AxisList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Future;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

import com.google.common.collect.ImmutableList;

import java.lang.System;

import jenkins.model.Jenkins;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TriggerBuilderTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    private BlockableBuildTriggerConfig createTriggerConfig(String projects) {
        return new BlockableBuildTriggerConfig(projects, new BlockingBehaviour("never", "never", "never"), null);
    }

    @Test
    public void testProjectWasEnabledDuringTheBuild() throws Exception {
        final Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        final Project<?, ?> disabledJob = r.createFreeStyleProject("projectC");
        final BlockableBuildTriggerConfig config = Mockito.mock(BlockableBuildTriggerConfig.class);
        when(config.getProjects(any(EnvVars.class))).thenReturn(disabledJob.getName());
        when(config.getBlock()).thenReturn(new BlockingBehaviour(Result.FAILURE, Result.FAILURE, Result.FAILURE));

        final ArrayListMultimap<Job, Future<Run>> futures = ArrayListMultimap.create();
        when(config.perform3(any(AbstractBuild.class),
                Mockito.any(Launcher.class),
                Mockito.any(BuildListener.class))).thenReturn(futures);
        // Then project is disabled scheduler returns null instead of Future<Run> object
        futures.put(disabledJob, null);

        final List<Job> jobs = new ArrayList<Job>();
        jobs.add(disabledJob);

        when(config.getJobs(any(ItemGroup.class), any(EnvVars.class))).thenReturn(jobs);

        TriggerBuilder triggerBuilder = new TriggerBuilder(config);
        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertEquals(Result.SUCCESS, triggerProject.getLastBuild().getResult());
    }

    @Test
    public void testOrderOfLogEntries() throws Exception {
        r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");
        r.createFreeStyleProject("project4");
        r.createFreeStyleProject("project5");
        r.createFreeStyleProject("project6");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project2"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project3"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project4"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project5"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project6"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "project1 #1 completed. Result was SUCCESS",
                "project2 #1 completed. Result was SUCCESS",
                "project3 #1 completed. Result was SUCCESS",
                "project4 #1 completed. Result was SUCCESS",
                "project5 #1 completed. Result was SUCCESS",
                "project6 #1 completed. Result was SUCCESS");
    }

    @Issue("JENKINS-11257")
    @Test
    public void testDownstreamAbort() throws Exception {
        Shell shell = new Shell("sleep 1m");
        Project <?,?> project1 = r.createFreeStyleProject("project1");
        project1.getBuildersList().add(shell);

        final Project <?,?> triggerProject = r.createFreeStyleProject("projectA");
        triggerProject.getBuildersList().add(new TriggerBuilder(createTriggerConfig("project1")));

        Thread thread = TriggerBuilderScheduleThread(triggerProject);
        thread.start();

        Thread.sleep(100);
        AbortAllProjectBuilds(triggerProject);
        thread.join();

        r.assertBuildStatus(Result.ABORTED, project1.getLastBuild());
        assertLines(triggerProject.getLastBuild(),
                "Aborted project1 #1");

    }

    @Issue("JENKINS-11257")
    @Test
    public void testDownstreamQueueAbort() throws Exception {
        r.jenkins.setNumExecutors(1);
        r.createFreeStyleProject("project1");

        final Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        triggerProject.getBuildersList().add(new TriggerBuilder(createTriggerConfig("project1")));

        Thread thread = TriggerBuilderScheduleThread(triggerProject);
        thread.start();

        Thread.sleep(100);
        AbortAllProjectBuilds(triggerProject);
        thread.join();

        assertTrue(r.jenkins.getQueue().isEmpty());
    }

    @Test
    public void testSubParameterBuilds() throws Exception {
        r.jenkins.setNumExecutors(10); // makes sure there are enough executors so that there are no deadlocks
        r.jenkins.setNodes(r.jenkins.getNodes()); // update nodes configuration (TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary)

        FreeStyleProject p1 = r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");

        ///triggered from project 1
        r.createFreeStyleProject("projectZ4");
        r.createFreeStyleProject("projectZ5");
        r.createFreeStyleProject("projectZ6");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project2"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project3"));

        TriggerBuilder triggerBuilder2 = new TriggerBuilder(createTriggerConfig("projectZ4"));
        triggerBuilder2.getConfigs().add(createTriggerConfig("projectZ5"));
        triggerBuilder2.getConfigs().add(createTriggerConfig("projectZ6"));

        p1.getBuildersList().add(triggerBuilder2);
        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();
        assertLines(triggerProject.getLastBuild(),
            "project1 #1 completed. Result was SUCCESS",
            "project2 #1 completed. Result was SUCCESS",
            "project3 #1 completed. Result was SUCCESS");
    }

    @Test
    public void testWaitingForCompletion() throws Exception {
        r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Waiting for the completion of project2",
                "Waiting for the completion of project3");
    }

    @Test
    public void testNonBlockingTrigger() throws Exception {
        r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1, project2, project3", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Triggering projects: project1, project2, project3");
    }

    @Test
    public void testConsoleOutputWithCounterParameters() throws Exception{
        r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");

        Project<?,?> triggerProject = r.createFreeStyleProject();

        BlockingBehaviour blockingBehaviour = new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE);
        ImmutableList<AbstractBuildParameterFactory> buildParameter = ImmutableList.<AbstractBuildParameterFactory>of(new CounterBuildParameterFactory("0", "2", "1", "TEST=COUNT$COUNT"));
        List<AbstractBuildParameters> emptyList = Collections.<AbstractBuildParameters>emptyList();

        BlockableBuildTriggerConfig bBTConfig = new BlockableBuildTriggerConfig("project1, project2, project3", blockingBehaviour, buildParameter, emptyList);

        triggerProject.getBuildersList().add(new TriggerBuilder(bBTConfig));

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "project1 #1 completed. Result was SUCCESS",
                "project1 #2 completed. Result was SUCCESS",
                "project1 #3 completed. Result was SUCCESS",
                "project2 #1 completed. Result was SUCCESS",
                "project2 #2 completed. Result was SUCCESS",
                "project2 #3 completed. Result was SUCCESS",
                "project3 #1 completed. Result was SUCCESS",
                "project3 #2 completed. Result was SUCCESS",
                "project3 #3 completed. Result was SUCCESS");
    }

    @Test
    public void testBlockingTriggerWithDisabledProjects() throws Exception {
        r.createFreeStyleProject("project1");
        Project<?, ?> p2 = r.createFreeStyleProject("project2");
        p2.disable();
        r.createFreeStyleProject("project3");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Skipping project2. The project is either disabled, or the authenticated user " + User.current() +
                        " has no Item.BUILD permissions, or the configuration has not been saved yet.",
                "Waiting for the completion of project3");
    }

    /** Verify that workflow build can be triggered */
    @Test
    public void testTriggerWithWorkflow() throws Exception {
        WorkflowJob p = (WorkflowJob) r.createProject(WorkflowJob.class, "project1");
        p.setDefinition(new CpsFlowDefinition("println('hello')"));

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerProject.getBuildersList().add(triggerBuilder);
        triggerProject.scheduleBuild2(0).get();

        assertNotNull(triggerProject.getLastBuild());
        assertEquals(1, p.getBuilds().size());
        assertNotNull(p.getLastBuild());
    }

    /** Verify that getProjectsList works with workflow and normal projects */
    @Issue("JENKINS-30040")
    @Test
    public void testGetProjectsList() throws Exception {
        WorkflowJob p = (WorkflowJob) r.createProject(WorkflowJob.class, "project1");
        p.setDefinition(new CpsFlowDefinition("println('hello')"));
        Project<?, ?> p2 = r.createFreeStyleProject("project2");

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerProject.getBuildersList().add(triggerBuilder);

        List<Job> jobs = new ArrayList<Job>();
        jobs.add(p);
        jobs.add(p2);
        String projectListAsString = triggerBuilder.getProjectListAsString(jobs);
        r.assertStringContains(projectListAsString, "project1");
        r.assertStringContains(projectListAsString, "project2");
    }

    /** Verify that workflow build can be triggered with normal project too */
    @Test
    public void testTriggerWithWorkflowMixedTypes() throws Exception {
        r.createFreeStyleProject("project1");
        WorkflowJob p = (WorkflowJob) r.createProject(WorkflowJob.class, "project2");
        p.setDefinition(new CpsFlowDefinition("println('hello')"));

        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2"));
        triggerProject.getBuildersList().add(triggerBuilder);
        triggerProject.scheduleBuild2(0).get();

        assertNotNull(triggerProject.getLastBuild());
        assertEquals(1, p.getBuilds().size());
        assertNotNull(p.getLastBuild());
    }

    @Bug(14278)
    @Test
    public void testBlockingTriggerWithMatrixProject() throws Exception {

        /* This test case will start a matrix project that is configured with 2 Axis
         * each with two possible values giving 4 combinations.
         * the build is configured with a TriggerBuilder which will block waiting for
         * 6 other projects to complete.
         *
         * To allow this to run with no jobs being queued we need to have enogh exectors for all builds
         * That is 1 + 4 + (4*6) = 29
         * The minimun number of executors needed to allow test to run with queued builds would be
         * 1 + 4 + 1 = 5 that is one exector for all of the builds that start others and
         * and also a free executor to allow the queue to progress
         *
         * Set as 50 for first case.
         */

        r.jenkins.setNumExecutors(50);
        r.jenkins.setNodes(r.jenkins.getNodes()); // update nodes configuration

        r.createFreeStyleProject("project1");
        r.createFreeStyleProject("project2");
        r.createFreeStyleProject("project3");
        r.createFreeStyleProject("project4");
        r.createFreeStyleProject("project5");
        r.createFreeStyleProject("project6");

        MatrixProject matrixProject = createMatrixProject("matrixProject");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project2"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project3"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project4"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project5"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project6"));

        matrixProject.getBuildersList().add(triggerBuilder);

        matrixProject.scheduleBuild2(0).get();

        List<String> log2 = matrixProject.getLastBuild().getLog(20);
        System.out.println(log2);

        List<MatrixRun> runs = matrixProject.getLastBuild().getRuns();

        assertEquals(4,runs.size());

        for (MatrixRun run : runs) {
            assertLinesRegex(run,
                "project1 #[0-9] completed. Result was SUCCESS",
                "project2 #[0-9] completed. Result was SUCCESS",
                "project3 #[0-9] completed. Result was SUCCESS",
                "project4 #[0-9] completed. Result was SUCCESS",
                "project5 #[0-9] completed. Result was SUCCESS",
                "project6 #[0-9] completed. Result was SUCCESS");
        }
    }

    @Bug(17751)
    @Test
    public void testTriggerFromPromotion() throws Exception {
        assertNotNull("promoted-builds must be installed.", Jenkins.getInstance().getPlugin("promoted-builds"));
        // Test combination with PromotedBuilds.
        // Assert that the original build can be tracked from triggered build.
        // The configuration is as following:
        // Project1 -> (built-in trigger) -> Project2
        //          -> (promotion) -> Project1/Promotion/TRIGGER -> (Parameterrized Trigger) -> Project3
        FreeStyleProject project1 = r.createFreeStyleProject();
        FreeStyleProject project2 = r.createFreeStyleProject();
        FreeStyleProject project3 = r.createFreeStyleProject();

        // project1 -> project2
        project1.getPublishersList().add(new hudson.tasks.BuildTrigger(project2.getName(), "SUCCESS"));

        // promotion for project1.
        hudson.plugins.promoted_builds.JobPropertyImpl promote = new hudson.plugins.promoted_builds.JobPropertyImpl(project1);
        project1.addProperty(promote);

        // promotion process to trigger project3
        PromotionProcess pp = promote.addProcess("TRIGGER");
        pp.conditions.add(new DownstreamPassCondition(project2.getName()));
        pp.getBuildSteps().add(new TriggerBuilder(createTriggerConfig(project3.getName())));
        // When using built-in BuildTrigger, set up as following:
        //pp.getBuildSteps().add(new hudson.tasks.BuildTrigger(project3.getName(), "SUCCESS"));

        // Are there any other ways to enable a new BuildTrigger?
        Jenkins.getInstance().rebuildDependencyGraph();

        project1.scheduleBuild2(0);

        // wait for all builds finish
        long timeout = 30000;
        long till = System.currentTimeMillis() + timeout;
        FreeStyleBuild project1_build = null;
        FreeStyleBuild project2_build = null;
        FreeStyleBuild project3_build = null;

        while(true) {
            Thread.sleep(1000);
            if(project1_build == null) {
                project1_build = project1.getLastBuild();
            }
            if(project2_build == null) {
                project2_build = project2.getLastBuild();
            }
            if(project3_build == null) {
                project3_build = project3.getLastBuild();
            }
            if(project1_build != null && !project1_build.isBuilding()
                    && project2_build != null && !project2_build.isBuilding()
                    && project3_build != null && !project3_build.isBuilding()
            ) {
                break;
            }

            if(System.currentTimeMillis() > till) {
                // something not completed.
                assertNotNull(
                        String.format("Failed to trigger project1(%s)", project1.getName()),
                        project1_build
                );
                assertFalse(
                        String.format("project1(%s) does not finish.", project1.getName()),
                        project1_build.isBuilding()
                );
                assertNotNull(
                        String.format("Failed to trigger project2(%s)", project2.getName()),
                        project2_build
                );
                assertFalse(
                        String.format("project2(%s) does not finish.", project2.getName()),
                        project2_build.isBuilding()
                );
                assertNotNull(
                        String.format("Failed to trigger project3(%s)", project3.getName()),
                        project3_build
                );
                assertFalse(
                        String.format("project3(%s) does not finish.", project3.getName()),
                        project3_build.isBuilding()
                );
                break;
            }
        }

        r.assertBuildStatusSuccess(project1_build);
        r.assertBuildStatusSuccess(project2_build);
        r.assertBuildStatusSuccess(project3_build);

        UpstreamCause c = project3_build.getCause(UpstreamCause.class);
        assertNotNull(String.format("Failed to get UpstreamCause from project3(%s)", project3.getName()), c);
        assertEquals("UpstreamCause is not properly set.", project1.getName(), c.getUpstreamProject());
    }

    protected MatrixProject createMatrixProject(String name) throws IOException {
        MatrixProject p = r.createProject(MatrixProject.class, name);
        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db","mysql","oracle"));
        axes.add(new TextAxis("direction","north","south"));
        p.setAxes(axes);

        return p;
    }

    @Test
    public void testExpansionOfMultipleProjectsInEnvVariable() throws Exception {
        FreeStyleProject upstream = r.createFreeStyleProject();
        upstream.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("PARAM", "downstream1,downstream2")
        ));

        FreeStyleProject downstream1 = r.createFreeStyleProject("downstream1");
        FreeStyleProject downstream2 = r.createFreeStyleProject("downstream2");
        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("${PARAM}", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        upstream.getBuildersList().add(triggerBuilder);

        FreeStyleBuild upstreamBuild = upstream.scheduleBuild2(0, new Cause.UserCause(), new ParametersAction(new StringParameterValue("PARAM", "downstream1,downstream2"))).get();
        r.assertBuildStatusSuccess(upstreamBuild);
        r.waitUntilNoActivity();
        FreeStyleBuild downstream1Build = downstream1.getLastBuild();
        FreeStyleBuild downstream2Build = downstream2.getLastBuild();
        r.assertBuildStatusSuccess(downstream1Build);
        r.assertBuildStatusSuccess(downstream2Build);
    }

    @Test
    public void testProjectTriggeredOnce() throws Exception {
        r.jenkins.setQuietPeriod(0);
        Project<?, ?> triggerProject = r.createFreeStyleProject("projectA");
        Project<?, ?> triggeredProject = r.createFreeStyleProject("project1");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);
        triggerProject.getBuildersList().add(new SleepBuilder(500));

        triggerProject.scheduleBuild2(0).get();
        Thread.sleep(500);
        assertEquals(triggeredProject.getBuilds().toArray().length, 1);
    }

    private void assertLines(Run<?,?> build, String... lines) throws IOException {
        List<String> log = build.getLog(Integer.MAX_VALUE);
        List<String> rest = log;
        for (String line : lines) {
            int where = rest.indexOf(line);
            assertFalse("Could not find line '" + line + "' among remaining log lines " + rest, where == -1);
            rest = rest.subList(where + 1, rest.size());
        }
    }

    private void assertLinesRegex (Run<?,?> build, String... regexs) throws IOException {
        // Same function as above but allows regex instead of just strings
        List<String> log = build.getLog(Integer.MAX_VALUE);
        List<String> rest = log;
        ListIterator li = log.listIterator();

        Pattern p = Pattern.compile(""); // initial pattern will be replaced in loop
        Matcher m = p.matcher((String)li.next());
        for (String regex : regexs) {
            int lastmatched = 0;
            m.usePattern(Pattern.compile(regex));
            while (li.hasNext()) {
                m.reset((String)li.next());
                if (m.matches()) {
                    lastmatched = li.nextIndex();
                    li = log.listIterator(li.nextIndex());
                    break;
                }
            }
            // set up rest to contain the part of the log that has not been successfully checked
            rest = log.subList(lastmatched + 1, log.size());
            assertTrue("Could not find regex '" + regex + "' among remaining log lines " + rest, li.hasNext() );
        }
    }

    private Thread TriggerBuilderScheduleThread(final Project<?,?> triggerProject) throws IOException {

        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    triggerProject.scheduleBuild2(0).get();
                } catch (InterruptedException e) {
                    //pass
                } catch (ExecutionException e) {
                    //pass
                }
            }
        };
        return new Thread(runnable);
    }

    private void AbortAllProjectBuilds(Project<?,?> project) throws IOException {
        for (Job job : project.getAllJobs()) {
            if (job.getName().equals(project.getName())) {
                RunList runList = job.getBuilds();
                for (Iterator<Build> it = runList.iterator(); it.hasNext(); ) {
                    Run run = it.next();
                        run.getExecutor().doStop();
                }
            }
        }
    }
}
