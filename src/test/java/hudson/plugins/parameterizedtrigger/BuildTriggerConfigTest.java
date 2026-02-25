package hudson.plugins.parameterizedtrigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class BuildTriggerConfigTest {

    @Test
    public void testOnJobRenamedMultipleProjects(JenkinsRule j) throws Exception {
        j.createFreeStyleProject("project_2");
        j.createFreeStyleProject("project_3");

        BuildTriggerConfig config =
                new BuildTriggerConfig("project_2, project_3", ResultCondition.SUCCESS, false, null);
        boolean changed = config.onJobRenamed(j.jenkins, "project_3", "project_5");

        assertTrue(changed, "The config should report that it was changed");
        assertEquals("project_2,project_5", config.getProjects());
    }

    @Test
    public void testOnJobRenamedMultipleTriggerBlocks(JenkinsRule j) throws Exception {
        j.createFreeStyleProject("project_2");
        hudson.model.FreeStyleProject p3 = j.createFreeStyleProject("project_3");
        hudson.model.FreeStyleProject p1 = j.createFreeStyleProject("project_1");

        p1.getPublishersList()
                .add(new BuildTrigger(java.util.Arrays.asList(
                        new BuildTriggerConfig("project_2", ResultCondition.SUCCESS, false, null))));
        p1.getPublishersList()
                .add(new BuildTrigger(java.util.Arrays.asList(
                        new BuildTriggerConfig("project_3", ResultCondition.SUCCESS, false, null))));

        p3.renameTo("project_5");

        java.util.List<BuildTrigger> triggers = p1.getPublishersList().getAll(BuildTrigger.class);
        assertEquals(2, triggers.size(), "Should still have two trigger blocks");
        assertEquals("project_2", triggers.get(0).getConfigs().get(0).getProjects());
        assertEquals("project_5", triggers.get(1).getConfigs().get(0).getProjects());
    }
}
