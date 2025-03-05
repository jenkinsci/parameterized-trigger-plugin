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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.EnvVars;
import hudson.model.Cause.UserIdCause;
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
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DefaultParametersTest {

    @Test
    void test(JenkinsRule r) throws Exception {

        Project projectA = r.createFreeStyleProject("projectA");
        projectA.getPublishersList()
                .add(new BuildTrigger(
                        new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new CurrentBuildParameters())));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("key1", "value1"), new StringParameterDefinition("key2", "value2")));
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        try {
            // SECURITY-170: this is needed for tests down the line.
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            String log = JenkinsRule.getLog((Run) projectA.scheduleBuild2(
                            0, new UserIdCause(), new ParametersAction(new StringParameterValue("KEY3", "value3")))
                    .get());
            Queue.Item q = r.jenkins.getQueue().getItem(projectB);
            assertNotNull(q, "projectB should be triggered: " + log);
            q.getFuture().get();
            assertNotNull(builder.getEnvVars(), "builder should record environment");
            assertEquals("value1", builder.getEnvVars().get("KEY1"));
            assertEquals("value2", builder.getEnvVars().get("KEY2"));
            assertEquals("value3", builder.getEnvVars().get("KEY3"));

            projectA.scheduleBuild2(
                            0, new UserIdCause(), new ParametersAction(new StringParameterValue("key1", "value3")))
                    .get();
            r.jenkins.getQueue().getItem(projectB).getFuture().get();
            assertEquals("value3", builder.getEnvVars().get("KEY1"));
            assertEquals("value2", builder.getEnvVars().get("KEY2"));
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }

    @Test
    void testMergeParameters(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        Project projectB = r.createFreeStyleProject("projectB");
        //    projectB defaults: FOO=bar  BAR=override-me
        // Invoke projectA with:          BAR=foo  BAZ=override-me
        //  Merge in predefined:                   BAZ=moo  HOHO=blah
        //      Expected result: FOO=bar  BAR=foo  BAZ=moo  HOHO=blah
        projectB.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("FOO", "bar"), new StringParameterDefinition("BAR", "override-me")));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB",
                        ResultCondition.SUCCESS,
                        new CurrentBuildParameters(),
                        new PredefinedBuildParameters("BAZ=moo\nHOHO=blah"))));
        r.jenkins.rebuildDependencyGraph();

        try {
            // System.setProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME, "true");
            System.setProperty("hudson.model.ParametersAction.keepUndefinedParameters", "true");

            Run run = (Run) projectA.scheduleBuild2(
                            0,
                            new UserIdCause(),
                            new ParametersAction(
                                    new StringParameterValue("BAR", "foo"),
                                    new StringParameterValue("BAZ", "override-me")))
                    .get();
            Queue.Item q = r.jenkins.getQueue().getItem(projectB);
            assertNotNull(q, "projectB should be triggered: " + JenkinsRule.getLog(run));
            run = (Run) q.getFuture().get();
            assertEquals(1, run.getActions(ParametersAction.class).size(), "should be exactly one ParametersAction");
            EnvVars envVars = builder.getEnvVars();
            assertNotNull(envVars, "builder should record environment");
            assertEquals("bar", envVars.get("FOO"), "FOO");
            assertEquals("foo", envVars.get("BAR"), "BAR");
            assertEquals("moo", envVars.get("BAZ"), "BAZ");
            assertEquals("blah", envVars.get("HOHO"), "HOHO");
        } finally {
            // System.clearProperty(ParametersAction.KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME);
            System.clearProperty("hudson.model.ParametersAction.keepUndefinedParameters");
        }
    }
}
