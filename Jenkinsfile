// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(
  useContainerAgent: true,
  configurations: [
    // [platform: 'linux', jdk: 17], // Java 17 fails tests with a serialization error in mocked objects
    [platform: 'linux', jdk: 11],
    [platform: 'windows', jdk: 11],
])
