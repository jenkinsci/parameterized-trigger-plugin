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

import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;

import java.util.Collections;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;
import com.google.common.collect.ImmutableList;
import hudson.model.Run;
import java.io.IOException;

public class TriggerBuilderTest extends HudsonTestCase {

    private BlockableBuildTriggerConfig createTriggerConfig(String projects) {
        return new BlockableBuildTriggerConfig(projects, new BlockingBehaviour("never", "never", "never"), null);
    }

    public void testOrderOfLogEntries() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");
        createFreeStyleProject("project4");
        createFreeStyleProject("project5");
        createFreeStyleProject("project6");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project2"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project3"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project4"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project5"));
        triggerBuilder.getConfigs().add(createTriggerConfig("project6"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "project1 #1 completed. Result was SUCCESS",
                "project2 #1 completed. Result was SUCCESS",
                "project3 #1 completed. Result was SUCCESS",
                "project4 #1 completed. Result was SUCCESS",
                "project5 #1 completed. Result was SUCCESS",
                "project6 #1 completed. Result was SUCCESS");
        
    }
    
    public void testWaitingForCompletion() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Waiting for the completion of project2",
                "Waiting for the completion of project3");
    }
    
    public void testNonBlockingTrigger() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1, project2, project3", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Triggering projects: project1, project2, project3");
    }

    public void testConsoleOutputWithCounterParameters() throws Exception{
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");
        
        Project<?,?> triggerProject = createFreeStyleProject();
        
        BlockingBehaviour blockingBehaviour = new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE);
        ImmutableList<AbstractBuildParameterFactory> buildParameter = ImmutableList.<AbstractBuildParameterFactory>of(new CounterBuildParameterFactory("0","2","1", "TEST=COUNT$COUNT"));
        List<AbstractBuildParameters> emptyList = Collections.<AbstractBuildParameters>emptyList();
        
        BlockableBuildTriggerConfig bBTConfig = new BlockableBuildTriggerConfig("project1, project2, project3", blockingBehaviour, buildParameter, emptyList);
        
        triggerProject.getBuildersList().add(new TriggerBuilder(bBTConfig));

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "project1 #1 completed. Result was SUCCESS",
                "project1 #2 completed. Result was SUCCESS",
                "project1 #3 completed. Result was SUCCESS",
                "project2 #1 completed. Result was SUCCESS",
                "project2 #2 completed. Result was SUCCESS",
                "project2 #3 completed. Result was SUCCESS",
                "project3 #1 completed. Result was SUCCESS",
                "project3 #2 completed. Result was SUCCESS",
                "project3 #3 completed. Result was SUCCESS");
    }

    
    public void testBlockingTriggerWithDisabledProjects() throws Exception {
        createFreeStyleProject("project1");
        Project<?, ?> p2 = createFreeStyleProject("project2");
        p2.disable();
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0).get();

        assertLines(triggerProject.getLastBuild(),
                "Waiting for the completion of project1",
                "Skipping project2. The project is either disabled or the configuration has not been saved yet.",
                "Waiting for the completion of project3");
      
    }

    private void assertLines(Run<?,?> build, String... lines) throws IOException {
        List<String> log = build.getLog(Integer.MAX_VALUE);
        List<String> rest = log;
        for (String line : lines) {
            int where = rest.indexOf(line);
            assertFalse("Could not find line '" + line + "' among remaining log lines " + rest, where == -1);
            rest = rest.subList(where + 1, rest.size());
        }
    }

}
