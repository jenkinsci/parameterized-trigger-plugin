/*
 * The MIT License
 *
 * Copyright (c) 2011-2, Jørgen P. Tjernø <jorgenpt@gmail.com> 
 *                       Chris Johnson
 *                       Geoff Cummings
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
import hudson.model.Result;

public class BuildInfoExporterAction implements EnvironmentContributingAction {

  public static final String JOB_NAME_VARIABLE = "LAST_TRIGGERED_JOB_NAME";
  public static final String ALL_JOBS_NAME_VARIABLE = "TRIGGERED_JOB_NAMES";
  
  public static final String BUILD_NUMBER_VARIABLE = "LAST_TRIGGERED_BUILD_NUMBER";
  public static final String BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBER_";
  public static final String ALL_BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBERS_";

  public static final String BUILD_RESULT_VARIABLE = "LAST_TRIGGERED_BUILD_RESULT";
  public static final String BUILD_RESULT_VARIABLE_PREFIX = "TRIGGERED_BUILD_RESULT_";
  public static final String ALL_BUILD_RESULT_VARIABLE_PREFIX = "TRIGGERED_BUILD_RESULTS_";

  public static final String BUILD_RUN_COUNT_PREFIX = "TRIGGERED_BUILD_RUN_COUNT_";
  public static final String RUN = "_RUN_";
  
  private String buildName;
  private int buildNumber;
  private Result buildResult;
  
  public BuildInfoExporterAction(String buildName, int buildNumber, Result buildResult) {
    super();
    this.buildName = buildName;
    this.buildNumber = buildNumber;
    this.buildResult = buildResult;
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
    
    
  //Specific for the job we have just build
    String triggeredBuildRunKey = BUILD_RUN_COUNT_PREFIX + sanatizedBuildName;
    Integer triggeredBuildRun = 1;

    //check if job has been run before in this build.
    boolean jobHasRanBefore = false;
    if (env.containsKey(triggeredBuildRunKey)) {
        jobHasRanBefore = true;
        triggeredBuildRun = Integer.parseInt(env.get(triggeredBuildRunKey)) + 1;
    }
    env.put(triggeredBuildRunKey, Integer.toString(triggeredBuildRun));
    
    
    String tiggeredBuildNumberKey = BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName;
    String tiggeredBuildRunNumberKey = BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName + RUN + triggeredBuildRun;
    String tiggeredBuildResultKey = BUILD_RESULT_VARIABLE_PREFIX + sanatizedBuildName;
    String tiggeredBuildRunResultKey = BUILD_RESULT_VARIABLE_PREFIX + sanatizedBuildName + RUN + triggeredBuildRun;

    env.put(tiggeredBuildNumberKey, Integer.toString(buildNumber));
    env.put(tiggeredBuildRunNumberKey, Integer.toString(buildNumber));
    env.put(tiggeredBuildResultKey, buildResult.toString());
    env.put(tiggeredBuildRunResultKey, buildResult.toString());

    // Store a list of all jobs which have been run, along with numbers and results
    String buildNumberVariable = ALL_BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName;
    String buildResultVariable = ALL_BUILD_RESULT_VARIABLE_PREFIX + sanatizedBuildName;
    if (jobHasRanBefore) {
        // do not need to add it to list of job names
        // need to record this build number
        env.put(buildNumberVariable, env.get(buildNumberVariable) + "," + Integer.toString(buildNumber));
        // need to record this build result
        env.put(buildResultVariable, env.get(buildResultVariable) + "," + buildResult.toString());
    } else {
        // need to add it to list of job names
        if (env.containsKey(ALL_JOBS_NAME_VARIABLE)) {
            String originalvalue = env.get(ALL_JOBS_NAME_VARIABLE);
            env.put(ALL_JOBS_NAME_VARIABLE, originalvalue+","+sanatizedBuildName);
        } else {
            env.put(ALL_JOBS_NAME_VARIABLE, sanatizedBuildName);
        }
        // need to store the build number
        env.put(buildNumberVariable, Integer.toString(buildNumber));
        // need to record this build result
        env.put(buildResultVariable, buildResult.toString());
    }
  }
}
