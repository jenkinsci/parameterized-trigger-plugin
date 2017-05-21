package hudson.plugins.parameterizedtrigger.test;

import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.ProjectSpecificParameterValuesActionTransform;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectSpecificParameterValuesActionTransformTest {
    
    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void test() throws IOException {
        Project project = r.createFreeStyleProject("project");

        project.addProperty(new ParametersDefinitionProperty(
                    new BooleanParameterDefinition("key1", false, "derp")
                    ));

        ParametersAction action = new ParametersAction(
                new StringParameterValue("key1", "true")
                );

        ProjectSpecificParameterValuesActionTransform transform = new ProjectSpecificParameterValuesActionTransform();

        ParametersAction result = transform.transformParametersAction(action, project);

        assertEquals(1, result.getParameters().size(), 1);
        assertTrue(result.getParameter("key1") instanceof BooleanParameterValue);
        assertTrue(((BooleanParameterValue)result.getParameter("key1")).value);
    }
}
