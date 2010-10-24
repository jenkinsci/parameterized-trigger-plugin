package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

import static hudson.model.Result.*;

/**
 * Determines how to handle the status of the triggered builds in {@link TriggerBuilder}.
 *
 * @author Kohsuke Kawaguchi
 */
public class BlockingBehaviour implements Describable<BlockingBehaviour> {
    public final Result unstableThreshold;
    public final Result failureThreshold;

    @DataBoundConstructor
    public BlockingBehaviour(String unstableThreshold, String failureThreshold) {
        this.unstableThreshold = parse(unstableThreshold);
        this.failureThreshold = parse(failureThreshold);
    }

    private Result parse(String t) {
        return t.equals("never") ? null : Result.fromString(t);
    }

    public BlockingBehaviour(Result unstableThreshold, Result failureThreshold) {
        this.unstableThreshold = unstableThreshold;
        this.failureThreshold = failureThreshold;
    }

    /**
     * Maps the result of the triggered build to the result of the triggering build.
     */
    public Result mapResult(Result r) {
        if (failureThreshold!=null && r.isWorseOrEqualTo(failureThreshold))   return FAILURE;
        if (unstableThreshold!=null && r.isWorseOrEqualTo(unstableThreshold))  return UNSTABLE;
        return SUCCESS;
    }

    @Override
    public Descriptor<BlockingBehaviour> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BlockingBehaviour> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

        public List<Result> getAllResults() {
            return Arrays.asList(SUCCESS,UNSTABLE,FAILURE);
        }
    }
}
