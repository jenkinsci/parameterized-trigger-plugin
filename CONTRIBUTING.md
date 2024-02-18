# Contributing to the Parameterized Trigger Plugin

## Beginner Topics

Look for contribution areas in the
[issues in our issues tracker](https://issues.jenkins.io/issues/?jql=resolution%20is%20EMPTY%20and%20component%3D15592).

## Background

Plugin source code is hosted on [GitHub](https://github.com/jenkinsci/parameterized-trigger-plugin).
New feature proposals and bug fix proposals should be submitted as [GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).
Your pull request will be evaluated by the [Jenkins job](https://ci.jenkins.io/job/Plugins/job/parameterized-trigger-plugin/).

Before submitting your change, please assure that you've added tests which verify your change.

## Code formatting

Source code and pom file formatting is maintained by the `spotless` maven plugin.
Before submitting a pull request, confirm the formatting is correct with:

* `mvn spotless:apply`

## Building and Testing

Compile and test the plugin with the command:

* `mvn clean verify`

Compile the plugin without running tests using the command:

* `mvn clean -DskipTests verify`

### Reviewing code coverage

Code coverage reporting is available as a maven target.
Please try to improve code coverage with tests when you submit.

* `mvn -P enable-jacoco clean install jacoco:report` to report code coverage

The code coverage report is a set of HTML files that show methods and lines executed.
The following commands will open the `index.html` file in the browser.

* Windows - `start target\site\jacoco\index.html`
* Linux - `xdg-open target/site/jacoco/index.html`
* Gitpod - `cd target/site/jacoco && python -m http.server 8000`

The file will have a list of package names.
Click on them to find a list of class names.

The lines of the code will be covered in three different colors, red, green, and orange.
Red lines are not covered in the tests.
Green lines are covered with tests.

### Spotbugs static analysis

Please don't introduce new spotbugs output.

* `mvn spotbugs:check` to analyze project using https://spotbugs.github.io/[Spotbugs].
* `mvn spotbugs:gui` to review Spotbugs report using GUI

## Automated Tests

Automated tests are run as part of the `verify` phase.

```
$ mvn clean verify
```

## Issue Reports

Please report issues and enhancements through the [Jenkins issue tracker](https://www.jenkins.io/participate/report-issue/redirect/#15592).
