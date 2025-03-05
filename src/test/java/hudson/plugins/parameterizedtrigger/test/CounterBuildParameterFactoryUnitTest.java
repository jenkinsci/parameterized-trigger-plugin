package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory.SteppingValidationEnum;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Chris Johnson
 */
class CounterBuildParameterFactoryUnitTest {

    @Test
    void countingShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(1, 2, 1);
        assertEquals(2, parameters.size());
    }

    @Test
    void countingWithNegativeIncrementShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(2, 1, -1);
        assertEquals(2, parameters.size());
    }

    @Test
    void countingWithBigNegativeIncrementShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(2, 1, -3);
        assertEquals(1, parameters.size());
    }

    @Test
    void countingWithNoIncrementShouldNotWork() {
        assertThrows(RuntimeException.class, () -> getParameters(1, 2, 0));
    }

    @Test
    void countingWithNoIncrementOnlyOneElement() throws Exception {
        // step is ignored if from and to are equal
        List<AbstractBuildParameters> parameters = getParameters(1, 1, 0);
        assertEquals(1, parameters.size());
    }

    @Test
    void countingWhenFromToIsSameShouldWork() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(1, 1, 1);
        assertEquals(1, parameters.size());
    }

    @Test
    void countingWithNoIncrementShouldNotWork1() {
        assertThrows(RuntimeException.class, () -> getParameters(1, 2, 0, SteppingValidationEnum.FAIL));
    }

    @Test
    void countingWithNoIncrementShouldNotWork2() {
        assertThrows(
                AbstractBuildParameters.DontTriggerException.class,
                () -> getParameters(1, 2, 0, SteppingValidationEnum.SKIP));
    }

    @Test
    void countingWithNoIncrementShouldNotWork3() throws Exception {
        List<AbstractBuildParameters> parameters = getParameters(1, 2, 0, SteppingValidationEnum.NOPARMS);
        assertEquals(0, parameters.size());
    }

    private List<AbstractBuildParameters> getParameters(long from, long to, long step)
            throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        return getParameters(from, to, step, SteppingValidationEnum.FAIL);
    }

    private List<AbstractBuildParameters> getParameters(
            long from, long to, long step, SteppingValidationEnum validationFailure)
            throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        EnvVars vars = new EnvVars();
        TaskListener listener = mock(TaskListener.class);
        when(build.getEnvironment(listener)).thenReturn(vars);
        when(listener.getLogger()).thenReturn(System.out);
        CounterBuildParameterFactory counterFactory =
                new CounterBuildParameterFactory(from, to, step, "", validationFailure);

        return counterFactory.getParameters(build, listener);
    }
}
