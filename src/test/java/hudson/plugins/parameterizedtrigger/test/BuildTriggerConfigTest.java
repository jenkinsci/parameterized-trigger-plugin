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
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.User;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.SubProjectData;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.security.ACL;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContextHolder;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.acegisecurity.context.SecurityContext;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;

public class BuildTriggerConfigTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

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
    @Test
    public void testGetProjectListDynamic() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        // trigger two dynamic project
        BlockableBuildTriggerConfig masterConfig = createConfig("sub${JOB_NAME}1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = r.createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        r.jenkins.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 dynamic and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 0, 1, 0, 1);

    }

    /**
     * Testing fixed (statically) defined projects
     *
     * @throws Exception
     */
    @Test
    public void testGetProjectListStatic() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, subproject2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = r.createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        r.jenkins.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 1, 0, 0, 1);

    }

    @Test
    public void testGetProjectListWithWorkflow() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");
        WorkflowJob p = r.createProject(WorkflowJob.class, "workflowproject");
        p.setDefinition(new CpsFlowDefinition("println('hello')"));

        // Trigger a normal and workflow project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, workflowproject");
        addParameterizedTrigger(masterProject, masterConfig);

        // Only create 1 sub project
        Project subProject1 = r.createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);

        List<Job> jobs = masterConfig.getJobs(masterProject.getParent(), null);
        assertEquals(2, jobs.size());
        assertTrue("Job should include workflow job", jobs.contains(p));
        assertTrue("Job should include non-workflow job", jobs.contains(subProject1));

        List<AbstractProject> projects = masterConfig.getProjectList(masterProject.getParent(), null);
        assertEquals(1, projects.size());
        assertFalse("Projects should NOT include workflow job", projects.contains(p));
        assertTrue("Projects should include non-workflow job", projects.contains(subProject1));
    }

    @Test
    public void testBuildWithWorkflowProjects() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");
        WorkflowJob workflowProject = r.createProject(WorkflowJob.class, "workflowproject");
        workflowProject.setDefinition(new CpsFlowDefinition("node { echo myParam; }"));
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<ParameterDefinition>();
        definition.add(new StringParameterDefinition("myParam","myParam"));
        workflowProject.addProperty(new ParametersDefinitionProperty(definition));

        // Trigger a normal and workflow project
        String projectToTrigger = "subproject1, workflowproject";
        List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
        buildParameters.add(new CurrentBuildParameters());

        PredefinedBuildParameters customParams = new PredefinedBuildParameters("myParam=GOOBER");
        buildParameters.add(customParams);

        BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");
        BlockableBuildTriggerConfig masterConfig = new BlockableBuildTriggerConfig(projectToTrigger, neverFail, buildParameters);

        addParameterizedTrigger(masterProject, masterConfig);

        Project subProject1 = r.createFreeStyleProject("subproject1");
        subProject1.setQuietPeriod(0);
        subProject1.addProperty(new ParametersDefinitionProperty(definition));

        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Check all builds triggered correctly
        assertEquals(1, workflowProject.getBuilds().size());
        assertEquals(1, subProject1.getBuilds().size());

        // Verify workflow job completed successfully and that it was able to use the parameter set in trigger
        WorkflowRun workflowRun = workflowProject.getBuilds().get(0);
        r.assertBuildStatusSuccess(workflowRun);
        r.assertLogContains("GOOBER", workflowRun);
    }

    @Bug(31727)
    @Test
    public void testShouldNotFailOnDiscoverWithoutReadPermission() throws Exception {
        // Setup global security
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        User user = User.get("testUser");
        ProjectMatrixAuthorizationStrategy strategy = new ProjectMatrixAuthorizationStrategy();
        strategy.add(Item.DISCOVER, "anonymous");
        strategy.add(Jenkins.READ, "anonymous");
        r.jenkins.setAuthorizationStrategy(strategy);

        // Create project with downstream trigger
        final FreeStyleProject downstreamProject = r.createFreeStyleProject("downstreamProject");
        final FreeStyleProject upstreamProject = r.createFreeStyleProject("upstreamProject");
        final BlockableBuildTriggerConfig triggerConfig = createConfig("downstreamProject");
        addParameterizedTrigger(upstreamProject, triggerConfig);

        // Setup upstream project security
        Map<Permission,Set<String>> permissions = new HashMap<Permission,Set<String>>();
        Set<String> userIds = new HashSet<String>(Arrays.asList("testUser"));
        permissions.put(Item.READ, userIds);
        AuthorizationMatrixProperty projectPermissions = new AuthorizationMatrixProperty(permissions);
        upstreamProject.addProperty(projectPermissions);

        // Ensure that we can get the info about the downstream project, but it is unresolved
        ACL.impersonate(user.impersonate(), new Runnable() {
            @Override
            public void run() {
                SubProjectData projectInfo = triggerConfig.getProjectInfo(upstreamProject);
                assertTrue("Downstream project should be unresolved, because testUser has no READ permission",
                        projectInfo.getUnresolved().contains(downstreamProject.getName()));
            }
        });

        // Now invoke the build and check again (other logic handlers)
        r.buildAndAssertSuccess(upstreamProject);
        ACL.impersonate(user.impersonate(), new Runnable() {
            @Override
            public void run() {
                SubProjectData projectInfo = triggerConfig.getProjectInfo(upstreamProject);
                assertTrue("Downstream project should be unresolved, because testUser has no READ permission",
                        projectInfo.getUnresolved().contains(downstreamProject.getName()));
            }
        });
    }

    /**
     * Testing statically and dynamically defined projects
     *
     * @throws Exception
     */
    @Test
    public void testGetProjectListMix() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Create 2 sub projects
        r.createFreeStyleProject("subproject1").setQuietPeriod(0);
        r.createFreeStyleProject("subproject2").setQuietPeriod(0);

        r.jenkins.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 unresolved project
        validateOutcome(masterProject, masterConfig, 1, 1, 0, 0);

    }

    /**
     * Testing triggered projects
     *
     * @throws Exception
     */
    @Test
    public void testGetProjectListTriggered() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        // trigger two fixed project
        BlockableBuildTriggerConfig masterConfig = createConfig("subproject1, sub${JOB_NAME}2");
        addParameterizedTrigger(masterProject, masterConfig);

        // Create 2 sub projects
        r.createFreeStyleProject("subproject1").setQuietPeriod(0);
        r.createFreeStyleProject("subproject2").setQuietPeriod(0);

        r.jenkins.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Remove one trigger
        masterConfig = createConfig("subproject1");
        addParameterizedTrigger(masterProject, masterConfig);

        r.jenkins.rebuildDependencyGraph();
        masterProject.scheduleBuild2(0, new Cause.UserCause()).get();

        // Expects 1 fixed and 1 triggered project
        validateOutcome(masterProject, masterConfig, 1, 0, 1, 0);

    }

    @Test
    public void testBlankConfig() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        FormValidation form = r.jenkins.getDescriptorByType(BuildTriggerConfig.DescriptorImpl.class).doCheckProjects(masterProject, "");

        assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    public void testNonExistedProject() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        FormValidation form = r.jenkins.getDescriptorByType(BuildTriggerConfig.DescriptorImpl.class).doCheckProjects(masterProject, "nonExistedProject");

        assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    public void testValidConfig() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        FormValidation form = r.jenkins.getDescriptorByType(BuildTriggerConfig.DescriptorImpl.class).doCheckProjects(masterProject, "project");

        assertEquals(FormValidation.Kind.OK, form.kind);
    }

    @Test
    public void testBlankProjectNameInConfig() throws Exception {
        Project<?, ?> masterProject = r.createFreeStyleProject("project");

        FormValidation form = r.jenkins.getDescriptorByType(BuildTriggerConfig.DescriptorImpl.class).doCheckProjects(masterProject, "project, ");

        assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Issue("JENKINS-32527")
    @Test
    public void testFieldValidation() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("project");
        BuildTriggerConfig.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(BuildTriggerConfig.DescriptorImpl.class);
        assertNotNull(descriptor);
        // Valid value, Empty Value
        assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(p, p.getFullName()).kind);
        assertSame(FormValidation.Kind.ERROR, descriptor.doCheckProjects(p, "FOO").kind);
        assertSame(FormValidation.Kind.ERROR, descriptor.doCheckProjects(p, "").kind);
        //JENKINS-32526: Check that it behaves gracefully for an unknown context.
        assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(null, p.getFullName()).kind);
        assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(null, "FOO").kind);
        assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(null, "").kind);

        // Just returns OK if no permission
        r.jenkins.setAuthorizationStrategy(new ProjectMatrixAuthorizationStrategy());
        SecurityContext orig = ACL.impersonate(Jenkins.ANONYMOUS);
        try {
            assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(p, "").kind);
            assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(null, "").kind);
        } finally {
            SecurityContextHolder.setContext(orig);
        }

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(new MockQueueItemAuthenticator(Collections.singletonMap("project", User.get("alice").impersonate())));
        FreeStyleProject other = r.createFreeStyleProject("other");
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy().grant(Jenkins.READ, Item.READ).onItems(other).to("alice");
        r.jenkins.setAuthorizationStrategy(auth);
        assertSame(FormValidation.Kind.ERROR, descriptor.doCheckProjects(p, "other").kind);
        auth.grant(Item.BUILD).onItems(other).to("alice");
        assertSame(FormValidation.Kind.OK, descriptor.doCheckProjects(p, "other").kind);
    }
}
