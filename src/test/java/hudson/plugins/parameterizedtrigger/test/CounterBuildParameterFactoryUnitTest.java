package hudson.plugins.parameterizedtrigger.test;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CounterBuildParameterFactoryUnitTest {

    @Test
    public void countingShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(1,2,1);
        assertEquals(2, parameters.size());
    }

    @Test
    public void countingWithNegativeIncrementShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(2,1,-1);
        assertEquals(2, parameters.size());
    }

    @Test
    public void countingWithBigNegativeIncrementShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(2,1,-3);
        assertEquals(1, parameters.size());
    }

    @Test(expected = RuntimeException.class)
    public void countingWithNoIncrementShouldNotWork() throws Exception {
        getParameters(1,2,0);
    }

    @Test
    public void countingWithNoIncrementOnlyOneElementShouldNotWork() throws Exception {
        getParameters(1,1,0);
    }

    @Test
    public void countingWhenFromToIsSameShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(1,1,1);
        assertEquals(1, parameters.size());
    }

    private List<AbstractBuildParameters> getParameters(long from, long to, long step) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        AbstractBuild<?,?> build = mock(AbstractBuild.class);
        EnvVars vars = new EnvVars();
        TaskListener listener = mock(TaskListener.class);
        when(build.getEnvironment(listener)).thenReturn(vars);
        CounterBuildParameterFactory counterFactory = new CounterBuildParameterFactory(from,to,step,"");
        return counterFactory.getParameters(build, listener);
    }
}
