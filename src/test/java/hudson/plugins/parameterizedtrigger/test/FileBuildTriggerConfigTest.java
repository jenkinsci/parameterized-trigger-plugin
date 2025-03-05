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

import static org.junit.jupiter.api.Assertions.*;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.LabelAxis;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.labels.LabelExpression;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FileBuildTriggerConfigTest {

    @Test
    void test(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        String properties = "KEY=value";
        projectA.setScm(new SingleFileSCM("properties.txt", properties));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB", ResultCondition.SUCCESS, new FileBuildParameters("properties.txt"))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("KEY", "key"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        r.jenkins.getQueue().getItem(projectB).getFuture().get();

        assertNotNull(builder.getEnvVars(), "builder should record environment");
        assertEquals("value", builder.getEnvVars().get("KEY"));
    }

    @Test
    void test_multiplefiles(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        projectA.setScm(new ExtractResourceSCM(getClass().getResource("multiple_property_files.zip")));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB",
                        ResultCondition.SUCCESS,
                        new FileBuildParameters("a_properties.txt,z_properties.txt"))));
        projectA.getPublishersList().add(new ArtifactArchiver("a_properties.txt"));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("A_TEST_01", "test1"));
        definition.add(new StringParameterDefinition("A_TEST_02", "test2"));
        definition.add(new StringParameterDefinition("A_TEST_03", "test3"));
        definition.add(new StringParameterDefinition("Z_TEST_100", "test1"));
        definition.add(new StringParameterDefinition("Z_TEST_101", "test2"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        r.jenkins.getQueue().getItem(projectB).getFuture().get();

        assertNotNull(builder.getEnvVars(), "builder should record environment");
        // test from first file
        assertEquals("These_three_values_should", builder.getEnvVars().get("A_TEST_01"));
        assertEquals("be_from_file_a_properties_txt", builder.getEnvVars().get("A_TEST_02"));
        assertEquals("which_has_three_definitions", builder.getEnvVars().get("A_TEST_03"));
        // test from second file
        assertEquals("These_two_values_should", builder.getEnvVars().get("Z_TEST_100"));
        assertEquals("be_from_file_z_properties_txt", builder.getEnvVars().get("Z_TEST_101"));
    }

    @Test
    void test_failOnMissingFile(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        projectA.setScm(new ExtractResourceSCM(getClass().getResource("multiple_property_files.zip")));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB",
                        ResultCondition.SUCCESS,
                        new FileBuildParameters("a_properties.txt,missing_file.txt,z_properties.txt", true))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        r.waitUntilNoActivity();

        // There should be no builds of projectB as not triggered.
        assertEquals(0, projectB.getBuilds().size());
    }

    @Test
    void testUtf8File(JenkinsRule r) throws Exception {
        FreeStyleProject projectA = r.createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n" // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes(StandardCharsets.UTF_8)));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB",
                        ResultCondition.SUCCESS,
                        new FileBuildParameters("properties.txt", "UTF-8", true))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            projectA.scheduleBuild2(0).get();
            r.jenkins.getQueue().getItem(projectB).getFuture().get();

            assertNotNull(builder.getEnvVars(), "builder should record environment");
            assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
            assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testShiftJISFile(JenkinsRule r) throws Exception {
        // ShiftJIS is an encoding of Japanese texts.
        // I test here that a non-UTF-8 encoding also works.

        FreeStyleProject projectA = r.createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n" // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes("Shift_JIS")));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB",
                        ResultCondition.SUCCESS,
                        new FileBuildParameters("properties.txt", "Shift_JIS", true))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            projectA.scheduleBuild2(0).get();
            r.jenkins.getQueue().getItem(projectB).getFuture().get();

            assertNotNull(builder.getEnvVars(), "builder should record environment");
            assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
            assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testPlatformDefaultEncodedFile(JenkinsRule r) throws Exception {
        // ShiftJIS is an encoding of Japanese texts.
        // I test here that a non-UTF-8 encoding also works.

        FreeStyleProject projectA = r.createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n" // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes()));
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB", ResultCondition.SUCCESS, new FileBuildParameters("properties.txt"))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            projectA.scheduleBuild2(0).get();
            r.jenkins.getQueue().getItem(projectB).getFuture().get();

            assertNotNull(builder.getEnvVars(), "builder should record environment");

            // This test explicitly uses the platform's default encoding, which e.g. on Windows is likely to be
            // windows-1250
            // or windows-1252. With these single-byte encodings we cannot expect multi-byte strings to be encoded
            // correctly.
            final boolean isMultiByteDefaultCharset =
                    Charset.defaultCharset().newEncoder().maxBytesPerChar() > 1.0f;
            if (isMultiByteDefaultCharset) {
                assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
                assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
            }
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testDoCheckEncoding(JenkinsRule r) {
        FileBuildParameters.DescriptorImpl d =
                (FileBuildParameters.DescriptorImpl) r.jenkins.getDescriptorOrDie(FileBuildParameters.class);

        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding(null).kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("  ").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("UTF-8").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("Shift_JIS").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding(" UTF-8 ").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckEncoding("NoSuchEncoding").kind);
    }

    @Test
    void testNullifyEncoding(JenkinsRule r) {
        // to use default encoding, encoding must be null.
        {
            FileBuildParameters target = new FileBuildParameters("*.properties", null, false);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameters target = new FileBuildParameters("*.properties", "", false);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameters target = new FileBuildParameters("*.properties", "  ", false);
            assertNull(target.getEncoding());
        }
    }

    /**
     * Builder that writes a file.
     */
    private static class WriteFileBuilder extends Builder {
        private final String filename;
        private final String content;
        private final String encoding;

        public WriteFileBuilder(String filename, String content, String encoding) {
            this.filename = filename;
            this.content = content;
            this.encoding = encoding;
        }

        public WriteFileBuilder(String filename, String content) {
            this(filename, content, Charset.defaultCharset().name());
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            EnvVars env = build.getEnvironment(listener);
            env.putAll(build.getBuildVariables());
            build.getWorkspace().child(filename).write(env.expand(content), encoding);
            return true;
        }
    }

    private static String getStringParameterValue(AbstractBuild<?, ?> build, String name) {
        ParametersAction action = build.getAction(ParametersAction.class);
        if (action == null) {
            return null;
        }
        ParameterValue v = action.getParameter(name);
        if (!(v instanceof StringParameterValue)) {
            return null;
        }
        return ((StringParameterValue) v).value;
    }

    @Test
    void testMatrixBuildsOnSameNodes(JenkinsRule r) throws Exception {
        // all builds runs on controller.
        // upstream matrix projects creates properties files in each builds.
        MatrixProject upstream = r.createProject(MatrixProject.class);
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2")));
        WriteFileBuilder wfb = new WriteFileBuilder("properties.txt", "triggered_${childname}=true");

        FreeStyleProject downstream = r.createFreeStyleProject();

        // Without useMatrixBuild, publisher
        // Downstream project is triggered without parameters.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);

            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, false, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertNull(getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            build.delete();
        }

        // With useMatrixBuild, publisher
        // Downstream project is triggered with parameters, merging properties files in all children.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);

            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            // Build is triggered without parameters.
            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertEquals("true", getStringParameterValue(build, "triggered_child2"));
            build.delete();
        }

        // Without useMatrixBuild, builder
        // Downstream project is triggered with parameters of each child.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            upstream.getBuildersList()
                    .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                            downstream.getFullName(),
                            null,
                            List.of(new FileBuildParameters("properties.txt", null, false, false, null, false)))));

            upstream.getPublishersList().clear();

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            // Build is triggered in each builds with parameters.
            assertEquals(2, downstream.getBuilds().size());
            FreeStyleBuild build1 = downstream.getLastBuild();
            FreeStyleBuild build2 = build1.getPreviousBuild();

            if (build1.getCause(Cause.UpstreamCause.class).getUpstreamProject().contains("child1")) {
                assertEquals("true", getStringParameterValue(build1, "triggered_child1"));
                assertNull(getStringParameterValue(build1, "triggered_child2"));

                assertNull(getStringParameterValue(build2, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build2, "triggered_child2"));
            } else {
                assertEquals("true", getStringParameterValue(build2, "triggered_child1"));
                assertNull(getStringParameterValue(build2, "triggered_child2"));

                assertNull(getStringParameterValue(build1, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build1, "triggered_child2"));
            }

            build2.delete();
            build1.delete();
        }

        // With useMatrixBuild, publisher
        // Downstream project is triggered with parameters of each child.
        // (useMatrixBuild is ignored)
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            upstream.getBuildersList()
                    .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                            downstream.getFullName(),
                            null,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            upstream.getPublishersList().clear();

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            // Build is triggered in each builds with parameters.
            assertEquals(2, downstream.getBuilds().size());
            FreeStyleBuild build1 = downstream.getLastBuild();
            FreeStyleBuild build2 = build1.getPreviousBuild();

            if (build1.getCause(Cause.UpstreamCause.class).getUpstreamProject().contains("child1")) {
                assertEquals("true", getStringParameterValue(build1, "triggered_child1"));
                assertNull(getStringParameterValue(build1, "triggered_child2"));

                assertNull(getStringParameterValue(build2, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build2, "triggered_child2"));
            } else {
                assertEquals("true", getStringParameterValue(build2, "triggered_child1"));
                assertNull(getStringParameterValue(build2, "triggered_child2"));

                assertNull(getStringParameterValue(build1, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build1, "triggered_child2"));
            }

            build2.delete();
            build1.delete();
        }
    }

    @Test
    void testMatrixBuildsOnOtherNodes(JenkinsRule r) throws Exception {
        // each builds run on other nodes.
        // upstream matrix projects creates properties files in each builds.
        r.createOnlineSlave(LabelExpression.parseExpression("child1"));
        r.createOnlineSlave(LabelExpression.parseExpression("child2"));

        MatrixProject upstream = r.createProject(MatrixProject.class);
        upstream.setAxes(new AxisList(new LabelAxis("childname", Arrays.asList("child1", "child2"))));
        WriteFileBuilder wfb = new WriteFileBuilder("properties.txt", "triggered_${childname}=true");

        FreeStyleProject downstream = r.createFreeStyleProject();

        // Without useMatrixBuild, publisher
        // Downstream project is triggered without parameters.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);

            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, false, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertNull(getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            build.delete();
        }

        // With useMatrixBuild, publisher
        // Downstream project is triggered with parameters, merging properties files in all children.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);

            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertEquals("true", getStringParameterValue(build, "triggered_child2"));
            build.delete();
        }

        // Without useMatrixBuild, builder
        // Downstream project is triggered with parameters of each child.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            upstream.getBuildersList()
                    .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                            downstream.getFullName(),
                            null,
                            List.of(new FileBuildParameters("properties.txt", null, false, false, null, false)))));

            upstream.getPublishersList().clear();

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(2, downstream.getBuilds().size());
            FreeStyleBuild build1 = downstream.getLastBuild();
            FreeStyleBuild build2 = build1.getPreviousBuild();

            if (build1.getCause(Cause.UpstreamCause.class).getUpstreamProject().contains("child1")) {
                assertEquals("true", getStringParameterValue(build1, "triggered_child1"));
                assertNull(getStringParameterValue(build1, "triggered_child2"));

                assertNull(getStringParameterValue(build2, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build2, "triggered_child2"));
            } else {
                assertEquals("true", getStringParameterValue(build2, "triggered_child1"));
                assertNull(getStringParameterValue(build2, "triggered_child2"));

                assertNull(getStringParameterValue(build1, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build1, "triggered_child2"));
            }

            build2.delete();
            build1.delete();
        }

        // With useMatrixBuild, publisher
        // Downstream project is triggered with parameters of each child.
        // (useMatrixBuild is ignored)
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            upstream.getBuildersList()
                    .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                            downstream.getFullName(),
                            null,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            upstream.getPublishersList().clear();

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(2, downstream.getBuilds().size());
            FreeStyleBuild build1 = downstream.getLastBuild();
            FreeStyleBuild build2 = build1.getPreviousBuild();

            if (build1.getCause(Cause.UpstreamCause.class).getUpstreamProject().contains("child1")) {
                assertEquals("true", getStringParameterValue(build1, "triggered_child1"));
                assertNull(getStringParameterValue(build1, "triggered_child2"));

                assertNull(getStringParameterValue(build2, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build2, "triggered_child2"));
            } else {
                assertEquals("true", getStringParameterValue(build2, "triggered_child1"));
                assertNull(getStringParameterValue(build2, "triggered_child2"));

                assertNull(getStringParameterValue(build1, "triggered_child1"));
                assertEquals("true", getStringParameterValue(build1, "triggered_child2"));
            }

            build2.delete();
            build1.delete();
        }
    }

    @Test
    void testMatrixBuildsCombinationFilter(JenkinsRule r) throws Exception {
        MatrixProject upstream = r.createProject(MatrixProject.class);
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2", "child3")));
        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "triggered_${childname}=true"));

        FreeStyleProject downstream = r.createFreeStyleProject();

        // without combinationFilter
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertEquals("true", getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }

        // with combinationFilter
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters(
                                    "properties.txt", null, false, true, "childname!='child2'", false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }
    }

    @Test
    void testMatrixBuildsOnlyExactRuns(JenkinsRule r) throws Exception {
        MatrixProject upstream = r.createProject(MatrixProject.class);
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2", "child3")));
        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "triggered_${childname}=true"));

        FreeStyleProject downstream = r.createFreeStyleProject();

        // Run build.
        // builds of child1, child2, child3 is created.
        upstream.scheduleBuild2(0).get();

        // child2 is dropped
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child3")));

        // without onlyExactRuns
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, false)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            MatrixBuild b = upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();
            System.out.println(">>>>>>>>" + b.getLog());

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertEquals("true", getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }

        // with onlyExactRuns
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList()
                    .add(new BuildTrigger(new BuildTriggerConfig(
                            downstream.getFullName(),
                            ResultCondition.SUCCESS,
                            true,
                            List.of(new FileBuildParameters("properties.txt", null, false, true, null, true)))));

            r.jenkins.rebuildDependencyGraph();

            assertEquals(0, downstream.getBuilds().size());

            upstream.scheduleBuild2(0).get();
            r.waitUntilNoActivity();

            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }
    }

    @Issue("JENKINS-22705")
    @Test
    void testMatrixBuildsConfiguration(JenkinsRule r) throws Exception {
        FreeStyleProject downstream = r.createFreeStyleProject();

        MatrixProject upstream = r.createProject(MatrixProject.class);
        upstream.setAxes(new AxisList(new TextAxis("axis1", "value1", "value2")));
        upstream.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        downstream.getFullName(),
                        ResultCondition.SUCCESS,
                        true,
                        List.of(new FileBuildParameters(
                                "properties.txt", "UTF-8", true, true, "axis1=value1", true)))));
        upstream.save();

        String upstreamName = upstream.getFullName();

        upstream = r.jenkins.getItemByFullName(upstreamName, MatrixProject.class);
        assertNotNull(upstream);

        BuildTrigger trigger = upstream.getPublishersList().get(BuildTrigger.class);
        assertNotNull(trigger);

        assertEquals(1, trigger.getConfigs().size());

        BuildTriggerConfig config = trigger.getConfigs().get(0);

        assertEquals(1, config.getConfigs().size());
        FileBuildParameters p = (FileBuildParameters) config.getConfigs().get(0);
        assertEquals("properties.txt", p.getPropertiesFile());
        assertEquals("UTF-8", p.getEncoding());
        assertTrue(p.getFailTriggerOnMissing());
        assertTrue(p.isUseMatrixChild());
        assertEquals("axis1=value1", p.getCombinationFilter());
        assertTrue(p.isOnlyExactRuns());
    }

    @Test
    void testAbsolutePath(JenkinsRule r) throws Exception {
        FreeStyleProject downstream = r.createFreeStyleProject();

        FreeStyleProject upstream = r.createFreeStyleProject();

        File absoluteFile = new File(r.jenkins.getRootDir(), "properties.txt");
        if (!absoluteFile.getParentFile().exists()) {
            FileUtils.forceMkdir(absoluteFile.getParentFile());
        }
        FileUtils.writeStringToFile(absoluteFile, "absolute_param=value1");

        File workspace = new File(r.jenkins.getWorkspaceFor(upstream).getRemote());
        File relativeDir = workspace.getParentFile();

        if (!relativeDir.exists()) {
            FileUtils.forceMkdir(relativeDir);
        }
        File relativeFile = new File(relativeDir, "properties.txt");
        FileUtils.writeStringToFile(relativeFile, "relative_param1=value2");

        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "relative_param2=value3"));
        upstream.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        downstream.getFullName(),
                        ResultCondition.SUCCESS,
                        true,
                        List.of(new FileBuildParameters(String.format(
                                "%s,../properties.txt,properties.txt", absoluteFile.getAbsolutePath()))))));

        r.jenkins.rebuildDependencyGraph();

        r.assertBuildStatusSuccess(upstream.scheduleBuild2(0));
        r.waitUntilNoActivity();

        FreeStyleBuild build = downstream.getLastBuild();
        assertNotNull(build);
        assertEquals("value1", getStringParameterValue(build, "absolute_param"));
        assertEquals("value2", getStringParameterValue(build, "relative_param1"));
        assertEquals("value3", getStringParameterValue(build, "relative_param2"));
    }

    /**
     * Builder that removes a workspace.
     */
    private static class WorkspaceRemoveBuilder extends Builder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            build.getWorkspace().deleteRecursive();
            return true;
        }
    }

    @Issue("JENKINS-22229")
    @Test
    void testAbsolutePathWithoutWorkspace(JenkinsRule r) throws Exception {
        // Though it is rather a problem with ws-cleanup-plugin,
        // there's a case a workspace is removed.
        FreeStyleProject downstream = r.createFreeStyleProject();

        FreeStyleProject upstream = r.createFreeStyleProject();

        File absoluteFile = new File(r.jenkins.getRootDir(), "properties.txt");
        if (!absoluteFile.getParentFile().exists()) {
            FileUtils.forceMkdir(absoluteFile.getParentFile());
        }
        FileUtils.writeStringToFile(absoluteFile, "absolute_param=value1");

        upstream.getBuildersList().add(new WorkspaceRemoveBuilder());
        upstream.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        downstream.getFullName(),
                        ResultCondition.SUCCESS,
                        true,
                        List.of(new FileBuildParameters(absoluteFile.getAbsolutePath())))));

        r.jenkins.rebuildDependencyGraph();

        r.assertBuildStatusSuccess(upstream.scheduleBuild2(0));
        r.waitUntilNoActivity();

        FreeStyleBuild build = downstream.getLastBuild();
        assertNotNull(build);
        assertEquals("value1", getStringParameterValue(build, "absolute_param"));
    }
}
