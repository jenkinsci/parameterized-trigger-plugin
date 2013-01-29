/*
 * The MIT License
 *
 * Copyright (c) 2004-2012, Chris Johnson
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory.NoFilesFoundEnum;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;

import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.io.IOException;


public class FileBuildParameterFactoryTest extends HudsonTestCase {

    private TriggerBuilder createTriggerBuilder(AbstractProject project, NoFilesFoundEnum action){

        TriggerBuilder tBuilder = new TriggerBuilder(
                                new BlockableBuildTriggerConfig(project.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(
                                    new FileBuildParameterFactory("*.txt", action)),
                                Collections.<AbstractBuildParameters>emptyList()));
        return tBuilder;
    }

    @Test
	public void testSingleFile() throws Exception {

        //create triggered build, with capture env builder
        Project projectB = createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        //create triggering build
        FreeStyleProject projectA = createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
            build.getWorkspace().child("abc.txt").write("TEST=hello_abc","UTF-8");
            return true;
            }
        });

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(1, builds.size());

        Set<String> values = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(ImmutableSet.of("hello_abc"), values);

    }

    @Test
    public void testMultipleFiles() throws Exception {

        //create triggered build, with capture env builder
        Project projectB = createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        //create triggering build
        FreeStyleProject projectA = createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
            build.getWorkspace().child("abc.txt").write("TEST=hello_abc","UTF-8");
            build.getWorkspace().child("xyz.txt").write("TEST=hello_xyz","UTF-8");
            build.getWorkspace().child("xyz.properties").write("TEST=hello_xyz","UTF-8");
            return true;
            }
        });

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(2, builds.size());

        Set<String> values = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(ImmutableSet.of("hello_abc","hello_xyz"), values);

    }

    @Test
    public void testNoFilesSkip() throws Exception {

        //create triggered build, with capture env builder
        Project projectB = createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        //create triggering build
        FreeStyleProject projectA = createFreeStyleProject();

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(0, builds.size());
    }

    @Test
    public void testNoFilesNoParms() throws Exception {

        //create triggered build, with capture env builder
        Project projectB = createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        //create triggering build
        FreeStyleProject projectA = createFreeStyleProject();

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.NOPARMS));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(1, builds.size());
    }

    @Test(expected = RuntimeException.class)
    public void testNoFilesFail() throws Exception {

        //create triggered build, with capture env builder
        Project projectB = createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        //create triggering build
        FreeStyleProject projectA = createFreeStyleProject();

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.FAIL));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(0, builds.size());
    }

}
