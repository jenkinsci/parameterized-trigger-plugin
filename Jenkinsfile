// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(
  // Run a JVM per core in tests
  forkCount: '1C',
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Test Java 11, 17, and 21
  configurations: [
    [platform: 'linux', jdk: 17],
    [platform: 'linux', jdk: 21, jenkins: '2.414'],
    [platform: 'windows', jdk: 11],
])
