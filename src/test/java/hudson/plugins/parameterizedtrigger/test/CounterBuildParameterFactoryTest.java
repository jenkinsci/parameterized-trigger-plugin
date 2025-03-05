package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CounterBuildParameterFactoryTest {

    @Test
    void testWithOneParameter(JenkinsRule r) throws Exception {
        Project<?, ?> projectA = r.createFreeStyleProject();
        Project projectB = r.createFreeStyleProject();
        projectA.getBuildersList()
                .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                        projectB.getName(),
                        new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                        Collections.singletonList(new CounterBuildParameterFactory("0", "1", "1", "TEST=COUNT$COUNT")),
                        Collections.emptyList())));

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("TEST", "test"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(2, builds.size());
        Set<String> values = new HashSet<>();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(new HashSet<>(Arrays.asList("COUNT0", "COUNT1")), values);
    }

    @Test
    void testWithTwoParameters(JenkinsRule r) throws Exception {
        Project<?, ?> projectA = r.createFreeStyleProject();
        Project projectB = r.createFreeStyleProject();
        projectA.getBuildersList()
                .add(new TriggerBuilder(new BlockableBuildTriggerConfig(
                        projectB.getName(),
                        new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                        Arrays.asList(
                                new CounterBuildParameterFactory("0", "1", "1", "TEST=COUNT$COUNT"),
                                new CounterBuildParameterFactory("0", "2", "1", "NEWTEST=COUNT$COUNT")),
                        Collections.emptyList())));
        projectB.setConcurrentBuild(true);

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("TEST", "test"));
        definition.add(new StringParameterDefinition("NEWTEST", "newtest"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(6, builds.size());
        Set<String> values = new HashSet<>();
        Set<String> newValues = new HashSet<>();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            assertTrue(buildEnvVar.containsKey("NEWTEST"));
            values.add(buildEnvVar.get("TEST"));
            newValues.add(buildEnvVar.get("NEWTEST"));
        }
        assertEquals(new HashSet<>(Arrays.asList("COUNT0", "COUNT1")), values);
        assertEquals(new HashSet<>(Arrays.asList("COUNT0", "COUNT1", "COUNT2")), newValues);
    }
}
