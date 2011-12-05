/*
 * The MIT License
 *
 * Copyright (c) 2011, Jørgen P. Tjernø <jorgenpt@gmail.com>
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

import java.util.Map;

import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.AbstractBuild;

public class BuildInfoExporterAction implements EnvironmentContributingAction {
  public static final String JOB_NAME_VARIABLE = "LAST_TRIGGERED_JOB_NAME";
  public static final String BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBER_";

  private String buildName;
  private int buildNumber;

  public BuildInfoExporterAction(String buildName, int buildNumber) {
    super();
    this.buildName = buildName;
    this.buildNumber = buildNumber;
  }

  public String getIconFileName() {
    return null;
  }

  public String getDisplayName() {
    return null;
  }

  public String getUrlName() {
    return null;
  }


  public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
    env.put(JOB_NAME_VARIABLE, buildName);
    env.put(BUILD_NUMBER_VARIABLE_PREFIX + buildName, Integer.toString(buildNumber));
  }
}
