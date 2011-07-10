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
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;

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

		//confirm projectA's post build trigger is updated automatically
		assertEquals("post build trigger project should be renamed", "projectB-renamed,projectC", projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects());
		
		projectB.delete();

		//confirm projectA's build step trigger is updated automatically:
		assertEquals("build step trigger project should be removed", "projectC", projectA.getBuildersList().get(TriggerBuilder.class).getConfigs().get(0).getProjects());

		//confirm projectA's post build trigger is updated automatically:
		assertEquals("post build trigger project should be removed", "projectC", projectA.getPublishersList().get(BuildTrigger.class).getConfigs().get(0).getProjects());
	}

	private Project<?, ?> createParentProject(String parentJobName, String... childJobNames) throws IOException {
		//create ProjectA
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

		//setup post build trigger
		project.getPublishersList().add(new BuildTrigger(new BuildTriggerConfig(childJobNamesString.toString(), ResultCondition.SUCCESS, new CurrentBuildParameters())));
		return project;
	}

}
