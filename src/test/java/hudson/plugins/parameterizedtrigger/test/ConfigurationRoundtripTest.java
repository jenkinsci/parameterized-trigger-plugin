/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
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
package hudson.plugins.parameterizedtrigger.test;

import hudson.model.FreeStyleProject;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.SubversionRevisionBuildParameters;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConfigurationRoundtripTest extends HudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
/*  poorly written core breaks this test, so this is not yet testable.      
        FreeStyleProject p = createFreeStyleProject();
        BuildTrigger orig = new BuildTrigger(
                // try 0, 1, and 2 build parameters
                new BuildTriggerConfig("x", ResultCondition.ALWAYS),
                new BuildTriggerConfig("y", ResultCondition.FAILED,
                        new FileBuildParameters("foo")),
                new BuildTriggerConfig("z", ResultCondition.UNSTABLE_OR_BETTER,
                        new CurrentBuildParameters(),
                        new SubversionRevisionBuildParameters()));
        p.getPublishersList().add(orig);
        submit(createWebClient().getPage(p,"configure").getFormByName("config"));

        // TODO: switch to assertEqualDataBoundBeans in newer Hudson test harness.
        assertEqualBeans(orig, p.getPublishersList().get(BuildTrigger.class), "configs");
*/
    }
}
