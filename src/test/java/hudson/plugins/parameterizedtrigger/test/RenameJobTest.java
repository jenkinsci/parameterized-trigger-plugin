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

import hudson.model.FreeStyleBuild;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BuildStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jenkins.model.ModifiableTopLevelItemGroup;

import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockFolder;

public class RenameJobTest extends HudsonTestCase {

	public void testRenameAndDeleteJobSingleProject() throws Exception {
		//create projectA
		Project<?,?> projectA = createParentProject("projectA", "projectB");
		Project<?,?> projectB = createFreeStyleProject("projectB");
		hudson.rebuildDependencyGraph();

		projectB.renameTo("projectB-renamed");

		//confirm projectA's build step trigger is updated automatically
		assertEquals("build step trigger project should be renamed", "projectB-renamed", projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0).getProjects());

		//confirm projectA's post build trigger is updated automatically
		assertEquals("post build trigger project should be renamed", "projectB-renamed", projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects());
		
		projectB.delete();

		//confirm projectA's build step trigger is updated automatically:
		assertNull("now-empty build step trigger should be removed", projectA.getBuildersList().get(TriggerBuilder.class));

		//confirm projectA's post build trigger is updated automatically:
		assertNull("now-empty post build trigger should be removed", projectA.getPublishersList().get(BuildTrigger.class));
	}

	public void testRenameAndDeleteJobMultipleProjects() throws Exception {
		Project<?,?> projectA = createParentProject("projectA", "projectB", "projectC");
		Project<?,?> projectB = createFreeStyleProject("projectB");
		createFreeStyleProject("projectC");
		hudson.rebuildDependencyGraph();

		projectB.renameTo("projectB-renamed");
		
		//confirm projectA's build step trigger is updated automatically
		assertEquals("build step trigger project should be renamed", "projectB-renamed,projectC", projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0).getProjects());
		
	    final List<ConditionalBuilder> all = projectA.getBuildersList().getAll(ConditionalBuilder.class);
	    final TriggerBuilder wrappedBuilder0 = (TriggerBuilder)all.get(0).getConditionalbuilders().get(0);
	    assertEquals("build step trigger project within first conditionalbuildstep should be renamed", "projectB-renamed,projectC", wrappedBuilder0.getConfigs().get(0).getProjects());

	    final List<SingleConditionalBuilder> allSingleConditions = projectA.getBuildersList().getAll(SingleConditionalBuilder.class);
	    final TriggerBuilder singleCondTrigger0 = (TriggerBuilder)allSingleConditions.get(0).getBuildStep();
        assertEquals("build step trigger project within first singleconditionalbuildstep should be renamed", "projectB-renamed,projectC", singleCondTrigger0.getConfigs().get(0).getProjects());

		//confirm projectA's post build trigger is updated automatically
		assertEquals("post build trigger project should be renamed", "projectB-renamed,projectC", projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects());
		
		projectB.delete();

		//confirm projectA's build step trigger is updated automatically:
		assertEquals("build step trigger project should be removed", "projectC", projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0).getProjects());

		//confirm projectA's post build trigger is updated automatically:
		assertEquals("post build trigger project should be removed", "projectC", projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects());
		
	    //confirm deletes are reflected within conditional buildsteps too
        final List<ConditionalBuilder> allAfterDelete = projectA.getBuildersList().getAll(ConditionalBuilder.class);
        final TriggerBuilder wrappedBuilderAfterDel0 = (TriggerBuilder)allAfterDelete.get(0).getConditionalbuilders().get(0);
        assertEquals("build step trigger project within first conditionalbuildstep should be removed", "projectC", wrappedBuilderAfterDel0.getConfigs().get(0).getProjects());
        

        final List<SingleConditionalBuilder> allSingleAfterDelete = projectA.getBuildersList().getAll(SingleConditionalBuilder.class);
        final TriggerBuilder singleCondTriggerAfterDel0 = (TriggerBuilder)allSingleAfterDelete.get(0).getBuildStep();
        assertEquals("build step trigger project within first singleconditionalbuildstep should be removed", "projectC", singleCondTriggerAfterDel0.getConfigs().get(0).getProjects());
	}

	private Project<?, ?> createParentProject(String parentJobName, String... childJobNames) throws IOException {
		Project<?,?> project = createFreeStyleProject(parentJobName);

		List<AbstractBuildParameters> buildParameters = new ArrayList<AbstractBuildParameters>();
		buildParameters.add(new CurrentBuildParameters());
		
		StringBuilder childJobNamesString = new StringBuilder();
		for(String childJobName : childJobNames){
			childJobNamesString.append(childJobName);
			childJobNamesString.append(",");
		}
		
		//setup build step trigger
		project.getBuildersList().add(new TriggerBuilder(new BlockableBuildTriggerConfig(childJobNamesString.toString(), null, buildParameters)));
		
		// setup triggers for conditional buildsteps
		// test conditional builder (multi)
		List<BuildStep> blist = new ArrayList<BuildStep>();
		TriggerBuilder tb = new TriggerBuilder(new BlockableBuildTriggerConfig(childJobNamesString.toString(), null, buildParameters));
	    blist.add(tb);
	    project.getBuildersList().add(new ConditionalBuilder(new AlwaysRun(), new BuildStepRunner.Run(), blist));
	    
	    // test conditional builder (single)
	    TriggerBuilder tb2 = new TriggerBuilder(new BlockableBuildTriggerConfig(childJobNamesString.toString(), null, buildParameters));		    
	    project.getBuildersList().add(new SingleConditionalBuilder(tb2, new AlwaysRun(), new BuildStepRunner.Run()));

		//setup post build trigger
		project.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(childJobNamesString.toString(), ResultCondition.SUCCESS, new CurrentBuildParameters())));
		return project;
	}

	private <T extends TopLevelItem> T createProject(Class<T> type, ModifiableTopLevelItemGroup owner, String name) throws IOException {
	    return (T) owner.createProject((TopLevelItemDescriptor) jenkins.getDescriptorOrDie(type), name, true);
	}

    public void testRenameAndDeleteJobInSameFolder() throws Exception {
        MockFolder folder1 = createProject(MockFolder.class, jenkins, "Folder1");
        FreeStyleProject p1 = createProject(FreeStyleProject.class, folder1, "ProjectA");
        FreeStyleProject p2 = createProject(FreeStyleProject.class, folder1, "ProjectB");

        p1.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(
                p2.getName(),   // This should not be getFullName().
                ResultCondition.ALWAYS,
                true,
                Arrays.asList((AbstractBuildParameters)new CurrentBuildParameters())
        )));
        
        jenkins.rebuildDependencyGraph();
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        // Rename
        p2.renameTo("ProjectB-renamed");
        assertEquals("ProjectB-renamed", p2.getName());
        
        // assertRenamed
        assertEquals(
                p2.getName(),
                p1.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects()
        );
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        p2.delete();
        assertNull(p1.getPublishersList().get(BuildTrigger.class));
    }

    public void testRenameAndDeleteJobInSubFolder() throws Exception {
        MockFolder folder1 = createProject(MockFolder.class, jenkins, "Folder1");
        MockFolder folder2 = createProject(MockFolder.class, folder1, "Folder2");
        FreeStyleProject p1 = createProject(FreeStyleProject.class, folder1, "ProjectA");
        FreeStyleProject p2 = createProject(FreeStyleProject.class, folder2, "ProjectB");
        
        p1.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(
                String.format("%s/%s", folder2.getName(), p2.getName()),
                    // This should not be getFullName().
                ResultCondition.ALWAYS,
                true,
                null,
                Arrays.asList((AbstractBuildParameters)new CurrentBuildParameters())
        )));
        
        jenkins.rebuildDependencyGraph();
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        // Rename
        p2.renameTo("ProjectB-renamed");
        assertEquals("ProjectB-renamed", p2.getName());
        
        // assertRenamed
        assertEquals(
                String.format("%s/%s", folder2.getName(), p2.getName()),
                p1.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects()
        );
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        p2.delete();
        assertNull(p1.getPublishersList().get(BuildTrigger.class));
    }

    public void testRenameAndDeleteJobInParentFolder() throws Exception {
        MockFolder folder1 = createProject(MockFolder.class, jenkins, "Folder1");
        MockFolder folder2 = createProject(MockFolder.class, folder1, "Folder2");
        FreeStyleProject p1 = createProject(FreeStyleProject.class, folder2, "ProjectA");
        FreeStyleProject p2 = createProject(FreeStyleProject.class, folder1, "ProjectB");

        p1.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(
                String.format("../%s", p2.getName()),
                    // This should not be getFullName().
                ResultCondition.ALWAYS,
                true,
                null,
                Arrays.asList((AbstractBuildParameters)new CurrentBuildParameters())
        )));
        
        jenkins.rebuildDependencyGraph();
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        // Rename
        p2.renameTo("ProjectB-renamed");
        assertEquals("ProjectB-renamed", p2.getName());
        
        // assertRenamed
        assertEquals(
                String.format("../%s", p2.getName()),
                p1.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects()
        );
        
        // Test this works
        {
            assertNull(p2.getLastBuild());
            
            buildAndAssertSuccess(p1);
            
            jenkins.getQueue().getItem(p2).getFuture().get(60, TimeUnit.SECONDS);
            FreeStyleBuild b = p2.getLastBuild();
            assertNotNull(b);
            assertBuildStatusSuccess(b);
            b.delete();
        }
        
        p2.delete();
        assertNull(p1.getPublishersList().get(BuildTrigger.class));
    }
    
    /**
     * {@link hudson.model.Items#computeRelativeNamesAfterRenaming(String, String, String, hudson.model.ItemGroup)} has a bug
     * that renaming names that contains the target name as prefix.
     * E.g. renaming ProjectB to ProjectB-renamed results in renaming ProjectB2 to ProjectB-renamed2.
     * This is fixed in Jenkins 1.530.
     * 
     * This test verifies this plugin is not affected by that problem.
     */
    public void testComputeRelativeNamesAfterRenaming() throws Exception {
        FreeStyleProject projectA = createFreeStyleProject("ProjectA");
        FreeStyleProject projectB = createFreeStyleProject("ProjectB");
        FreeStyleProject projectB2 = createFreeStyleProject("ProjectB2");
        
        projectA.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(
                String.format("%s,%s", projectB.getName(), projectB2.getName()),
                ResultCondition.ALWAYS,
                true,
                null,
                Arrays.asList((AbstractBuildParameters)new CurrentBuildParameters())
        )));
        
        jenkins.rebuildDependencyGraph();
        
        projectB.renameTo("ProjectB-renamed");
        
        // assertRenamed
        assertEquals(
                String.format("%s,%s", projectB.getName(), projectB2.getName()),
                projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects()
        );
    }
}
