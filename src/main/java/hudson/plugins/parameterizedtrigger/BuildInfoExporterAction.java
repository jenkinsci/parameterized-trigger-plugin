/*
 * The MIT License
 *
 * Copyright (c) 2011-2, Jørgen P. Tjernø <jorgenpt@gmail.com> Chris Johnson
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
import java.util.Arrays;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.EnvironmentContributingAction;
import hudson.model.AbstractBuild;

public class BuildInfoExporterAction implements EnvironmentContributingAction {
  public static final String JOB_NAME_VARIABLE = "LAST_TRIGGERED_JOB_NAME";
  public static final String ALL_JOBS_NAME_VARIABLE = "TRIGGERED_JOB_NAMES";
  public static final String BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBER_";
  public static final String ALL_BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBERS_";

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
    String sanatizedBuildName = buildName.replaceAll("[^a-zA-Z0-9]+", "_");
    // Note: this will only indicate the last project in the list that is ran
    env.put(JOB_NAME_VARIABLE, sanatizedBuildName);
    // All Triggered job names 
    String originalvalue = env.get(ALL_JOBS_NAME_VARIABLE);
    if(originalvalue == null) {
        env.put(ALL_JOBS_NAME_VARIABLE, sanatizedBuildName);
    } else {
        String[] items = Util.tokenize(originalvalue, ",");
        if(! Arrays.asList(items).contains(sanatizedBuildName))
            env.put(ALL_JOBS_NAME_VARIABLE, originalvalue+","+sanatizedBuildName);
    }
    env.put(BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName, Integer.toString(buildNumber));
    
    // handle case where multiple builds are triggered 
    String buildVariable = ALL_BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName;
    originalvalue = env.get(buildVariable);
    if(originalvalue == null) {
        env.put(buildVariable, Integer.toString(buildNumber));
    } else {
        env.put(buildVariable, originalvalue+","+Integer.toString(buildNumber));
    }
  }
}
