/*
 * The MIT License
 *
 * Copyright (c) 2013, Geoff Cummings
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
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import org.kohsuke.stapler.DataBoundConstructor;

public class RunParameterConfig implements Describable<RunParameterConfig> {

    private final String name;
    private final String projectName;
    private final String buildNumber;

    @DataBoundConstructor
    public RunParameterConfig(String name, String projectName, String buildNumber) {
        this.name = name;
        this.projectName = projectName;
        this.buildNumber = buildNumber;
    }

    public String getName() {
        return name;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    @SuppressWarnings("unchecked")
    public Descriptor<RunParameterConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RunParameterConfig> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }
    }
}
