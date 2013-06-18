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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import hudson.model.Project;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Cause.UpstreamCause;
import hudson.model.FreeStyleProject;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import org.jvnet.hudson.test.Bug;

import hudson.matrix.TextAxis;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.AxisList;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.Recipe;
import org.jvnet.hudson.test.recipes.WithPlugin;
import org.jvnet.hudson.test.recipes.Recipe.Runner;

import com.google.common.collect.ImmutableList;
import hudson.model.Run;
import java.lang.System;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jenkins.model.Jenkins;

public class TriggerBuilderTest extends HudsonTestCase {

    private BlockableBuildTriggerConfig createTriggerConfig(String projects) {
        return new BlockableBuildTriggerConfig(projects, new BlockingBehaviour("never", "never", "never"), null);
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

        BlockingBehaviour blockingBehaviour = new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE);
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
    
    /**
     * annotation to allow install multiple plugins.
     * just trigger each WithPlugin annotation.
     */
    @Target(METHOD)
    @Retention(RUNTIME)
    @Recipe(WithPlugins.RunnerImpl.class)
    public static @interface WithPlugins {
        WithPlugin[] value();
        public class RunnerImpl extends Recipe.Runner<WithPlugins> {
            static private class WithPluginInfo {
                WithPlugin plugin;
                Runner<WithPlugin> runner;
                
                public WithPluginInfo(WithPlugin plugin, Runner<WithPlugin> runner)
                {
                    this.plugin = plugin;
                    this.runner = runner;
                }
            }
            private WithPlugins a;
            private List<WithPluginInfo> recipes = new ArrayList<WithPluginInfo>();
            
            @Override
            public void setup(HudsonTestCase testCase, WithPlugins recipe) throws Exception {
                a = recipe;
                for(WithPlugin plugin: a.value()) {
                    Recipe r = plugin.annotationType().getAnnotation(Recipe.class);
                    if(r==null)continue;
                    @SuppressWarnings("unchecked")
                    final Runner<WithPlugin> runner = (Runner<WithPlugin>)r.value().newInstance();
                    recipes.add(new WithPluginInfo(plugin, runner));
                    runner.setup(testCase,plugin);
                }
            }
            
            @Override
            public void tearDown(HudsonTestCase testCase, WithPlugins recipe)
                    throws Exception
            {
                for(WithPluginInfo info: recipes) {
                    info.runner.tearDown(testCase, info.plugin);
                }
                super.tearDown(testCase, recipe);
            }

            @Override
            public void decorateHome(HudsonTestCase testCase, File home) throws Exception {
                for(WithPluginInfo info: recipes) {
                    info.runner.decorateHome(testCase, home);
                }
            }
        }
    }
    
    @Bug(17751)
    @WithPlugins({@WithPlugin("javadoc-1.0.hpi"), @WithPlugin("promoted-builds-2.10.hpi")})
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
