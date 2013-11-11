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

import ch.lambdaj.collection.LambdaCollections;
import ch.lambdaj.function.convert.Converter;
import com.google.common.collect.ImmutableList;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.*;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory.NoFilesFoundEnum;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Joiner.on;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class FileBuildParameterFactoryTest extends HudsonTestCase {

    public static final String PROPERTY_KEY = "TEST";
    public static final String PROPERTY_KEY_CYRILLIC = "TEST2";
    public static final String PROPERTY_VALUE_CYRILLIC = "значение";
    public static final String PROPERTY_VALUE_1 = "hello_abc";
    public static final String PROPERTY_VALUE_2 = "hello_xyz";


    private TriggerBuilder createTriggerBuilder(AbstractProject project, NoFilesFoundEnum action) {

        TriggerBuilder tBuilder = new TriggerBuilder(
                new BlockableBuildTriggerConfig(project.getName(),
                        new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                        ImmutableList.<AbstractBuildParameterFactory>of(
                                new FileBuildParameterFactory("*.txt", action)),
                        Collections.<AbstractBuildParameters>emptyList()));
        return tBuilder;
    }

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
                build.getWorkspace().child("abc.txt")
                        .write(on("\n").join(
                                PROPERTY_KEY + "=" + PROPERTY_VALUE_1,
                                PROPERTY_KEY_CYRILLIC + "=" + PROPERTY_VALUE_CYRILLIC
                        ), "UTF-8");
                return true;
            }
        });

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertThat("there is not 1 build in list", builds, hasSize(1));

        List<Map<? extends String, ? extends String>> buildEnvVars = LambdaCollections.with(builds)
                .convert(getEnvVars(builder));

        assertThat(buildEnvVars, everyItem(hasEntry(PROPERTY_KEY, PROPERTY_VALUE_1)));
        assertThat(buildEnvVars, everyItem(hasEntry(PROPERTY_KEY_CYRILLIC, PROPERTY_VALUE_CYRILLIC)));
    }

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
                build.getWorkspace().child("abc.txt").write(PROPERTY_KEY + "=" + PROPERTY_VALUE_1, "UTF-8");
                build.getWorkspace().child("xyz.txt").write(PROPERTY_KEY + "=" + PROPERTY_VALUE_2, "UTF-8");
                build.getWorkspace().child("xyz.properties").write(PROPERTY_KEY + "=" + PROPERTY_VALUE_2, "UTF-8");
                return true;
            }
        });

        // add Trigger builder, with file paramter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertThat("wrong number of builds was triggered", builds, hasSize(2));


        List<Map<? extends String, ? extends String>> buildEnvVars = LambdaCollections.with(builds)
                .convert(getEnvVars(builder));

        assertThat(buildEnvVars, hasItem(allOf(
                hasEntry(PROPERTY_KEY, PROPERTY_VALUE_1),
                not(hasEntry(PROPERTY_KEY, PROPERTY_VALUE_2)))));

        assertThat(buildEnvVars, hasItem(allOf(
                hasEntry(PROPERTY_KEY, PROPERTY_VALUE_2),
                not(hasEntry(PROPERTY_KEY, PROPERTY_VALUE_1)))));
    }

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


    /**
     * Transforms builds IDs to EnvVars (casted to Map<String, String>)
     *
     * @param builder - it captures envvars for every build
     * @return lambda-style converter
     */
    private Converter<FreeStyleBuild, Map<? extends String, ? extends String>> getEnvVars(
            final CaptureAllEnvironmentBuilder builder) {
        return new Converter<FreeStyleBuild, Map<? extends String, ? extends String>>() {
            public Map<String, String> convert(FreeStyleBuild from) {
                return builder.getEnvVars().get(from.getId());
            }
        };
    }

}
