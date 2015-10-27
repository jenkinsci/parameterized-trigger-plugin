package hudson.plugins.parameterizedtrigger.test;

import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.Project;
import hudson.model.TextParameterDefinition;
import hudson.model.TextParameterValue;
import hudson.plugins.parameterizedtrigger.ProjectSpecificParameterValuesActionTransform;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;

public class ProjectSpecificParameterValuesActionTransformTest extends HudsonTestCase {
    public void test() throws IOException {
        Project project = createFreeStyleProject("project");

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

    public void testStringParamSubclass() throws IOException {
        Project project = createFreeStyleProject("project");

        project.addProperty(new ParametersDefinitionProperty(
                    new TextParameterDefinition("key1", "herpderp", "derp")
                    ));

        ParametersAction action = new ParametersAction(
                new StringParameterValue("key1", "herpaderp")
                );

        ProjectSpecificParameterValuesActionTransform transform = new ProjectSpecificParameterValuesActionTransform();

        ParametersAction result = transform.transformParametersAction(action, project);

        assertEquals(1, result.getParameters().size(), 1);
        assertTrue(result.getParameter("key1") instanceof TextParameterValue);
        assertEquals(((TextParameterValue)result.getParameter("key1")).value, "herpaderp");
    }
}
