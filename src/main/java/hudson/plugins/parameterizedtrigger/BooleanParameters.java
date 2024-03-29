/*
 * The MIT License
 *
 * Copyright (c) 2013, Chris Johnson
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

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BooleanParameterValue;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Chris Johnson
 */
public class BooleanParameters extends AbstractBuildParameters {

    private final List<BooleanParameterConfig> configs;

    @DataBoundConstructor
    public BooleanParameters(List<BooleanParameterConfig> configs) {
        this.configs = configs;
    }

    @Override
    public Action getAction(AbstractBuild<?, ?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException {

        List<ParameterValue> values = configs.stream()
                .map(config -> new BooleanParameterValue(config.getName(), config.getValue()))
                .collect(Collectors.toList());

        return new ParametersAction(values);
    }

    public List<BooleanParameterConfig> getConfigs() {
        return configs;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
        @Override
        public String getDisplayName() {
            return "Boolean parameters";
        }
    }
}
