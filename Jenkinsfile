// Builds a module using https://github.com/jenkins-infra/pipeline-library
buildPlugin(
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 17], // use 'docker' if you have containerized tests
    [platform: 'windows', jdk: 11],
])