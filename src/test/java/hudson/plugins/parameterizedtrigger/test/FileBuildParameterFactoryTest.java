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

import static org.junit.jupiter.api.Assertions.*;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory.NoFilesFoundEnum;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FileBuildParameterFactoryTest {

    private TriggerBuilder createTriggerBuilder(AbstractProject project, NoFilesFoundEnum action) {
        return createTriggerBuilder(project, action, null);
    }

    private TriggerBuilder createTriggerBuilder(AbstractProject project, NoFilesFoundEnum action, String encoding) {

        TriggerBuilder tBuilder = new TriggerBuilder(new BlockableBuildTriggerConfig(
                project.getName(),
                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                Collections.singletonList(new FileBuildParameterFactory("*.txt", encoding, action)),
                Collections.emptyList()));
        return tBuilder;
    }

    @Test
    void testSingleFile(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("TEST", "test"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("abc.txt").write("TEST=hello_abc", "UTF-8");
                return true;
            }
        });

        // add Trigger builder, with file parameter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(1, builds.size());

        Set<String> values = new HashSet<>();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(Collections.singleton("hello_abc"), values);
    }

    @Test
    void testMultipleFiles(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("TEST", "test"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("abc.txt").write("TEST=hello_abc", "UTF-8");
                build.getWorkspace().child("xyz.txt").write("TEST=hello_xyz", "UTF-8");
                build.getWorkspace().child("xyz.properties").write("TEST=hello_xyz", "UTF-8");
                return true;
            }
        });
        // add Trigger builder, with file parameter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(2, builds.size());

        Set<String> values = new HashSet<>();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(new HashSet<>(Arrays.asList("hello_abc", "hello_xyz")), values);
    }

    @Test
    void testNoFilesSkip(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();

        // add Trigger builder, with file parameter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(0, builds.size());
    }

    @Test
    void testNoFilesNoParams(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();

        // add Trigger builder, with file parameter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.NOPARMS));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(1, builds.size());
    }

    @Test
    void testNoFilesFail(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();

        // add Trigger builder, with file parameter factory
        projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.FAIL));

        projectA.scheduleBuild2(0).get();

        // check triggered builds are correct.
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(0, builds.size());
    }

    @Test
    void testUtf8File(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("abc.txt")
                        .write(
                                "TEST=こんにちは\n" // "hello" in Japanese.
                                        + "ＴＥＳＴ=hello_abc", // TEST in multibytes.
                                "UTF-8");
                return true;
            }
        });

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            // add Trigger builder, with file parameter factory
            projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP, "UTF-8"));

            projectA.scheduleBuild2(0).get();

            // check triggered builds are correct.
            r.waitUntilNoActivity();
            List<FreeStyleBuild> builds = projectB.getBuilds();
            assertEquals(1, builds.size());

            for (FreeStyleBuild build : builds) {
                EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
                System.out.printf("'%s'%n", "こんにちは");
                System.out.printf("'%s'%n", buildEnvVar.get("TEST"));
                assertEquals("こんにちは", buildEnvVar.get("TEST"));
                assertEquals("hello_abc", buildEnvVar.get("ＴＥＳＴ"));
            }
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testShiftJISFile(JenkinsRule r) throws Exception {
        // ShiftJIS is an encoding of Japanese texts.
        // I test here that a non-UTF-8 encoding also works.

        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("abc.txt")
                        .write(
                                "TEST=こんにちは\n" // "hello" in Japanese.
                                        + "ＴＥＳＴ=hello_abc", // TEST in multibytes.
                                "Shift_JIS");
                return true;
            }
        });

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            // add Trigger builder, with file parameter factory
            projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP, "Shift_JIS"));

            projectA.scheduleBuild2(0).get();

            // check triggered builds are correct.
            r.waitUntilNoActivity();
            List<FreeStyleBuild> builds = projectB.getBuilds();
            assertEquals(1, builds.size());

            for (FreeStyleBuild build : builds) {
                EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
                assertEquals("こんにちは", buildEnvVar.get("TEST"));
                assertEquals("hello_abc", buildEnvVar.get("ＴＥＳＴ"));
            }
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testPlatformDefaultEncodedFile(JenkinsRule r) throws Exception {
        // create triggered build, with capture env builder
        Project projectB = r.createFreeStyleProject();
        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);

        // create triggering build
        FreeStyleProject projectA = r.createFreeStyleProject();
        projectA.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("abc.txt")
                        .write(
                                "ＴＥＳＴ=ｈｅｌｌｏ＿ａｂｃ", // TEST=hello_abc in multibytes.
                                null);
                return true;
            }
        });

        // SECURITY-170: need to allow multibyte params that can't be traditionally declared.
        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            // add Trigger builder, with file parameter factory
            projectA.getBuildersList().add(createTriggerBuilder(projectB, NoFilesFoundEnum.SKIP, ""));

            projectA.scheduleBuild2(0).get();

            // check triggered builds are correct.
            r.waitUntilNoActivity();
            List<FreeStyleBuild> builds = projectB.getBuilds();
            assertEquals(1, builds.size());

            // This test explicitly uses the platform's default encoding, which e.g. on Windows is likely to be
            // windows-1250
            // or windows-1252. With these single-byte encodings we cannot expect multi-byte strings to be encoded
            // correctly.
            final boolean isMultiByteDefaultCharset =
                    Charset.defaultCharset().newEncoder().maxBytesPerChar() > 1.0f;
            if (isMultiByteDefaultCharset) {
                for (FreeStyleBuild build : builds) {
                    EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
                    assertEquals("ｈｅｌｌｏ＿ａｂｃ", buildEnvVar.get("ＴＥＳＴ"));
                }
            }
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testDoCheckEncoding(JenkinsRule r) {
        FileBuildParameterFactory.DescriptorImpl d = (FileBuildParameterFactory.DescriptorImpl)
                r.jenkins.getDescriptorOrDie(FileBuildParameterFactory.class);

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
            FileBuildParameterFactory target =
                    new FileBuildParameterFactory("*.properties", null, NoFilesFoundEnum.SKIP);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameterFactory target = new FileBuildParameterFactory("*.properties", "", NoFilesFoundEnum.SKIP);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameterFactory target =
                    new FileBuildParameterFactory("*.properties", "  ", NoFilesFoundEnum.SKIP);
            assertNull(target.getEncoding());
        }
    }
}
