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

import hudson.model.FreeStyleBuild;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

public class PredefinedPropertiesBuildTriggerConfigTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
	public void test() throws Exception {

		Project projectA = r.createFreeStyleProject("projectA");
		String properties = "KEY=value";
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
						new PredefinedBuildParameters(properties))));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = r.createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<ParameterDefinition>();
        definition.add(new StringParameterDefinition("KEY","key"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
		r.jenkins.rebuildDependencyGraph();

		projectA.scheduleBuild2(0).get();
		r.jenkins.getQueue().getItem(projectB).getFuture().get();

		assertNotNull("builder should record environment", builder.getEnvVars());
		assertEquals("value", builder.getEnvVars().get("KEY"));
	}
	
    @Test
    public void testNonAscii() throws Exception {

        Project projectA = r.createFreeStyleProject("projectA");
        String properties = "KEY=１２３\n" // 123 in multibytes
                + "ＫＥＹ=value\n";    // "KEY" in multibytes
        projectA.getPublishersList().add(
                new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS,
                        new PredefinedBuildParameters(properties))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<ParameterDefinition>();
        definition.add(new StringParameterDefinition("KEY","key"));
        definition.add(new StringParameterDefinition("ＫＥＹ","otherkey"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        r.jenkins.getQueue().getItem(projectB).getFuture().get();

        assertNotNull("builder should record environment", builder.getEnvVars());
        assertEquals("１２３", builder.getEnvVars().get("KEY"));
        assertEquals("value", builder.getEnvVars().get("ＫＥＹ"));
    }
    
    @Test
    @Issue("SECURITY-101")
    public void ensureTextBasedParameterAreCorrectlyConvertedToPassword() throws Exception {
        // creation
        Project parent = r.createFreeStyleProject("parent");
        Project child = r.createFreeStyleProject("child");
        
        {// configuration on parent
            String properties = "login=derp\n" +
                    "pwd=d3rp\n";
            
            parent.getPublishersList().add(
                    new BuildTrigger(
                            new BuildTriggerConfig(
                                    child.getName(), ResultCondition.SUCCESS,
                                    new PredefinedBuildParameters(properties)
                            )
                    )
            );
        }
        
        {// configuration of child
            child.setQuietPeriod(1);
            
            child.addProperty(new ParametersDefinitionProperty(
                    new StringParameterDefinition("login", "default-login", null),
                    new PasswordParameterDefinition("pwd", "default-pwd", null)
            ));
        }
        
        r.jenkins.rebuildDependencyGraph();
        
        // run of parent
        parent.scheduleBuild2(0).get();
        FreeStyleBuild childLastBuild = (FreeStyleBuild) r.jenkins.getQueue().getItem(child).getFuture().get();
        
        List<ParametersAction> actions = childLastBuild.getActions(ParametersAction.class);
        assertFalse(actions.isEmpty());
        ParametersAction pa = actions.get(0);
        assertEquals(StringParameterValue.class, pa.getParameter("login").getClass());
        assertEquals(PasswordParameterValue.class, pa.getParameter("pwd").getClass());
    }
}
