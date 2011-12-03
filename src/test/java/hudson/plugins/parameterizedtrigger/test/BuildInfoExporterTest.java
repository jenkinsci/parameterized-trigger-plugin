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
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserCause;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

public class BuildInfoExporterTest extends HudsonTestCase {

	public void test() throws Exception {
		Project<?,?> projectA = createFreeStyleProject("projectA");
		List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
		buildParameters.add(new CurrentBuildParameters());
                BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");
                BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("projectB", neverFail, buildParameters);
		projectA.getBuildersList().add(new TriggerBuilder(config));

		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		projectA.getBuildersList().add(builder);

		Project projectB = createFreeStyleProject("projectB");
		projectB.setQuietPeriod(0);
		hudson.rebuildDependencyGraph();

                // Just to make sure they differ from projectA's build numbers.
                projectB.updateNextBuildNumber(3);

                int expectedBuildNumber = projectB.getNextBuildNumber();
		projectA.scheduleBuild2(0, new UserCause()).get();

                EnvVars envVars = builder.getEnvVars();
                assertThat(envVars, notNullValue());
                assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", "projectB"));
                assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB", Integer.toString(expectedBuildNumber)));

                // The below test for expectedBuildNumber is meaningless if the
                // value doesn't update, though it should always update.
                assertThat(projectB.getNextBuildNumber(), is(not(expectedBuildNumber)));

                expectedBuildNumber = projectB.getNextBuildNumber();
		projectA.scheduleBuild2(0, new UserCause()).get();
                envVars = builder.getEnvVars();

                assertThat(envVars, notNullValue());
                assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", "projectB"));
                assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB", Integer.toString(expectedBuildNumber)));
	}
}
