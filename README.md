Parameterized Trigger Plugin
============================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/parameterized-trigger.svg)](https://plugins.jenkins.io/parameterized-trigger)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/parameterized-trigger-plugin.svg?label=changelog)](https://github.com/jenkinsci/parameterized-trigger-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/parameterized-trigger.svg?color=blue)](https://plugins.jenkins.io/parameterized-trigger)

This plugin lets you trigger new builds when your build has completed,
with various ways of specifying parameters for the new build.

These new builds appear as "Subprojects" in the Jenkins UI when you
are looking at a project that triggers them.

You can add multiple configurations: each has a list of projects to trigger, a condition for when to trigger them 
(based on the result of the current build), and a parameters section.

There is also a Parameterized Remote Trigger Plugin in case you want to trigger a build on a different/remote Jenkins Controller.

The parameters section can contain a combination of one or more of the following:

-   a set of predefined properties
-   properties from a properties file read from the workspace of the triggering build
-   the parameters of the current build
-   **Subversion revision**: makes sure the triggered projects are built with the same revision(s) of the triggering build.
    You still have to make sure those projects are actually configured to checkout the right Subversion URLs.
-   **Restrict matrix execution to a subset**: allows you to specify the same combination filter expression as you use in the matrix project configuration and further restricts the subset of the downstream matrix builds to be run.

The parameter section is itself pluggable, and other plugins can contribute other sources of parameters.

This triggering mechanism can be used both as a post-build step or as a build step, in which case you can also block for the completion of the triggered builds.
This lets you create a "function call" like semantics.

| WARNING: You must define the parameter in downstream jobs via the _This project is parameterized_ property. For example, if _job1_ passes `ABC=123` to _job2_ then in _job2_ mark the job as _This project is parameterized_ and add a parameter named `ABC`. See [this advisory](https://jenkins.io/blog/2016/05/11/security-update/) for more information |
| --- |

 #### Usage as a Build step

When using the _Trigger/Call builds on another project_ item.  
If the trigger is configured with the "Block until the triggered projects finish their builds" enabled, the following Environment variables are made available for further build steps.

Env variables for future build steps:

-   `LAST_TRIGGERED_JOB_NAME` - Last project started
-   `TRIGGERED_BUILD_NUMBER_<project name>` - Last build numbertriggered" (since 2.17)
-   `TRIGGERED_JOB_NAMES` - Comma separated list of all triggered projects
-   `TRIGGERED_BUILD_NUMBERS_<project name>` - Comma separated list of build numbers triggered
-   `TRIGGERED_BUILD_RESULT_<project name>` - Last triggered build result of project
-   `TRIGGERED_BUILD_RESULT_<project name>*RUN*<build number>` - Result of triggered build for build number
-   `TRIGGERED_BUILD_RUN_COUNT_<project name>` - Number of builds triggered for the project

From 2.17 onwards: All Project names have characters not a-zA-Z or 0-9 replaced by  
`_(multiple characters are condensed into a single)`.  

Note that with the BuildStep a variable can be used for the project name, I.E. `${projectName}`.

#### Use of the plugin in a Matrix job

##### Post build task

When using the trigger parameterized build as a post build task for a matrix job the triggering will be be done once when all of the different matrix configurations have completed.  
In this case some of the Environment variables may not be resolvable as passing them to downstream jobs will fail.
You also cannot use a variable for the downstream project name.
If this functionality is needed, the BuildStep must be used. 

Environment variables that should be available are the the default shell ones (`<yourserver:port>/env-vars.html`) and ones defined as Parameters.  
Variables added by the other plugins as a buildwrappers may not be available.

##### Build step

When using the trigger parameterized build as a buildstep it will be called for every different configuration,
so if triggering another project with no parameters it will be done the same number of times as you have configurations,
possible causing the triggered job to run more than once.

However this also allows you to trigger other jobs with parameters relating to the current configuration,
i.e. triggering a build on the same node with the same JDK.

#### Plugins contributing additional parameter types to this plugin

* Git Plugin
* NodeLabel Parameter Plugin

#### Backward compatibility with version 2.22

-   Since Parameterized Trigger 2.23, there are cases that Parameterized Trigger fails to trigger downstream builds that can be successfully triggered with Parameterized Trigger \<= 2.22.
    -   This is caused by the new behavir introduced in Parameterized Trigger 2.23.
        It gets to pass parameter values not directly to the downstream build, but to parameter definitions of downstream projects.
        This enables parameter definitions perform its specific process, for example, selecting nodes with NodeLabel Parameter Plugin.
-   Example: There is a project with a choice parameter with choices A, B, C. 
    When you triggered that project with parameter value D, it fails with following output in the upstream:

```
    java.lang.IllegalArgumentException: Illegal choice: D at
    hudson.model.ChoiceParameterDefinition.checkValue(ChoiceParameterDefinition.java:72)
```

-   This is taken as a designated behavior.
    -   As those failures are ones designed by parameter definitions.
        For example, the choice parameter is designed not to accept unexpected values.
    -   You will face same problem when you triggered those builds with Jenkins CLI or Remote access API.
-   It is recommended to fix your project configuration to have parameter definitions not fail.
    -   For example, use EnvInject Plugin to process values or use Extensible Choice Parameter Plugin which provides a choice parameter accepting edited values.
        
##### Workaround
        
As backward compatibility, you can make it work without fix by setting Java system property `hudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode` to `true`.
It can be done with launching Jenkins as followings:

```
        java -Dhudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode=true -jar jenkins.war
```

In RedHat Linux systems, you can modify /etc/sysconfig/jenkins as following:

```
        -JENKINS\_JAVA\_OPTIONS="-Djava.awt.headless=true"
        +JENKINS\_JAVA\_OPTIONS="-Djava.awt.headless=true -Dhudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode=true"
```
In Debian Linux systems, you can modify /etc/defaults/jenkins as following:

```
        -JAVA_ARGS="-Djava.awt.headless=true" # Allow graphs etc. to work even when an X server is present
        +JAVA_ARGS="-Djava.awt.headless=true -Dhudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode=true"
```

In Windows system, you can modify jenkins.xml placed in the installed folder.

```
        -<arguments>-Xrs -Xmx256m -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -jar "%BASE%\\jenkins.war" --httpPort=8080\</arguments>
        +\<arguments>-Xrs -Xmx256m -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -Dhudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode=true -jar "%BASE%\\jenkins.war" --httpPort=8080\</arguments>
```

# Report an issue

Please report issues and enhancements through the [Jenkins issue tracker](https://www.jenkins.io/participate/report-issue/redirect/#15592).