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

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Project;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.SubProjectData;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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

    public void testGetProjectListWithWorkflow() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");
        WorkflowJob p = (WorkflowJob) Jenkins.getInstance().createProject(WorkflowJob.class, "workflowproject");
        p.setDefinition(new CpsFlowDefinition("println('hello')"));

        // Trigger a normal and workflow project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, workflowproject");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        List<Job> jobs = masterConfig.getProjectListGeneral(masterProject.getParent(), null);
        assertEquals(2, jobs.size());
        assertTrue("Job should include workflow job", jobs.contains(p));
        assertTrue("Job should include non-workflow job", jobs.contains(subProject1));

        List<AbstractProject> projects = masterConfig.getProjectList(masterProject.getParent(), null);
        assertEquals(1, projects.size());
        assertFalse("Projects should NOT include workflow job", projects.contains(p));
        assertTrue("Projects should include non-workflow job", projects.contains(subProject1));
    }

    public void testBuildWithWorkflowProjects() throws Exception {
        Project<?, ?> masterProject = createFreeStyleProject("project");
        WorkflowJob workflowProject = (WorkflowJob) Jenkins.getInstance().createProject(WorkflowJob.class, "workflowproject");
        workflowProject.setDefinition(new CpsFlowDefinition("node { echo myParam; }"));

        // Trigger a normal and workflow project
        String projectToTrigger = "subproject1, workflowproject";
        List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());

        PredefinedBuildParameters customParams = new PredefinedBuildParameters("myParam=GOOBER");
        buildParameters.add(customParams);


        BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");
        BlockableBuildTriggerConfig masterConfig = new BlockableBuildTriggerConfig(projectToTrigger, neverFail, buildParameters);

        addParameterizedTrigger(masterProject, masterConfig);

        Project subProject1 = createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Check all builds triggered correctly
        assertEquals(1, workflowProject.getBuilds().size());
        assertEquals(1, subProject1.getBuilds().size());

        // Verify workflow job completed successfully and that it was able to use the parameter set in trigger
        WorkflowRun workflowRun = workflowProject.getBuilds().get(0);
        assertEquals(Result.SUCCESS.ordinal, workflowRun.getResult().ordinal);
        assertTrue("Parameter was not passed correctly to workflow job!", workflowRun.getLog(100).contains("GOOBER"));
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
