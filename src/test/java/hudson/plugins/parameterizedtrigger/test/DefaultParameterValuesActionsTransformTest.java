package hudson.plugins.parameterizedtrigger.test;

import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.DefaultParameterValuesActionsTransform;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefaultParameterValuesActionsTransformTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void test() throws IOException {
        Project project = r.createFreeStyleProject("project");

        project.addProperty(new ParametersDefinitionProperty(
                    new StringParameterDefinition("key1", "value1"),
                    new StringParameterDefinition("key2", "value2")
                    ));

        ParametersAction action = new ParametersAction(
                new StringParameterValue("key2", "not-value2"),
                new StringParameterValue("key3", "value3")
                );

        DefaultParameterValuesActionsTransform transform = new DefaultParameterValuesActionsTransform();

        ParametersAction result = transform.transformParametersAction(action, project);

        assertEquals(3, result.getParameters().size());

        assertStringParameterValueEquals("value1", result.getParameter("key1"));
        assertStringParameterValueEquals("not-value2", result.getParameter("key2"));
        assertStringParameterValueEquals("value3", result.getParameter("key3"));
    }

    private static void assertStringParameterValueEquals(String expected, ParameterValue actual) {
        if (actual == null) {
            fail("ParameterValue is Null");
        } else {
            assertEquals(
                expected,
                ((StringParameterValue)actual).value
                );
        }
    }
}
