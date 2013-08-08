/*
 * The MIT License
 *
 * Copyright (c) 2013 Sony Mobile Communications AB. All rights reserved.
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

import hudson.model.Cause;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.SubProjectData;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class BuildTriggerConfigTest extends HudsonTestCase {

    private BlockableBuildTriggerConfig createConfig(String projectToTrigger){
        List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());
        BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");
        return new BlockableBuildTriggerConfig(projectToTrigger, neverFail, buildParameters);
    }

    private void addParameterizedTrigger(Project<?, ?> projectA, BlockableBuildTriggerConfig config) throws Exception {
        projectA.getBuildersList().add(new TriggerBuilder(config));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectA.getBuildersList().add(builder);
    }

    private void validateOutcome(Project<?, ?> project, BuildTriggerConfig config,
                                 int fixedExpected, int dynamicExpected, int triggeredExpected, int unresolvedExpected) {

        SubProjectData subProjectData = config.getProjectInfo(project);

        assertEquals("Not the expected number of fixed project(s)", fixedExpected, subProjectData.getFixed().size());
        assertEquals("Not the expected number of dynamic project(s)", dynamicExpected, subProjectData.getDynamic().size());
        assertEquals("Not the expected number of triggered project(s)", triggeredExpected, subProjectData.getTriggered().size());
        assertEquals("Not the expected number of unresolved project(s)", unresolvedExpected, subProjectData.getUnresolved().size());
    }

    /**
     * Testing dynamically defined projects
     *
     * @throws Exception
     */
    public void testGetProjectListDynamic() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");

        // trigger two dynamic project
        BlockableBuildTriggerConfig masterConfig = createConfig("sub${JOB_NAME}1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        hudson.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 dynamic and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 0, 1, 0, 1);

    }

    /**
     * Testing fixed (statically) defined projects
     *
     * @throws Exception
     */
    public void testGetProjectListStatic() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, subproject2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        hudson.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 1, 0, 0, 1);

    }

    /**
     * Testing statically and dynamically defined projects
     *
     * @throws Exception
     */
    public void testGetProjectListMix() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Create 2 sub projects
        createFreeStyleProject("subproject1").setQuietPeriod(0);
        createFreeStyleProject("subproject2").setQuietPeriod(0);

        hudson.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 1, 1, 0, 0);

    }

    /**
     * Testing triggered projects
     *
     * @throws Exception
     */
    public void testGetProjectListTriggered() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Create 2 sub projects
        createFreeStyleProject("subproject1").setQuietPeriod(0);
        createFreeStyleProject("subproject2").setQuietPeriod(0);

        hudson.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Remove one trigger
        masterConfig = createConfig("subproject1");
        addParameterizedTrigger(masterProject, masterConfig);

        hudson.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 triggered project
        validateOutcome(masterProject, masterConfig, 1, 0, 1, 0);

    }

}
