# Changelog

#### 2.35.1

Release date: (July 12, 2017)

-   Prevent NullPointerException when Parameterized Trigger starts
    multiple builds and rejects some of them due to the missing
    Job/Build permission
    ([JENKINS-45471](https://issues.jenkins-ci.org/browse/JENKINS-45471))

#### 2.35

Release date: (July 10, 2017)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2017-07-10/)

#### 2.34

Release date: (Jun 19, 2017)

-   Update Jenkins core minimal requirement to 1.642.3
-   Trigger config: Handle missing ancestor job when selecting
    Downstream Projects in non-job
    items ([JENKINS-32527](https://issues.jenkins-ci.org/browse/JENKINS-32527))

#### 2.33

Release date:  (Feb 26, 2017)

-   Allow passing current build parameters to the projects being
    triggered via Parameterized Post-Build Trigger ([PR
    \#99](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/99))
-   Allow setting downstream projects to be triggered from child
    projects ([PR
    \#106](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/106))
-   Prevent error when triggering a list of project with a trailing
    comma ([PR
    \#111](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/111))
-   Prevent NullPointerException in the case of race condition with
    project enabling/disabling ([PR
    \#110](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/110))
-   Ensure that FileParameter file is closed after its reading when
    constructing parameters ([PR
    \#102](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/102))

#### 2.32

Release date: (Jul 26, 2016)

-   Log a message instead of throwing a NullPointerException when not
    being able to load a build's workspace ([PR
    \#105](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/105))

#### 2.31

Release date: (Dec 16, 2015)

-   Convert to use the new plugin parent pom
    ([JENKINS-34474](https://issues.jenkins-ci.org/browse/JENKINS-34474))

#### 2.30

Release date: (Dec 16, 2015)

-   Fix the sub project list visualization if a user has Item.DISCOVER
    without Item.READ (JENKINS-31727)
-   Minor fixes and improvements ([PR
    \#91](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/91))

#### 2.29

Release date: (Sep 10, 2015)

-   Workflow compatibility fixes
-   Extra unit tests for JENKINS-30040

#### 2.28

Release date: (Mar 7, 2015)

-   Support firing downstream workflow jobs via trigger ([issue
    \#26050](https://issues.jenkins-ci.org/browse/JENKINS-26050))

#### 2.27

Release date: (Mar 7, 2015)

-   Allow using non-default artifact manager for properties file ([issue
    \#28980](https://issues.jenkins-ci.org/browse/JENKINS-28980))

#### 2.26

Release date: (Mar 7, 2015)

-   Trim file names in property file lists
-   Updated the version of depending subversion-plugin to 1.38 with
    svnkit-1.3.6-jenkins-2 that supports TLS. (JENKINS-25772)
-   Add FAILURE\_OR\_BETTER Trigger Threshold. (JENKINS-26029)
-   Rely on archived artifacts to trigger (parameterized) build fallback
    to workspace for backward compatibility. (JENKINS-25192)
-   Added export of triggered builds and jobs to XML/JSON API.
    (JENKINS-26031)
-   Updated required core version to 1.580.1 - the plugin requires now
    Java 6 (the same as Jenkins Core itself)

#### 2.25

Release date: (Jun 1, 2014)

-   FIXED: useMatrixChild in FileBuildParameters cannot be configured at
    all. (JENKINS-22705)
-   Support absolute paths in "Parameters from properties file".
    (JENKINS-23084)
-   Allow using parameter files even if no workspace exists.
    (JENKINS-22229)
-   Output more informative logs when failing converting parameter
    values in compatibility mode. (JENKINS-22281)
-   FIXED: Checking project names in configuration page works wrong with
    Cloudbees Template plugin. (JENKINS-22856)

#### 2.24

Release date: (Mar 16, 2014)

-   This is a release to provide backward compatibility with
    Parameterized Trigger Plugin \<= 2.22 (JENKINS-22125).
    -   See \#Backward compatibility with version 2.22 for details.

#### 2.23

Release date: (Mar 09, 2014)

-   Now attempts to convert parameter strings to the type defined in the
    project
    -   When the parameter definition is subclass of
        `SimpleParameterDefinition`, Parameterized Trigger plugin tries
        to create a parameter value by calling
        `SimpleParameterDefinition#createValue`, which is used when
        triggering builds via CLI.
-   Avoid NPE by checking the `getLastSuccessfulBuild()` for null before
    calling `resolveProject()` (JENKINS-20932)
-   Supports property files with non-ascii characters. This feature only
    works properly in Java 1.6, but Java 1.5 is still supported.
    (JENKINS-19990, JENKINS-20651)
-   Matrix project support for "Parameters from properties file".
    (JENKINS-21013)
-   Added license notice (MIT) (JENKINS-21270)

#### 2.22

Release date: (Dec 13, 2013)

-   Fixed projects.size to projects.size() in groovy script. This caused
    exception with IBM Java 6. (JENKINS-20719)
-   expand variable in project names looking for unresolved names
-   Triggered subprojects displayed in build status pages now preserves
    the triggered order.

#### 2.21

Release date: (Oct 06, 2013)

-   Supported hierarchical job model (cloudbees folders)
-   Avoids NPE when used as a build step and the triggered job is
    deleted or renamed (JENKINS-19793)

#### 2.20

Release date: (Aug 26, 2013)

-   update/delete project references in conditional buildsteps
    (JENKINS-18967)
-   Removed unnecessary plugin dependency (related to JENKINS-19152)

#### 2.19

Release date: (Aug 11, 2013)

-   Bugfix: Check existence of all subproject(s) before launching
-   List configured and triggered subproject from project view
    (subprojects are grouped into static, dynamic and unresolved)
-   set upstream builds to original builds of promotions (JENKINS-17751)

#### 2.18

Release date: (Jun 2, 2013)

-   Added a new parameter factory type that takes multiple BLOB files
    and trigger one build each.

#### 2.17

Release date: (Feb 26, 2013)

Fixed Issues

-   Parameterized trigger from a multi-configuration project triggers
    downstream jobs twice (JENKINS-11669)
-   Allow specifying Git SHA1 commit id for next build(s) as Predefined
    parameter (JENKINS-13852)
-   Counter/File Parameter Factories now can prevent triggering, skip
    adding parameteers or fail the build if errors occur.
    (JENKINS-13872)
-   A parameterized buildstep in a matrix project causes
    IllegalArgumentException (JENKINS-14278)
-   Unusable environment variable TRIGGERED\_BUILD\_NUMBER\_jobname
    (JENKINS-14545)
-   It's not possible to specify multiple property files in parametrised
    trigger (JENKINS-15834)
-   Boolean parameter becomes string (JENKINS-15920)
-   Define impllicit parameter: "build them on the same node"
    (JENKINS-16334)

Note: Environment parameter names have changed from previous version, due to fix for (JENKINS-14545) when using as a build step.  
Project names are now alphanumeric only and all other consecutive chars replaced with \_

#### 2.16

Release date: (Oct 09, 2012)

-   Error validating project name in form (JENKINS-15130)
-   clarify error when no parameter set and `triggerWithNoParameter`
    unchecked (unfiled)

#### 2.15

Release date: (May 23, 2012)

-   Allow each parameter type to be selected only once (requires Jenkins
    core version 1.463!). (JENKINS-8916)
-   Fix triggering projects based on variables (JENKINS-13875)

#### 2.14

Release date: (Apr 27, 2012)

-   Fixed infinite loop, when "Block until the triggered projects finish
    their builds" option is used and triggered projects are disabled.
    (JENKINS-12923)
-   Added a new parameter definition in the trigger that lets the
    upstream controls which subset of the downstream matrix job to
    build.

#### 2.13

Release date: (Feb 9, 2012)

-   Builds triggered as build steps will now appear as downstream
    builds. (JENKINS-11082, JENKINS-9263, JENKINS-5184)
-   Info on builds kicked off as blocking build steps exposed.
    (JENKINS-11345)
-   When multiple builds are launched as blocking build steps, all
    should be listed as "waiting to complete", and should be listed in
    launched order. (JENKINS-11116, JENKINS-11117)
-   Fixed a compatibility with hierarchical jobs (such as the CloudBees
    folder plugin.)
-   Added a mechanism to repeatedly invoke jobs for each file that
    matches GLOB ([pull
    \#11](https://github.com/jenkinsci/parameterized-trigger-plugin/pull/11))

#### 2.12

Release date: (Oct 30, 2011)

-   Added a mechanism to repeatedly invoke the same project with
    different set of parameters.
-   Improved the default value of the blocking configuration.
-   Added hyperlinks to console output

#### 2.11

Release date: (Aug 6, 2011)

-   Added "unstable or worse" condition (JENKINS-9858).

#### 2.10

Release date: (Jul 10, 2011)

-   Implemented JENKINS-10028: Textbox for downstream projects should be
    validated as with the "Build other projects" text box.
-   Implemented JENKINS-8788: When renaming a job, the parameterized
    trigger should reflect the new name if the job appears as a
    parameterized triggered job.

#### 2.9

Release date: (Jul 10, 2011)

-   Fix subversion plugin dependency.
-   Multiple triggers between the same upstream/downstream project combo
    will now work, though this will also depend on Jenkins core 1.414.
    (JENKINS-8985)
-   Implemented JENKINS-9217: Added a new option to not run further
    build steps if triggered builds fail or are unstable
-   Implemented JENKINS-9391: Allow triggering projects based on
    variables

#### 2.8

Release date: (Mar 27, 2011)

-   Prevent IndexOutOfBoundsException, when no projects are specified.
-   Added new "Trigger build without parameters" option for post-action
    trigger builds.

#### 2.7

Release date: (Mar 1, 2011)

-   Rerelease 2.6 to properly set required Jenkins version.

#### 2.6

Release date: (Feb 17, 2011)

-   Improved the progress report to show what jobs are being executed
    when used as a builder.
-   Fixed NPE when the child build is called concurrently by other jobs.

#### 2.5

Release date: (Feb 12, 2011)

-   Added a mechanism to call other builds synchronously and map their
    results.

#### 2.4

Release date: (Jul 29, 2010)

-   Fix passing of parameters when a maven project is the upstream job.
    (JENKINS-6141)
-   Add support for multiconfiguration(matrix) projects. (JENKINS-5687)
-   Fix variable expansion in "predefined parameters" so backslashes in
    variable values are not lost. Add help text about backslash
    handling. (JENKINS-6004)
-   File parameters are currently not reusable, so omit these when
    "current build parameters" is selected. (JENKINS-6777)
-   Update trigger settings when other jobs are renamed or deleted.
    (JENKINS-6483)
-   Add an "always trigger" option. (JENKINS-6656)

#### 2.3

Release date: (Jan 16, 2010)

-   **NOTE**: This release is built against Jenkins 1.341 but works with
    Jenkins **1.319** or higher.
-   Implement Jenkins API so connected jobs show in Upstream/Downstream
    Projects list on job pages with Jenkins 1.341 or higher.
    (JENKINS-5184)
-   Merge together parameter lists from multiple sources to avoid
    multiple *Parameters* links on build pages (JENKINS-5143) and
    failure to pass some parameters further along the downstream chain
    (JENKINS-4587).

#### 2.2

Release date: (Jan 11, 2010)

-   Restore compatibility with Java 5. (JENKINS-5231)
-   Rollback change for JENKINS-5184, as this caused builds to be
    triggered twice. (JENKINS-5232)

#### 2.1

Release date: (Jan 9, 2010)

-   Implement Jenkins API so connected jobs show in Upstream/Downstream
    Projects list on job pages. (JENKINS-5184)

#### 2.0

Release date: (Aug 10, 2009)

Major refactoring. Now supports any combination of projects to build,
result condition and set of parameter sources.

Should be backward compatible for configuration, except the 'batch
condition' which was removed.

#### 1.6

Release date: (Jul 18, 2009)

-   Removed downstream projects parameters duplication.

#### 1.5

Release date: (Jul 5, 2009)

-   Added batch condition: a batch/shell script can be used to decide if
    project\[s\] built should be triggered.
-   If downstream project parameters values are not specified explicitly
    their default values are used now (empty values was used in previous
    versions).

#### 1.4

Release date: (Jun 11, 2009)

-   Environment variables (including current build parameters) can now
    be used in the downstream build parameters and in parameters file
    name.

#### 1.3

Release date: (Feb 28, 2009)

-   Trigger another build when the current is unstable

#### 1.2

Release date: (Feb 27, 2009)

-   Fixes incompatibility with Jenkins 1.285. Jenkins 1.286 is a minimum
    requirement for this version.

#### 1.0, 1.1 

Release date: (Feb 9, 2009)

-   Initial release

