/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Geoff Cummings
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

import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.RunParameterConfig;
import hudson.plugins.parameterizedtrigger.RunParameters;

import java.util.ArrayList;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

public class RunParametersTest extends HudsonTestCase {

    public void test() throws Exception {

        Project projectA = createFreeStyleProject("projectA");

        List<RunParameterConfig> configs = new ArrayList<RunParameterConfig>();
        configs.add(new RunParameterConfig("RUN_PARAM", "projectA", "1"));

        projectA.getPublishersList().add(
                new BuildTrigger(
                        new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new RunParameters(configs))));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        Project projectB = createFreeStyleProject("projectB");
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(1);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        hudson.getQueue().getItem(projectB).getFuture().get();

        assertNotNull("builder should record environment", builder.getEnvVars());

        // Would prefer to check for RUN_PARAM_JOBNAME and RUN_PARAM_NUMBER
        // but these were not added until Jenkins 1.503
        assertEquals("projectA", builder.getEnvVars().get("RUN_PARAM.jobName"));
        assertEquals("1", builder.getEnvVars().get("RUN_PARAM.number"));

    }
}
