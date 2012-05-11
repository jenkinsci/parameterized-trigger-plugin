/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Tom Huybrechts
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
import hudson.model.Queue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test of scripting the triggering.
 *
 */
public class ScriptTest extends HudsonTestCase {

    public void testTriggerByScript_onSuccess_scriptReturnsTrue() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS, "true");
        assertEquals(1, projectB.getLastBuild().getNumber());
    }

    public void testTriggerByScript_onSuccess_scriptReturnsFalse() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS, "false");
        assertNull(projectB.getLastBuild());
    }

    public void testTriggerByScript_onFailure_scriptReturnsTrue() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new FailureBuilder());
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS, "true");
        assertNull(projectB.getLastBuild());
    }

    public void testTriggerByScript_onFailure_scriptReturnsFalse() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new FailureBuilder());
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS, "false");
        assertNull(projectB.getLastBuild());
    }

    private void schedule(Project projectA, Project projectB, ResultCondition condition, String script)
            throws IOException, InterruptedException, ExecutionException {
        projectA.getPublishersList().replace(new BuildTrigger(new BuildTriggerConfig("projectB", condition, script, new PredefinedBuildParameters(""))));
        hudson.rebuildDependencyGraph();
        projectA.scheduleBuild2(0).get();
        Queue.Item q = hudson.getQueue().getItem(projectB);
        if (q != null) q.getFuture().get();
    }
    
}
