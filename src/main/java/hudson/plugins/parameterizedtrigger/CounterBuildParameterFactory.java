package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A BuildParameterFactory generating Predefined Parameters for a counter
 */
public class CounterBuildParameterFactory extends AbstractBuildParameterFactory {

    private final String from;
    private final String to;
    private final String step;
    private final String paramExpr;


    public CounterBuildParameterFactory(long from, long to, long step, String paramExpr) {
        this(Long.toString(from), Long.toString(to), Long.toString(step), paramExpr);
    }

    @DataBoundConstructor
    public CounterBuildParameterFactory(String from, String to, String step, String paramExpr) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.paramExpr = paramExpr;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        EnvVars envVars = build.getEnvironment(listener);

        long fromNum = Long.valueOf(envVars.expand(from));
        long toNum = Long.valueOf(envVars.expand(to));
        long stepNum = Long.valueOf(envVars.expand(step));

        ArrayList<AbstractBuildParameters> params = Lists.newArrayList();
        int upDown = Long.signum(toNum - fromNum);

        if (upDown == 0) {
            params.add(getParameterForCount(fromNum));
        } else {
            if (stepNum == 0) {
                throw new RuntimeException(Messages.CounterBuildParameterFactory_CountingWillNotTerminate());
            }
            if (upDown * stepNum < 0) {
                throw new RuntimeException(Messages.CounterBuildParameterFactory_CountingWillNotTerminate());
            }

            for (Long i = fromNum; upDown * i <= upDown * toNum; i += stepNum) {
                params.add(getParameterForCount(i));
            }
        }
        return params;
    }

    private PredefinedBuildParameters getParameterForCount(Long i) {
        String stringWithCount = Util.replaceMacro(paramExpr, ImmutableMap.of("COUNT", i.toString()));
        return new PredefinedBuildParameters(stringWithCount);
    }

    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.CounterBuildParameterFactory_CounterBuildParameterFactory();
        }

        public FormValidation doCheckFrom(@QueryParameter String value) { return validateNumberField(value);
        }
        public FormValidation doCheckTo(@QueryParameter String value) {
            return validateNumberField(value);
        }
        public FormValidation doCheckStep(@QueryParameter String value) {
            return validateNumberField(value);
        }

        private FormValidation validateNumberField(String value) {
            // The field can contain Parameters - eliminate them first. The remaining String should
            // be empty or a number.
            String valueWithoutVariables = Util.replaceMacro(value, EMPTY_STRING_VARIABLE_RESOLVER);
            if (StringUtils.isNotEmpty(valueWithoutVariables) && !isNumber(valueWithoutVariables)) {
                return FormValidation.warning(hudson.model.Messages.Hudson_NotANumber());
            } else {
                return FormValidation.validateRequired(value);
            }
        }

        private boolean isNumber(String value) {
            try {
                Long.valueOf(value);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getStep() {
        return step;
    }

    public String getParamExpr() {
        return paramExpr;
    }

    private static final VariableResolver<String> EMPTY_STRING_VARIABLE_RESOLVER = new VariableResolver<String>() {

        @Override
        public String resolve(String name) {
            return "";
        }
    };

}
