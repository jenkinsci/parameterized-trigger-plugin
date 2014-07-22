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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

public class TriggerBuilderTest extends HudsonTestCase {

    private BlockableBuildTriggerConfig createTriggerConfig(String projects) {
        return new BlockableBuildTriggerConfig(projects, new BlockingBehaviour("never", "never", "never", 0, null), null);
    }

    public void testOrderOfLogEntries() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");
        createFreeStyleProject("project4");
        createFreeStyleProject("project5");
        createFreeStyleProject("project6");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

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

    public void testSubParameterBuilds() throws Exception {
        hudson.setNumExecutors(10); // makes sure there are enough executors so that there are no deadlocks
        hudson.setNodes(hudson.getNodes()); // update nodes configuration

        FreeStyleProject p1 = createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        ///triggered from project 1
        createFreeStyleProject("projectZ4");
        createFreeStyleProject("projectZ5");
        createFreeStyleProject("projectZ6");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

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

    public void testWaitingForCompletion() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Waiting for the completion of project2",
                "Waiting for the completion of project3");
    }

    public void testShouldNotRetry() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        project.getBuildersList().add(getFailingBuilder());

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertEquals(project.getBuilds().size(), 1);
        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1");
    }

    public void testShouldRetryOnceThenNotMatch() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        project.getBuildersList().add(getFailingBuilder());

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1",
                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE, 2, "Build number 1 failing"), null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertEquals(2, project.getBuilds().size());
        assertLinesRegex(triggerProject.getLastBuild(),
                ".*Waiting for the completion of project1.*",
                ".*log matched retry pattern.*",
                ".*Waiting for the completion of project1.*");
    }

    public void testShouldRetryThreeTimesThenStop() throws Exception {
        FreeStyleProject project = createFreeStyleProject("project1");
        project.getBuildersList().add(getFailingBuilder());

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1",
                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE, 3, "failing"), null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertEquals(4, project.getBuilds().size());
        assertLinesRegex(triggerProject.getLastBuild(),
                ".*Waiting for the completion of project1.*",
                ".*log matched retry pattern.*",
                ".*Waiting for the completion of project1.*",
                ".*log matched retry pattern.*",
                ".*Waiting for the completion of project1.*",
                ".*log matched retry pattern.*",
                ".*Waiting for the completion of project1.*");
    }

    private TestBuilder getFailingBuilder() {
        return new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                listener.getLogger().println("Build number " + abstractBuild.getNumber() + " failing");
                abstractBuild.setResult(Result.FAILURE);
                return true;
            }
        };
    }

    public void testNonBlockingTrigger() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1, project2, project3", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Triggering projects: project1, project2, project3");
    }

    public void testConsoleOutputWithCounterParameters() throws Exception{
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?,?> triggerProject = createFreeStyleProject();

        BlockingBehaviour blockingBehaviour = new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE, 0, null);
        ImmutableList<AbstractBuildParameterFactory> buildParameter = ImmutableList.<AbstractBuildParameterFactory>of(new CounterBuildParameterFactory("0","2","1", "TEST=COUNT$COUNT"));
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


    public void testBlockingTriggerWithDisabledProjects() throws Exception {
        createFreeStyleProject("project1");
        Project<?, ?> p2 = createFreeStyleProject("project2");
        p2.disable();
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Skipping project2. The project is either disabled or the configuration has not been saved yet.",
                "Waiting for the completion of project3");
    }


    @Bug(14278)
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

        hudson.setNumExecutors(50);
        hudson.setNodes(hudson.getNodes()); // update nodes configuration

        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");
        createFreeStyleProject("project4");
        createFreeStyleProject("project5");
        createFreeStyleProject("project6");

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
    public void testTriggerFromPromotion() throws Exception {
        assertNotNull("promoted-builds must be installed.", Jenkins.getInstance().getPlugin("promoted-builds"));
        // Test combination with PromotedBuilds.
        // Assert that the original build can be tracked from triggered build.
        // The configuration is as following:
        // Project1 -> (built-in trigger) -> Project2
        //          -> (promotion) -> Project1/Promotion/TRIGGER -> (Parameterrized Trigger) -> Project3
        FreeStyleProject project1 = createFreeStyleProject();
        FreeStyleProject project2 = createFreeStyleProject();
        FreeStyleProject project3 = createFreeStyleProject();
        
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
        
        assertBuildStatusSuccess(project1_build);
        assertBuildStatusSuccess(project2_build);
        assertBuildStatusSuccess(project3_build);
        
        UpstreamCause c = project3_build.getCause(UpstreamCause.class);
        assertNotNull(String.format("Failed to get UpstreamCause from project3(%s)", project3.getName()), c);
        assertEquals("UpstreamCause is not properly set.", project1.getName(), c.getUpstreamProject());
    }

    @Override
    protected MatrixProject createMatrixProject(String name) throws IOException {
        MatrixProject p = super.createMatrixProject(name);
        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db","mysql","oracle"));
        axes.add(new TextAxis("direction","north","south"));
        p.setAxes(axes);

        return p;
    }

    public void testExpansionOfMultipleProjectsInEnvVariable() throws Exception {
        FreeStyleProject upstream = createFreeStyleProject();
        upstream.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("PARAM", "downstream1,downstream2")
        ));

        FreeStyleProject downstream1 = createFreeStyleProject("downstream1");
        FreeStyleProject downstream2 = createFreeStyleProject("downstream2");
        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("${PARAM}", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        upstream.getBuildersList().add(triggerBuilder);

        FreeStyleBuild upstreamBuild = upstream.scheduleBuild2(0, new Cause.UserCause(), new ParametersAction(new StringParameterValue("PARAM", "downstream1,downstream2"))).get();
        assertBuildStatusSuccess(upstreamBuild);
        waitUntilNoActivity();
        FreeStyleBuild downstream1Build = downstream1.getLastBuild();
        FreeStyleBuild downstream2Build = downstream2.getLastBuild();
        assertBuildStatusSuccess(downstream1Build);
        assertBuildStatusSuccess(downstream2Build);
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
}
