Parameterized Trigger Plugin
============================
This plugin lets you trigger new builds when your build has completed, with various ways of specifying parameters for the new build.

See the documentation and release notes at [Parameterized Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin) on the Jenkins Wiki for more information.

Note: Set Java property "ptp.disableActionViews" (typically Launch Jenkins with -Dptp.disableActionViews) to stop the listing of
"Sub Projects" on the build and project main pages. This can be of use when other plugins are used to render the upstream/downstream
flow and there is reduntant information displayed about the triggered builds.
