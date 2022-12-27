# Contributing to the Parameterized Trigger Plugin

## Beginner Topics

Look for contribution areas in the
[issues in our issues tracker](https://issues.jenkins.io/issues/?jql=resolution%20is%20EMPTY%20and%20component%3D15592).

## Background

Plugin source code is hosted on https://github.com/jenkinsci/parameterized-trigger-plugin[GitHub].
New feature proposals and bug fix proposals should be submitted as https://help.github.com/articles/creating-a-pull-request[GitHub pull requests].
Your pull request will be evaluated by the https://ci.jenkins.io/job/Plugins/job/parameterized-trigger-plugin/[Jenkins job].

Before submitting your change, please assure that you've added tests to verify your change.
Tests help us assure that we're delivering a reliable plugin, and that we've communicated our intent to other developers as executable descriptions of plugin behavior.

## Building and Testing

Compile and test the plugin with the command:

* `mvn clean verify`

Compile the plugin without running tests using the command:

* `mvn clean -DskipTests verify`

Code coverage reporting is available as a maven target.
Please try to improve code coverage with tests when you submit.

* `mvn -P enable-jacoco clean install jacoco:report` to report code coverage

Please don't introduce new spotbugs output.

* `mvn spotbugs:check` to analyze project using https://spotbugs.github.io/[Spotbugs].
* `mvn spotbugs:gui` to review Spotbugs report using GUI

## Automated Tests

Automated tests are run as part of the `verify` phase.
Run automated tests with multiple Java virtual machines with the command:

```
$ mvn clean -DforkCount=1C verify
```

## Issue Reports

Please report issues and enhancements through the [Jenkins issue tracker](https://www.jenkins.io/participate/report-issue/redirect/#15592).
