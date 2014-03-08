package hudson.plugins.parameterizedtrigger.test;

import hudson.plugins.parameterizedtrigger.ParameterizedTriggerUtils;
import hudson.model.ParametersAction;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import junit.framework.TestCase;

public class ParameterizedTriggerUtilsTest extends TestCase {

    public void testMergeParameters() {
        ParametersAction base = new ParametersAction(
                new StringParameterValue("key1", "value1"),
                new StringParameterValue("key2", "value2")
                );

        ParametersAction overlay = new ParametersAction(
                new StringParameterValue("key2", "not-value2"),
                new StringParameterValue("key3", "value3")
                );

        ParametersAction result = ParameterizedTriggerUtils.mergeParameters(base, overlay);

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
