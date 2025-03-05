package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.parameterizedtrigger.ParameterizedTriggerUtils;
import org.junit.jupiter.api.Test;

class ParameterizedTriggerUtilsTest {

    @Test
    void testMergeParameters() {
        ParametersAction base = new ParametersAction(
                new StringParameterValue("key1", "value1"), new StringParameterValue("key2", "value2"));

        ParametersAction overlay = new ParametersAction(
                new StringParameterValue("key2", "not-value2"), new StringParameterValue("key3", "value3"));

        ParametersAction result = ParameterizedTriggerUtils.mergeParameters(base, overlay);

        assertStringParameterValueEquals("value1", result.getParameter("key1"));
        assertStringParameterValueEquals("not-value2", result.getParameter("key2"));
        assertStringParameterValueEquals("value3", result.getParameter("key3"));
    }

    private static void assertStringParameterValueEquals(String expected, ParameterValue actual) {
        assertNotNull(actual, "ParameterValue is Null");
        assertEquals(expected, ((StringParameterValue) actual).value);
    }
}
