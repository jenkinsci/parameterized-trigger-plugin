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

import hudson.EnvVars;
import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.LabelAxis;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.*;
import hudson.model.labels.LabelExpression;
import hudson.plugins.parameterizedtrigger.*;
import hudson.tasks.Builder;
import hudson.tasks.ArtifactArchiver;
import hudson.util.FormValidation;
import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class FileBuildTriggerConfigTest extends HudsonTestCase {

	public void test() throws Exception {

		Project projectA = createFreeStyleProject("projectA");
		String properties = "KEY=value";
		projectA.setScm(new SingleFileSCM("properties.txt", properties));
		projectA.getPublishersList().add(
				new BuildTrigger(
				new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new FileBuildParameters("properties.txt"))));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
		hudson.rebuildDependencyGraph();

		projectA.scheduleBuild2(0).get();
		hudson.getQueue().getItem(projectB).getFuture().get();

		assertNotNull("builder should record environment", builder.getEnvVars());
		assertEquals("value", builder.getEnvVars().get("KEY"));
	}

	public void test_multiplefiles() throws Exception {

		Project projectA = createFreeStyleProject("projectA");
		projectA.setScm(new ExtractResourceSCM(getClass().getResource("multiple_property_files.zip")));
		projectA.getPublishersList().add(
				new BuildTrigger(
				new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new FileBuildParameters("a_properties.txt,z_properties.txt"))));
		projectA.getPublishersList().add(new ArtifactArchiver("a_properties.txt"));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
		hudson.rebuildDependencyGraph();

		projectA.scheduleBuild2(0).get();
		hudson.getQueue().getItem(projectB).getFuture().get();

		assertNotNull("builder should record environment", builder.getEnvVars());
		// test from first file
		assertEquals("These_three_values_should", builder.getEnvVars().get("A_TEST_01"));
		assertEquals("be_from_file_a_properties_txt", builder.getEnvVars().get("A_TEST_02"));
		assertEquals("which_has_three_definitions", builder.getEnvVars().get("A_TEST_03"));
		// test from second file
		assertEquals("These_two_values_should", builder.getEnvVars().get("Z_TEST_100"));
		assertEquals("be_from_file_z_properties_txt", builder.getEnvVars().get("Z_TEST_101"));

	}

	public void test_failOnMissingFile() throws Exception {

		Project projectA = createFreeStyleProject("projectA");
		projectA.setScm(new ExtractResourceSCM(getClass().getResource("multiple_property_files.zip")));
		projectA.getPublishersList().add(
				new BuildTrigger(
				new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new FileBuildParameters("a_properties.txt,missing_file.txt,z_properties.txt",true))));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
		hudson.rebuildDependencyGraph();

		projectA.scheduleBuild2(0).get();
		waitUntilNoActivity();

		// There should be no builds of projectB as not triggered.
		assertEquals(0, projectB.getBuilds().size());
	}
	
    public void testUtf8File() throws Exception {

        FreeStyleProject projectA = createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n"  // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes("UTF-8")));
        projectA.getPublishersList().add(
                new BuildTrigger(
                new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
                        new FileBuildParameters("properties.txt", "UTF-8", true))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        hudson.getQueue().getItem(projectB).getFuture().get();

        assertNotNull("builder should record environment", builder.getEnvVars());
        assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
        assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
    }

    public void testShiftJISFile() throws Exception {
        // ShiftJIS is an encoding of Japanese texts.
        // I test here that a non-UTF-8 encoding also works.

        FreeStyleProject projectA = createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n"  // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes("Shift_JIS")));
        projectA.getPublishersList().add(
                new BuildTrigger(
                new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
                        new FileBuildParameters("properties.txt", "Shift_JIS", true))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        hudson.getQueue().getItem(projectB).getFuture().get();

        assertNotNull("builder should record environment", builder.getEnvVars());
        assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
        assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
    }
    
    public void testPlatformDefaultEncodedFile() throws Exception {
        // ShiftJIS is an encoding of Japanese texts.
        // I test here that a non-UTF-8 encoding also works.

        FreeStyleProject projectA = createFreeStyleProject("projectA");
        String properties = "KEY=こんにちは\n"  // "hello" in Japanese.
                + "ＫＥＹ=value"; // "KEY" in multibytes.
        projectA.setScm(new SingleFileSCM("properties.txt", properties.getBytes()));
        projectA.getPublishersList().add(
                new BuildTrigger(
                new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
                        new FileBuildParameters("properties.txt"))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        FreeStyleProject projectB = createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        hudson.getQueue().getItem(projectB).getFuture().get();

        assertNotNull("builder should record environment", builder.getEnvVars());
        assertEquals("こんにちは", builder.getEnvVars().get("KEY"));
        assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
    }

    public void testDoCheckEncoding() throws Exception {
        FileBuildParameters.DescriptorImpl d
            = (FileBuildParameters.DescriptorImpl)jenkins.getDescriptorOrDie(FileBuildParameters.class);
        
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding(null).kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("  ").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("UTF-8").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding("Shift_JIS").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckEncoding(" UTF-8 ").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckEncoding("NoSuchEncoding").kind);
    }
    
    public void testNullifyEncoding() throws Exception {
        // to use default encoding, encoding must be null.
        {
            FileBuildParameters target
                = new FileBuildParameters("*.properties", null, false);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameters target
                = new FileBuildParameters("*.properties", "", false);
            assertNull(target.getEncoding());
        }
        {
            FileBuildParameters target
                = new FileBuildParameters("*.properties", "  ", false);
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
                throws InterruptedException, IOException
        {
            EnvVars env = build.getEnvironment(listener);
            env.putAll(build.getBuildVariables());
            build.getWorkspace().child(filename).write(env.expand(content), encoding);
            return true;
        }
    }
    
    private static String getStringParameterValue(AbstractBuild<?,?> build, String name) {
        ParametersAction action = build.getAction(ParametersAction.class);
        if (action == null) {
            return null;
        }
        ParameterValue v = action.getParameter(name);
        if (v == null || !(v instanceof StringParameterValue)) {
            return null;
        }
        return ((StringParameterValue)v).value;
    }
    
    public void testMatrixBuildsOnSameNodes() throws Exception {
        // all builds runs on master.
        // upstream matrix projects creates properties files in each builds.
        MatrixProject upstream = createMatrixProject();
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2")));
        WriteFileBuilder wfb = new WriteFileBuilder("properties.txt", "triggered_${childname}=true");
        
        FreeStyleProject downstream = createFreeStyleProject();
        
        // Without useMatrixBuild, publisher
        // Downstream project is triggered without parameters.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            
            upstream.getPublishersList().clear();
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, false, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getBuildersList().add(new TriggerBuilder(
                    new BlockableBuildTriggerConfig(downstream.getFullName(), null, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, false, null, false)
                    ))
            ));
            
            upstream.getPublishersList().clear();
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getBuildersList().add(new TriggerBuilder(
                    new BlockableBuildTriggerConfig(downstream.getFullName(), null, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            upstream.getPublishersList().clear();
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
    
    public void testMatrixBuildsOnOtherNodes() throws Exception {
        // each builds run on other nodes.
        // upstream matrix projects creates properties files in each builds.
        createOnlineSlave(LabelExpression.parseExpression("child1"));
        createOnlineSlave(LabelExpression.parseExpression("child2"));
        
        MatrixProject upstream = createMatrixProject();
        upstream.setAxes(new AxisList(new LabelAxis("childname", Arrays.asList("child1", "child2"))));
        WriteFileBuilder wfb = new WriteFileBuilder("properties.txt", "triggered_${childname}=true");
        
        FreeStyleProject downstream = createFreeStyleProject();
        
        // Without useMatrixBuild, publisher
        // Downstream project is triggered without parameters.
        {
            upstream.getBuildersList().clear();
            upstream.getBuildersList().add(wfb);
            
            upstream.getPublishersList().clear();
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, false, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getBuildersList().add(new TriggerBuilder(
                    new BlockableBuildTriggerConfig(downstream.getFullName(), null, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, false, null, false)
                    ))
            ));
            
            upstream.getPublishersList().clear();
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getBuildersList().add(new TriggerBuilder(
                    new BlockableBuildTriggerConfig(downstream.getFullName(), null, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            upstream.getPublishersList().clear();
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
    
    public void testMatrixBuildsCombinationFilter() throws Exception {
        MatrixProject upstream = createMatrixProject();
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2", "child3")));
        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "triggered_${childname}=true"));
        
        FreeStyleProject downstream = createFreeStyleProject();
        
        // without combinationFilter
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, "childname!='child2'", false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }
    }
    
    public void testMatrixBuildsOnlyExactRuns() throws Exception {
        MatrixProject upstream = createMatrixProject();
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child2", "child3")));
        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "triggered_${childname}=true"));
        
        FreeStyleProject downstream = createFreeStyleProject();
        
        // Run build.
        // builds of child1, child2, child3 is created.
        upstream.scheduleBuild2(0).get();
        
        // child2 is dropped
        upstream.setAxes(new AxisList(new TextAxis("childname", "child1", "child3")));
        
        // without onlyExactRuns
        {
            upstream.getPublishersList().clear();
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, false)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
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
            upstream.getPublishersList().add(new BuildTrigger(
                    new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                            new FileBuildParameters("properties.txt", null, false, true, null, true)
                    ))
            ));
            
            jenkins.rebuildDependencyGraph();
            
            assertEquals(0, downstream.getBuilds().size());
            
            upstream.scheduleBuild2(0).get();
            waitUntilNoActivity();
            
            assertEquals(1, downstream.getBuilds().size());
            FreeStyleBuild build = downstream.getLastBuild();
            assertEquals("true", getStringParameterValue(build, "triggered_child1"));
            assertNull(getStringParameterValue(build, "triggered_child2"));
            assertEquals("true", getStringParameterValue(build, "triggered_child3"));
            build.delete();
        }
    }
    
    @Bug(22705)
    public void testMatrixBuildsConfiguration() throws Exception {
        FreeStyleProject downstream = createFreeStyleProject();
        
        MatrixProject upstream = createMatrixProject();
        upstream.setAxes(new AxisList(new TextAxis("axis1", "value1", "value2")));
        upstream.getPublishersList().add(new BuildTrigger(
                new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                        new FileBuildParameters("properties.txt", "UTF-8", true, true, "axis1=value1", true)
                ))
        ));
        upstream.save();
        
        String upstreamName = upstream.getFullName();
        
        WebClient wc = createWebClient();
        submit(wc.getPage(upstream, "configure").getFormByName("config"));
        
        upstream = jenkins.getItemByFullName(upstreamName, MatrixProject.class);
        assertNotNull(upstream);
        
        BuildTrigger trigger = upstream.getPublishersList().get(BuildTrigger.class);
        assertNotNull(trigger);
        
        assertEquals(1, trigger.getConfigs().size());
        
        BuildTriggerConfig config = trigger.getConfigs().get(0);
        
        assertEquals(1, config.getConfigs().size());
        FileBuildParameters p = (FileBuildParameters)config.getConfigs().get(0);
        assertEquals("properties.txt", p.getPropertiesFile());
        assertEquals("UTF-8", p.getEncoding());
        assertTrue(p.getFailTriggerOnMissing());
        assertTrue(p.isUseMatrixChild());
        assertEquals("axis1=value1", p.getCombinationFilter());
        assertTrue(p.isOnlyExactRuns());
    }
    
    public void testAbsolutePath() throws Exception {
        FreeStyleProject downstream = createFreeStyleProject();
        
        FreeStyleProject upstream = createFreeStyleProject();
        
        File absoluteFile = new File(jenkins.getRootDir(), "properties.txt");
        if(!absoluteFile.getParentFile().exists()) {
            FileUtils.forceMkdir(absoluteFile.getParentFile());
        }
        FileUtils.writeStringToFile(absoluteFile, "absolute_param=value1");
        
        File workspace = new File(jenkins.getWorkspaceFor(upstream).getRemote());
        File relativeDir = workspace.getParentFile();
        
        if(!relativeDir.exists()) {
            FileUtils.forceMkdir(relativeDir);
        }
        File relativeFile = new File(relativeDir, "properties.txt");
        FileUtils.writeStringToFile(relativeFile, "relative_param1=value2");
        
        upstream.getBuildersList().add(new WriteFileBuilder("properties.txt", "relative_param2=value3"));
        upstream.getPublishersList().add(new BuildTrigger(
                new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                        new FileBuildParameters(String.format("%s,../properties.txt,properties.txt", absoluteFile.getAbsolutePath()))
                ))
        ));
        
        jenkins.rebuildDependencyGraph();
        
        assertBuildStatusSuccess(upstream.scheduleBuild2(0));
        waitUntilNoActivity();
        
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
                throws InterruptedException, IOException
        {
            build.getWorkspace().deleteRecursive();
            return true;
        }
    }
    
    @Bug(22229)
    public void testAbsolutePathWithoutWorkspace() throws Exception {
        // Though it is rather a problem with ws-cleanup-plugin, 
        // there's a case a workspace is removed.
        FreeStyleProject downstream = createFreeStyleProject();
        
        FreeStyleProject upstream = createFreeStyleProject();
        
        File absoluteFile = new File(jenkins.getRootDir(), "properties.txt");
        if(!absoluteFile.getParentFile().exists()) {
            FileUtils.forceMkdir(absoluteFile.getParentFile());
        }
        FileUtils.writeStringToFile(absoluteFile, "absolute_param=value1");
        
        upstream.getBuildersList().add(new WorkspaceRemoveBuilder());
        upstream.getPublishersList().add(new BuildTrigger(
                new BuildTriggerConfig(downstream.getFullName(), ResultCondition.SUCCESS, true, false, Arrays.<AbstractBuildParameters>asList(
                        new FileBuildParameters(absoluteFile.getAbsolutePath())
                ))
        ));
        
        jenkins.rebuildDependencyGraph();
        
        assertBuildStatusSuccess(upstream.scheduleBuild2(0));
        waitUntilNoActivity();
        
        FreeStyleBuild build = downstream.getLastBuild();
        assertNotNull(build);
        assertEquals("value1", getStringParameterValue(build, "absolute_param"));
    }
}
