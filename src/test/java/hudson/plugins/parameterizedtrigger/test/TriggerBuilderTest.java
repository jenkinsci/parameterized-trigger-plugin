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

import hudson.model.Cause.UserCause;
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

        triggerProject.scheduleBuild2(0, new UserCause()).get();

        List<String> log = triggerProject.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
        assertEquals("project1 #1 completed. Result was SUCCESS", log.get(2));
        assertEquals("project2 #1 completed. Result was SUCCESS", log.get(4));
        assertEquals("project3 #1 completed. Result was SUCCESS", log.get(6));
        assertEquals("project4 #1 completed. Result was SUCCESS", log.get(8));
        assertEquals("project5 #1 completed. Result was SUCCESS", log.get(10));
        assertEquals("project6 #1 completed. Result was SUCCESS", log.get(12));
        
    }
    
    public void testWaitingForCompletion() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0, new UserCause()).get();

        List<String> log = triggerProject.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
        assertEquals("Waiting for the completion of project1", log.get(1));
        assertEquals("Waiting for the completion of project2", log.get(3));
        assertEquals("Waiting for the completion of project3", log.get(5));
    }
    
    public void testNonBlockingTrigger() throws Exception {
        createFreeStyleProject("project1");
        createFreeStyleProject("project2");
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        BlockableBuildTriggerConfig config = new BlockableBuildTriggerConfig("project1, project2, project3", null, null);
        TriggerBuilder triggerBuilder = new TriggerBuilder(config);

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0, new UserCause()).get();

        List<String> log = triggerProject.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
        
        assertEquals("Triggering projects: project1, project2, project3", log.get(1));
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

        triggerProject.scheduleBuild2(0, new UserCause()).get();

        List<String> log = triggerProject.getLastBuild().getLog(30);
        for (String string : log) {
            System.out.println(string);
        }
        
        assertEquals("project1 #1 completed. Result was SUCCESS", log.get(2));
        assertEquals("project1 #2 completed. Result was SUCCESS", log.get(4));
        assertEquals("project1 #3 completed. Result was SUCCESS", log.get(6));
        assertEquals("project2 #1 completed. Result was SUCCESS", log.get(8));
        assertEquals("project2 #2 completed. Result was SUCCESS", log.get(10));
        assertEquals("project2 #3 completed. Result was SUCCESS", log.get(12));
        assertEquals("project3 #1 completed. Result was SUCCESS", log.get(14));
        assertEquals("project3 #2 completed. Result was SUCCESS", log.get(16));
        assertEquals("project3 #3 completed. Result was SUCCESS", log.get(18));
    }

    
    public void testBlockingTriggerWithDisabledProjects() throws Exception {
        createFreeStyleProject("project1");
        Project<?, ?> p2 = createFreeStyleProject("project2");
        p2.disable();
        createFreeStyleProject("project3");

        Project<?, ?> triggerProject = createFreeStyleProject("projectA");

        TriggerBuilder triggerBuilder = new TriggerBuilder(createTriggerConfig("project1, project2, project3"));

        triggerProject.getBuildersList().add(triggerBuilder);

        triggerProject.scheduleBuild2(0, new UserCause()).get();

        List<String> log = triggerProject.getLastBuild().getLog(20);
        for (String string : log) {
            System.out.println(string);
        }
        assertEquals("Waiting for the completion of project1", log.get(1));
        assertEquals("Skipping project2. The project is either disabled or the configuration has not been saved yet.", log.get(3));
        assertEquals("Waiting for the completion of project3", log.get(4));
      
    }
}
