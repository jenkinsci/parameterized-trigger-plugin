package hudson.plugins.parameterizedtrigger.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.model.StringParameterDefinition;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.CounterBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CounterBuildParameterFactoryTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void testWithOneParameter() throws Exception {
        Project<?,?> projectA = r.createFreeStyleProject();
        Project projectB = r.createFreeStyleProject();
        projectA.getBuildersList().add(
                new TriggerBuilder(
                        new BlockableBuildTriggerConfig(projectB.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(new CounterBuildParameterFactory("0","1","1", "TEST=COUNT$COUNT")),
                                Collections.<AbstractBuildParameters>emptyList())));

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<ParameterDefinition>();
        definition.add(new StringParameterDefinition("TEST","test"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(2, builds.size());
        Set<String> values = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals(ImmutableSet.of("COUNT0","COUNT1"), values);
    }

    @Test
    public void testWithTwoParameters() throws Exception {
        Project<?,?> projectA = r.createFreeStyleProject();
        Project projectB = r.createFreeStyleProject();
        projectA.getBuildersList().add(
                new TriggerBuilder(
                        new BlockableBuildTriggerConfig(projectB.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(
                                        new CounterBuildParameterFactory("0","1","1", "TEST=COUNT$COUNT"),
                                        new CounterBuildParameterFactory("0","2","1", "NEWTEST=COUNT$COUNT")
                                        ),
                                Collections.<AbstractBuildParameters>emptyList())));
        projectB.setConcurrentBuild(true);

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        // SECURITY-170: must define parameters in subjobs
        List<ParameterDefinition> definition = new ArrayList<ParameterDefinition>();
        definition.add(new StringParameterDefinition("TEST","test"));
        definition.add(new StringParameterDefinition("NEWTEST","newtest"));
        projectB.addProperty(new ParametersDefinitionProperty(definition));
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new UserIdCause()).get();
        r.waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals(6, builds.size());
        Set<String> values = Sets.newHashSet();
        Set<String> newValues = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            assertTrue(buildEnvVar.containsKey("NEWTEST"));
            values.add(buildEnvVar.get("TEST"));
            newValues.add(buildEnvVar.get("NEWTEST"));
        }
        assertEquals(ImmutableSet.of("COUNT0","COUNT1"), values);
        assertEquals(ImmutableSet.of("COUNT0", "COUNT1", "COUNT2"), newValues);
    }

}
