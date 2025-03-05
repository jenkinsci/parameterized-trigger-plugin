package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.ProjectSpecificParameterValuesActionTransform;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ProjectSpecificParameterValuesActionTransformTest {

    @Test
    void test(JenkinsRule r) throws IOException {
        Project project = r.createFreeStyleProject("project");

        project.addProperty(new ParametersDefinitionProperty(new BooleanParameterDefinition("key1", false, "derp")));

        ParametersAction action = new ParametersAction(new StringParameterValue("key1", "true"));

        ProjectSpecificParameterValuesActionTransform transform = new ProjectSpecificParameterValuesActionTransform();

        ParametersAction result = transform.transformParametersAction(action, project);

        assertEquals(1, result.getParameters().size());
        assertInstanceOf(BooleanParameterValue.class, result.getParameter("key1"));
        assertTrue(((BooleanParameterValue) result.getParameter("key1")).value);
    }

    @Test
    @Issue("SECURITY-101")
    void passwordParameterAreCorrectlyConvertedFromUpstreamProject(JenkinsRule r) throws IOException {
        FreeStyleProject project = r.createFreeStyleProject("child");

        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("login", "default-login", null),
                new PasswordParameterDefinition("pwd", "default-password", null)));

        ParametersAction action = new ParametersAction(
                new StringParameterValue("login", "derpLogin"), new StringParameterValue("pwd", "pa33"));

        ProjectSpecificParameterValuesActionTransform transform = new ProjectSpecificParameterValuesActionTransform();

        ParametersAction result = transform.transformParametersAction(action, project);

        assertEquals(2, result.getParameters().size());
        assertInstanceOf(StringParameterValue.class, result.getParameter("login"));
        assertInstanceOf(PasswordParameterValue.class, result.getParameter("pwd"));
        assertEquals("derpLogin", ((StringParameterValue) result.getParameter("login")).value);
        assertEquals(
                "pa33",
                ((PasswordParameterValue) result.getParameter("pwd")).getValue().getPlainText());
    }
}
