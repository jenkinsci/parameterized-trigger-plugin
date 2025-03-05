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

import hudson.model.Cause.UserIdCause;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CurrentBuildParametersTest {

    @Test
    void test(JenkinsRule r) throws Exception {
        Project<?, ?> projectA = r.createFreeStyleProject("projectA");
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("KEY", "key"));
        projectA.addProperty(new ParametersDefinitionProperty(definition));
        projectA.getPublishersList()
                .add(new BuildTrigger(
                        new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new CurrentBuildParameters())));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause(), new ParametersAction(new StringParameterValue("KEY", "value")))
                .get();
        r.jenkins.getQueue().getItem(projectB).getFuture().get();

        assertNotNull(builder.getEnvVars(), "builder should record environment");
        assertEquals("value", builder.getEnvVars().get("KEY"));

        // Now rename projectB and confirm projectA's build trigger is updated automatically:
        projectB.renameTo("new-projectB");
        assertEquals(
                "new-projectB",
                projectA.getPublishersList()
                        .get(BuildTrigger.class)
                        .getConfigs()
                        .get(0)
                        .getProjects(),
                "rename in trigger");

        // Now delete projectB and confirm projectA's build trigger is updated automatically:
        projectB.delete();
        assertNull(projectA.getPublishersList().get(BuildTrigger.class), "now-empty trigger should be removed");
    }

    /**
     * Project A: Post-build build-trigger
     * 			  + currentBuildParameters
     *			  + no parameters defined
     *			  + Trigger Build without parameters = false
     *	=> Project B should NOT be triggered
     *
     * @throws Exception
     */
    @Test
    void testPostBuildTriggerNoParametersWithoutParametersFalse(JenkinsRule r) throws Exception {
        testPostBuildTriggerNoParameters(r, false);
    }

    /**
     * Project A: Post-build build-trigger
     * 			  + currentBuildParameters
     *			  + no parameters defined
     *			  + Trigger Build without parameters = true
     *	=> Project B should be triggered
     *
     * @throws Exception
     */
    @Test
    void testPostBuildTriggerNoParametersWithoutParametersTrue(JenkinsRule r) throws Exception {
        testPostBuildTriggerNoParameters(r, true);
    }

    public void testPostBuildTriggerNoParameters(JenkinsRule r, boolean pWithoutParameters) throws Exception {
        Project<?, ?> projectA = r.createFreeStyleProject("projectA");
        List<AbstractBuildParameters> buildParameters = new ArrayList<>();
        buildParameters.add(new CurrentBuildParameters());
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig(
                        "projectB", ResultCondition.SUCCESS, pWithoutParameters, buildParameters)));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();

        Project<?, ?> projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        assertEquals(pWithoutParameters, r.jenkins.getQueue().contains(projectB));
        List<String> log = projectA.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
    }

    /**
     * Project A: Build step trigger build
     * 			  + currentBuildParameters
     * 			  + no parameters defined
     *  => Project B should be triggered
     *
     * @throws Exception
     */
    @Test
    void testBuildStepTriggerBuildNoParameters(JenkinsRule r) throws Exception {
        Project<?, ?> projectA = r.createFreeStyleProject("projectA");
        List<AbstractBuildParameters> buildParameters = new ArrayList<>();
        buildParameters.add(new CurrentBuildParameters());
        projectA.getBuildersList()
                .add(new TriggerBuilder(new BlockableBuildTriggerConfig("projectB", null, buildParameters)));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();

        Project<?, ?> projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        assertTrue(r.jenkins.getQueue().contains(projectB));
        List<String> log = projectA.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
    }
}
