/*
 * The MIT License
 *
 * Copyright (c) 2015-2017 CloudBees, Inc.
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
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.User;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticator;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import jenkins.security.QueueItemAuthenticatorDescriptor;
import org.acegisecurity.Authentication;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import static org.junit.Assert.*;
import org.junit.Before;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;

/**
 * Provides some permission checks for {@link TriggerBuilder}.
 * @author Oleg Nenashev
 */
public class ParameterizedTriggerPermissionTest {
    
    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Before
    public void setUpUserAndPermissions() {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        
        User.get("foo");
        ProjectMatrixAuthorizationStrategy str = new ProjectMatrixAuthorizationStrategy();
        str.add(Jenkins.READ, "foo");
        str.add(Item.READ, "foo");
        str.add(Item.DISCOVER, "foo");
        str.add(Computer.BUILD, "foo");
        r.jenkins.setAuthorizationStrategy(str);
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(new FooUserAuthenticator("foo"));
    }
    
    @Test
    @Issue("SECURITY-201")
    public void shouldBeUnableToTriggerWithoutPermissions_BuildStep() throws Exception {
        shouldBeUnableToTriggerWithoutPermissions(true);
    }
    
    @Test
    @Issue("SECURITY-201")
    public void shouldBeUnableToTriggerWithoutPermissions_Recorder() throws Exception {
        shouldBeUnableToTriggerWithoutPermissions(false);
    }
    
    public void shouldBeUnableToTriggerWithoutPermissions(boolean useBuildStep) throws Exception {        
        // Create controller project
        FreeStyleProject masterProject = createProjectWithPermissions("project", "foo", Arrays.asList(Item.BUILD));
        
        if (useBuildStep) {
            addParameterizedTrigger_BuildStep(masterProject, createBlockableConfig( "subproject1,subproject2"));
        } else {
            addParameterizedTrigger_Recorder(masterProject, createNonBlockableConfig("subproject1,subproject2"));
        }
        
        // Create subprojects
        FreeStyleProject subproject1 = createProjectWithPermissions("subproject1", "foo", null);
        FreeStyleProject subproject2 = createProjectWithPermissions("subproject2", "foo", Arrays.asList(Item.BUILD));
        r.jenkins.rebuildDependencyGraph();
        FreeStyleBuild build = r.buildAndAssertSuccess(masterProject);
        
        // Assert the subproject1 has not been built
        assertTrue("The subproject1 has been triggered, but it should not happen due to the build permissions", 
                subproject1.getBuilds().isEmpty());
        r.assertLogContains("has no Job.BUILD permission", build);
        
        if (!useBuildStep) {
            // Non-blocking, we have to check the status
            r.jenkins.getQueue().maintain();
            r.waitUntilNoActivity();
        }
        
        // Assert the subproject2 has been built properly
        assertEquals("The subproject2 should have been triggered once during the build", 
                1, subproject2.getBuilds().size());
        FreeStyleBuild lastBuild = subproject2.getLastBuild();
        assertNotNull("Cannot retrieve the last build of subproject2", lastBuild);
        Cause.UpstreamCause cause = lastBuild.getCause(Cause.UpstreamCause.class);
        assertNotNull("No upstream cause in subproject2", lastBuild);
    }

    @NonNull
    private FreeStyleProject createProjectWithPermissions(@NonNull String projectName, @NonNull String userName,
            @CheckForNull List<Permission> permissions) throws Exception {
        final TreeSet<String> userSet = new TreeSet<>(Arrays.asList(userName));
        
        FreeStyleProject project = r.createFreeStyleProject(projectName);
        HashMap<Permission, Set<String>> masterPermissions = new HashMap<>();
        if (permissions != null) {
            for (Permission p : permissions) {
                masterPermissions.put(p, userSet);
            }
        }
        AuthorizationMatrixProperty masterProp = new AuthorizationMatrixProperty(masterPermissions);
        
        project.addProperty(masterProp);
        project.setQuietPeriod(0);
        
        return project;
    }
 
    private BlockableBuildTriggerConfig createBlockableConfig(String projectToTrigger){
        List<AbstractBuildParameters> buildParameters = new ArrayList<>();
        buildParameters.add(new CurrentBuildParameters());
        BlockingBehaviour neverFail = new BlockingBehaviour("never", "never", "never");
        return new BlockableBuildTriggerConfig(projectToTrigger, neverFail, buildParameters);
    }
    
    private BuildTriggerConfig createNonBlockableConfig(String projectToTrigger){
        List<AbstractBuildParameters> buildParameters = new ArrayList<>();
        buildParameters.add(new CurrentBuildParameters());
        return new BuildTriggerConfig(projectToTrigger, ResultCondition.SUCCESS, true, 
                Collections.<AbstractBuildParameterFactory>emptyList(), Collections.<AbstractBuildParameters>emptyList(), false);
    }
    
    private void addParameterizedTrigger_BuildStep(Project<?, ?> projectA, BlockableBuildTriggerConfig config) 
            throws Exception {
        projectA.getBuildersList().add(new TriggerBuilder(config));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectA.getBuildersList().add(builder);
    }
    
    private void addParameterizedTrigger_Recorder(Project<?, ?> projectA, BuildTriggerConfig config) 
            throws Exception {
        projectA.getPublishersList().add(new BuildTrigger(config));
        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        projectA.getBuildersList().add(builder);
    }

    /**
     * Differs from {@link MockQueueItemAuthenticator} from the test-harness,
     * because authenticates the specified user for all jobs.
     */
    public static class FooUserAuthenticator extends QueueItemAuthenticator {

        private final String username;

        public FooUserAuthenticator(String username) {
            this.username = username;
        }
  
        public String getUsername() {
            return username;
        }
        
        @Override
        public Authentication authenticate(Queue.Task task) {
            return User.get(username).impersonate();
        }
     
        @Override
        public Authentication authenticate(Queue.Item item) {
            return User.get(username).impersonate();
        }
        
        @TestExtension("shouldBeUnableToTriggerWithoutPermissions")
        public static class DescriptorImpl extends QueueItemAuthenticatorDescriptor {
            @Override
            public String getDisplayName() {
                return "Authenticate as a specified user";
            }
        } 
    }
}
