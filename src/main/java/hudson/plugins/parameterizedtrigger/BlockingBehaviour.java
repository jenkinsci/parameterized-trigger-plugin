/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, InfraDNA, Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractDescribableImpl;
import org.apache.commons.lang.StringUtils;
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
public class BlockingBehaviour extends AbstractDescribableImpl<BlockingBehaviour> {
    public final Result buildStepFailureThreshold;
    public final Result unstableThreshold;
    public final Result failureThreshold;

    @DataBoundConstructor
    public BlockingBehaviour(String buildStepFailureThreshold, String unstableThreshold, String failureThreshold) {
        this.buildStepFailureThreshold = parse(buildStepFailureThreshold);
        this.unstableThreshold = parse(unstableThreshold);
        this.failureThreshold = parse(failureThreshold);
    }

    private Result parse(String t) {
        if(StringUtils.isBlank(t) || "never".equals(t)) {
            return null;
        }
        return Result.fromString(t);
    }

    public BlockingBehaviour(Result buildStepFailureThreshold, Result unstableThreshold, Result failureThreshold) {
        this.buildStepFailureThreshold = buildStepFailureThreshold;
        this.unstableThreshold = unstableThreshold;
        this.failureThreshold = failureThreshold;
    }
    
    /**
     * Maps the result of a triggered build to the result of the triggering build step.
     * 
     * @param r the {@link Result} of the triggered build to map
     * @return {@code false} if the triggering build step has to fail, {@code true} otherwise
     */
    public boolean mapBuildStepResult(Result r) {
        if (buildStepFailureThreshold!=null && r.isWorseOrEqualTo(buildStepFailureThreshold)) {
            return false;
        }
        return true;
    }

    /**
     * Maps the result of a triggered build to the result of the triggering build.
     * 
     * @param r the {@link Result} of the triggered build to map
     * @return the result of the triggering build
     */
    public Result mapBuildResult(Result r) {
        if (failureThreshold!=null && r.isWorseOrEqualTo(failureThreshold))   return FAILURE;
        if (unstableThreshold!=null && r.isWorseOrEqualTo(unstableThreshold))  return UNSTABLE;
        return SUCCESS;
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
