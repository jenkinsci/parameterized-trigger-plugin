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

import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.util.FormValidation;

import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ExtractResourceSCM;

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
}
