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

import com.google.common.collect.ImmutableList;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Cause.UserCause;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;

import java.util.Collections;
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
        AbstractBuild<?, ?> buildA1 = projectA.scheduleBuild2(0, new UserCause()).get();

        EnvVars envVars = builder.getEnvVars();
        System.out.println("envVars: " + envVars);
        
        assertThat(envVars, notNullValue());
        assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", "projectB"));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB", Integer.toString(expectedBuildNumber)));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB_RUN_1", Integer.toString(expectedBuildNumber)));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RESULT_projectB", buildA1.getResult().toString()));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RESULT_projectB_RUN_1", buildA1.getResult().toString()));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RUN_COUNT_projectB", "1"));
        assertThat(envVars, hasEntry("TRIGGERED_JOB_NAMES", "projectB"));

        // The below test for expectedBuildNumber is meaningless if the
        // value doesn't update, though it should always update.
        assertThat(projectB.getNextBuildNumber(), is(not(expectedBuildNumber)));

        expectedBuildNumber = projectB.getNextBuildNumber();
        AbstractBuild<?, ?> buildA2 = projectA.scheduleBuild2(0, new UserCause()).get();
        envVars = builder.getEnvVars();

        assertThat(envVars, notNullValue());
        assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", "projectB"));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB", Integer.toString(expectedBuildNumber)));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB_RUN_1", Integer.toString(expectedBuildNumber)));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RESULT_projectB", buildA2.getResult().toString()));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RESULT_projectB_RUN_1", buildA2.getResult().toString()));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_RUN_COUNT_projectB", "1"));
        assertThat(envVars, hasEntry("TRIGGERED_JOB_NAMES", "projectB"));

    }
    
    public void test_oddchars() throws Exception {
        Project<?,?> projectA = createFreeStyleProject("projectA");
        List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());
        BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");

        String testName = "odd£()+}{-=~chars-10";
        String testNameResult = "odd_chars_10";
        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig(testName, neverFail, buildParameters);
        projectA.getBuildersList().add(new TriggerBuilder(config));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectA.getBuildersList().add(builder);

        Project projectB = createFreeStyleProject(testName);
        projectB.setQuietPeriod(0);
        hudson.rebuildDependencyGraph();
        // Just to make sure they differ from projectA's build numbers.
        projectB.updateNextBuildNumber(3);

        int expectedBuildNumber = projectB.getNextBuildNumber();
        projectA.scheduleBuild2(0, new UserCause()).get();

        EnvVars envVars = builder.getEnvVars();
        System.out.println("envVars: " + envVars);
        
        assertThat(envVars, notNullValue());
        assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", testNameResult));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_"+testNameResult, Integer.toString(expectedBuildNumber)));

        // The below test for expectedBuildNumber is meaningless if the
        // value doesn't update, though it should always update.
        assertThat(projectB.getNextBuildNumber(), is(not(expectedBuildNumber)));

        expectedBuildNumber = projectB.getNextBuildNumber();
        projectA.scheduleBuild2(0, new UserCause()).get();
        envVars = builder.getEnvVars();

        assertThat(envVars, notNullValue());
        assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", testNameResult));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_"+testNameResult, Integer.toString(expectedBuildNumber)));
    }
    
    public void test_multipletriggers() throws Exception {
        String testNameResult = "projectB";
        String testNameResult2 = "projectC";
        int buildsToTest = 3;
        
        Project<?,?> projectA = createFreeStyleProject();
        Project projectB = createFreeStyleProject(testNameResult);
        Project projectC = createFreeStyleProject(testNameResult2);
        projectA.getBuildersList().add(
            new TriggerBuilder(
            new BlockableBuildTriggerConfig(testNameResult +"," + testNameResult2,
                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                    ImmutableList.<AbstractBuildParameterFactory>of(new CounterBuildParameterFactory("0",Integer.toString(buildsToTest-1),"1", "TEST=COUNT$COUNT")),
                    Collections.<AbstractBuildParameters>emptyList())));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectA.getBuildersList().add(builder);                                

        projectB.setQuietPeriod(0);
        projectB.updateNextBuildNumber(3);
        
        projectC.setQuietPeriod(0);
        projectC.updateNextBuildNumber(12);
        hudson.rebuildDependencyGraph();
        
        int firstExpectedBuildNumberB = projectB.getNextBuildNumber();
        int firstExpectedBuildNumberC = projectC.getNextBuildNumber();

        projectA.scheduleBuild2(0, new UserCause()).get();
        waitUntilNoActivity();

        EnvVars envVars = builder.getEnvVars();
        System.out.println("envVars: " + envVars);
        
        assertEquals(buildsToTest, projectB.getBuilds().size());
        assertEquals(buildsToTest, projectC.getBuilds().size());

        String allBuildNumbersB = "";
        for (int run = 1, buildNumber = firstExpectedBuildNumberB; run <= buildsToTest; run ++, buildNumber++) {
            if (allBuildNumbersB.length() > 0) {
                allBuildNumbersB += ",";
            }
            allBuildNumbersB += buildNumber;
            assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectB_RUN_" + run, Integer.toString(buildNumber)));
        }

        String allBuildNumbersC = "";
        for (int run = 1, buildNumber = firstExpectedBuildNumberC; run <= buildsToTest; run ++, buildNumber++) {
            if (allBuildNumbersC.length() > 0) {
                allBuildNumbersC += ",";
            }
            allBuildNumbersC += buildNumber;
            assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_projectC_RUN_" + run, Integer.toString(buildNumber)));
        }

        int lastBuildNumberB = firstExpectedBuildNumberB + (buildsToTest-1);
        int lastBuildNumberC = firstExpectedBuildNumberC + (buildsToTest-1);

        assertThat(envVars, notNullValue());
        assertThat(envVars, hasEntry("LAST_TRIGGERED_JOB_NAME", testNameResult2));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_"+testNameResult, Integer.toString(lastBuildNumberB)));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBER_"+testNameResult2, Integer.toString(lastBuildNumberC)));
        assertThat(envVars, hasEntry("TRIGGERED_JOB_NAMES", testNameResult + "," + testNameResult2));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBERS_"+testNameResult, allBuildNumbersB));
        assertThat(envVars, hasEntry("TRIGGERED_BUILD_NUMBERS_"+testNameResult2, allBuildNumbersC));

    }
}
