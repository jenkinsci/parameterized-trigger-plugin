package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wolfs
 */
public class CounterBuildParameterFactory extends AbstractBuildParameterFactory {

    private final long from;
    private final long to;
    private final long step;
    private final String paramExpr;

    @DataBoundConstructor
    public CounterBuildParameterFactory(long from, long to, long step, String paramExpr) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.paramExpr = paramExpr;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        ArrayList<AbstractBuildParameters> params = Lists.newArrayList();
        for (Long i = from; i < to; i += step) {
            String stringWithCount = Util.replaceMacro(paramExpr, ImmutableMap.of("COUNT", i.toString()));
            params.add(new PredefinedBuildParameters(stringWithCount));
        }
        return params;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameterFactory> {
        @Override
        public String getDisplayName() {
            return "Counter Parameter Factory";
        }
    }

}
