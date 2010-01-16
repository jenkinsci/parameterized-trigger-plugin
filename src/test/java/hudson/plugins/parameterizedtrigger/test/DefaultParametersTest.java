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
import hudson.model.Cause.UserCause;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import java.util.Arrays;

import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

public class DefaultParametersTest extends HudsonTestCase {

	public void test() throws Exception {

		Project projectA = createFreeStyleProject("projectA");
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new CurrentBuildParameters())));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = createFreeStyleProject("projectB");
		projectB.addProperty(new ParametersDefinitionProperty(
				new StringParameterDefinition("key1", "value1"),
				new StringParameterDefinition("key2", "value2")
				));
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
		hudson.rebuildDependencyGraph();

		String log = getLog((Run)projectA.scheduleBuild2(0, new UserCause(),
			new ParametersAction(new StringParameterValue("KEY3", "value3"))).get());
		Queue.Item q = hudson.getQueue().getItem(projectB);
		assertNotNull("projectB should be triggered: " + log, q);
		q.getFuture().get();
		assertNotNull("builder should record environment", builder.getEnvVars());
		assertEquals("value1", builder.getEnvVars().get("KEY1"));
		assertEquals("value2", builder.getEnvVars().get("KEY2"));
		assertEquals("value3", builder.getEnvVars().get("KEY3"));

		projectA.scheduleBuild2(0, new UserCause(), new ParametersAction(new StringParameterValue("key1", "value3"))).get();
		hudson.getQueue().getItem(projectB).getFuture().get();
		assertEquals("value3", builder.getEnvVars().get("KEY1"));
		assertEquals("value2", builder.getEnvVars().get("KEY2"));
	}

    public void testMergeParameters() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        Project projectB = createFreeStyleProject("projectB");
        //    projectB defaults: FOO=bar  BAR=override-me
        // Invoke projectA with:          BAR=foo  BAZ=override-me
        //  Merge in predefined:                   BAZ=moo  HOHO=blah
        //      Expected result: FOO=bar  BAR=foo  BAZ=moo  HOHO=blah
        projectB.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("FOO", "bar"),
                new StringParameterDefinition("BAR", "override-me")));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        projectA.getPublishersList().add(new BuildTrigger(
                new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
                    new CurrentBuildParameters(),
                    new PredefinedBuildParameters("BAZ=moo\nHOHO=blah"))));
        hudson.rebuildDependencyGraph();
        Run r = (Run)projectA.scheduleBuild2(0, new UserCause(), new ParametersAction(
                new StringParameterValue("BAR", "foo"),
                new StringParameterValue("BAZ", "override-me"))).get();
        Queue.Item q = hudson.getQueue().getItem(projectB);
        assertNotNull("projectB should be triggered: " + getLog(r), q);
        r = (Run)q.getFuture().get();
        assertEquals("should be exactly one ParametersAction", 1,
                     r.getActions(ParametersAction.class).size());
        EnvVars envVars = builder.getEnvVars();
        assertNotNull("builder should record environment", envVars);
        assertEquals("FOO", "bar", envVars.get("FOO"));
        assertEquals("BAR", "foo", envVars.get("BAR"));
        assertEquals("BAZ", "moo", envVars.get("BAZ"));
        assertEquals("HOHO", "blah", envVars.get("HOHO"));
    }
}
