/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.parameterizedtrigger.test;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.NodeParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.slaves.DumbSlave;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author cjohnson
 */
public class NodeParametersTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
	public void test() throws Exception {

		DumbSlave slave0 = r.createOnlineSlave();
		DumbSlave slave1 = r.createOnlineSlave();
		
		Project<?,?> projectA = r.createFreeStyleProject("projectA");
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new NodeParameters())));

		projectA.setAssignedNode(slave0);
		
		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = r.createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(1);
		// set to build on slave1 
		projectB.setAssignedNode(slave1);
		r.jenkins.rebuildDependencyGraph();

		AbstractBuild buildA = projectA.scheduleBuild2(0).get();
		r.waitUntilNoActivity();
//		hudson.getQueue().getItem(projectB).getFuture().get();
		
		assertEquals(slave0, buildA.getBuiltOn());
		assertNotNull("builder should record environment", builder.getEnvVars());
		// ProjectB will be built on slave 0 regardless of assigned node.
		assertEquals("slave0", builder.getEnvVars().get("NODE_NAME"));

	}

    @Test
	public void testQueuedJobsCombined() throws Exception {

		DumbSlave slave0 = r.createOnlineSlave();
		DumbSlave slave1 = r.createOnlineSlave();
		
		Project<?,?> projectA = r.createFreeStyleProject("projectA");
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new NodeParameters())));

		projectA.setAssignedNode(slave0);
		
		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = r.createFreeStyleProject("projectB");
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(10);
		// set to build on slave1 
		projectB.setAssignedNode(slave1);
		r.jenkins.rebuildDependencyGraph();

		AbstractBuild buildA = projectA.scheduleBuild2(0).get();
		AbstractBuild buildA2 = projectA.scheduleBuild2(0).get();
		r.waitUntilNoActivity();
//		hudson.getQueue().getItem(projectB).getFuture().get();
		assertEquals(2, projectA.getBuilds().size());
		
		assertEquals(slave0, buildA.getBuiltOn());
		assertEquals(slave0, buildA2.getBuiltOn());
		assertNotNull("builder should record environment", builder.getEnvVars());
		// ProjectB will be built on slave 0 regardless of assigned node.
		assertEquals("slave0", builder.getEnvVars().get("NODE_NAME"));
		// should only be a single build of projectB
		assertEquals(1, projectB.getBuilds().size());
	}
	
    @Test
	public void testQueuedJobsNotCombined() throws Exception {

		DumbSlave slave0 = r.createOnlineSlave();
		DumbSlave slave1 = r.createOnlineSlave();
		DumbSlave slave2 = r.createOnlineSlave();
		
		Project<?,?> projectA = r.createFreeStyleProject("projectA");
		projectA.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, new NodeParameters())));

		projectA.setAssignedNode(slave0);
		
		CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
		Project projectB = r.createFreeStyleProject("projectB");
		
		int firstBuildNumber = projectB.getNextBuildNumber();
		projectB.getBuildersList().add(builder);
		projectB.setQuietPeriod(5);
		// set to build on slave1 
		projectB.setAssignedNode(slave1);
		r.jenkins.rebuildDependencyGraph();

		AbstractBuild buildA = projectA.scheduleBuild2(0).get();
		// Now trigger on another slave
		projectA.setAssignedNode(slave2);
		AbstractBuild buildA2 = projectA.scheduleBuild2(0).get();
		r.waitUntilNoActivity();
		
		assertEquals(slave0, buildA.getBuiltOn());
		assertEquals(slave2, buildA2.getBuiltOn());

		// should have two builds of projectB
		assertEquals(2, projectB.getBuilds().size());

		AbstractBuild buildB = (AbstractBuild)projectB.getBuildByNumber(firstBuildNumber);
		assertNotNull("ProjectB failed to build", buildB);
		assertEquals(slave0, buildB.getBuiltOn());

		// get the second build of projectB
		AbstractBuild buildB2 = (AbstractBuild)buildB.getNextBuild();
		assertNotNull("ProjectB failed to build second time", buildB2);
		assertEquals(slave2, buildB2.getBuiltOn());

	}
}
