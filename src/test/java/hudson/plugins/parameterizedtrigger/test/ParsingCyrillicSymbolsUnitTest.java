package hudson.plugins.parameterizedtrigger.test;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStreamReader;
import java.util.Properties;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * User: lanwen
 * Date: 11.11.13
 * Time: 0:58
 */
@RunWith(MockitoJUnitRunner.class)
public class ParsingCyrillicSymbolsUnitTest {
    public static final String VALUE_UTF_STRING = "Кириллица";
    public static final String PARAM_NAME = "PARAM";
    public static final String PROPERTIES_SOURCE = PARAM_NAME + "=" + VALUE_UTF_STRING;


    @Mock
    private AbstractBuild build;
    @Mock
    private TaskListener listener;

    @Test
    public void propertiesShouldBeLoadedLikeThisExample() throws Exception {
        Properties p = new Properties();
        p.load(new InputStreamReader(new StringInputStream(PROPERTIES_SOURCE), defaultCharset()));
        assertThat("Wrong encoded cyrillic symbols", p, hasEntry((Object) PARAM_NAME, (Object) VALUE_UTF_STRING));
    }

    @Test
    public void predefinedBuildParamsShouldParseCyrillicSymbols() throws Exception {
        PredefinedBuildParameters predefParams = spy(new PredefinedBuildParameters(PROPERTIES_SOURCE));
        when(predefParams.getEnvironment(build, listener)).thenReturn(new EnvVars());
        ParameterValue parameter = ((ParametersAction) predefParams.getAction(build, listener))
                .getParameter(PARAM_NAME);

        EnvVars vars = new EnvVars();
        parameter.buildEnvVars(build, vars);

        assertThat("Wrong encoded cyrillic symbols", vars, hasEntry(PARAM_NAME, VALUE_UTF_STRING));
    }
}
