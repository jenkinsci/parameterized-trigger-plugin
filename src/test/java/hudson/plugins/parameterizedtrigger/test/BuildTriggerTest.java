package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.DefaultMatrixExecutionStrategyImpl;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixConfigurationSorter;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.CurrentBuildParameters;
import hudson.plugins.parameterizedtrigger.FileBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 *
 * @author Lucie Votypkova
 */
@WithJenkins
class BuildTriggerTest {

    @Test
    void testParentProjectTrigger(JenkinsRule r) throws Exception {
        FreeStyleProject downstream = r.createFreeStyleProject("downstream");
        MatrixProject upstream = r.createProject(MatrixProject.class, "upstream");
        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("parameter", "parameter-value"));
        upstream.addProperty(new ParametersDefinitionProperty(definition));
        AxisList axes = new AxisList();
        axes.add(new TextAxis("textAxis", "one", "two"));
        upstream.setAxes(axes);
        List<AbstractBuildParameters> params = new ArrayList<>();
        params.add(new CurrentBuildParameters());
        BuildTrigger triggerBuilder = new BuildTrigger(
                new BuildTriggerConfig("downstream", ResultCondition.SUCCESS, false, null, params, false));
        upstream.getPublishersList().add(triggerBuilder);

        /*
         * To avoid a period when upstream already finished, but async rebuilding graph {@link Jenkins#rebuildDependencyGraphAsync()}
         * could still be in progress or even not start.
         * Let's rebuild graph manually just in case to avoid flakiness.
         */
        r.jenkins.rebuildDependencyGraph();

        r.buildAndAssertSuccess(upstream);
        r.waitUntilNoActivity();

        String project =
                downstream.getLastBuild().getCause(Cause.UpstreamCause.class).getUpstreamProject();
        assertEquals("upstream", project, "Build should be triggered by matrix project.");
    }

    @Test
    void testChildProjectsTrigger(JenkinsRule r) throws Exception {
        MatrixProject upstream = r.createProject(MatrixProject.class, "upstream");
        FreeStyleProject downstream = r.createFreeStyleProject("downstream");

        List<ParameterDefinition> definition = new ArrayList<>();
        definition.add(new StringParameterDefinition("parameter", "parameter-value"));
        ParametersDefinitionProperty property = new ParametersDefinitionProperty(definition);
        upstream.addProperty(property);

        AxisList axes = new AxisList();
        axes.add(new TextAxis("textAxis", "a", "b"));
        upstream.setAxes(axes);
        upstream.getBuildersList().add(new CreatePropertyFileBuilder());
        DefaultMatrixExecutionStrategyImpl strategy = new DefaultMatrixExecutionStrategyImpl(true, null, null, null);
        upstream.setExecutionStrategy(strategy);

        List<AbstractBuildParameters> params = new ArrayList<>();
        params.add(new CurrentBuildParameters());
        List<AbstractBuildParameters> parameters = new ArrayList<>();
        parameters.add(new FileBuildParameters("property.prop", null, false, true, null, false));
        BuildTriggerConfig config =
                new BuildTriggerConfig("downstream", ResultCondition.SUCCESS, false, parameters, true);
        BuildTrigger triggerBuilder = new BuildTrigger(config);
        upstream.getPublishersList().add(triggerBuilder);
        r.jenkins.rebuildDependencyGraph();
        strategy.setSorter(new MatrixConfigurationSorterTestImpl());

        r.buildAndAssertSuccess(upstream);
        r.waitUntilNoActivity();

        FreeStyleBuild downstreamBuild2 = downstream.getLastBuild();
        FreeStyleBuild downstreamBuild1 = downstreamBuild2.getPreviousBuild();
        assertNotNull(downstreamBuild1, "Downstream job should be triggered by matrix configuration");
        assertNotNull(downstreamBuild2, "Downstream job should be triggered by matrix configuration");

        String project1 = downstreamBuild1.getCause(Cause.UpstreamCause.class).getUpstreamProject();
        String project2 = downstreamBuild2.getCause(Cause.UpstreamCause.class).getUpstreamProject();
        ArrayList<MatrixConfiguration> configurations = new ArrayList<>(upstream.getItems());
        configurations.sort(new MatrixConfigurationSorterTestImpl());
        assertEquals(configurations.get(0).getFullName(), project1, "Build should be triggered by matrix project.");
        assertEquals(configurations.get(1).getFullName(), project2, "Build should be triggered by matrix project.");
    }

    public static class MatrixConfigurationSorterTestImpl extends MatrixConfigurationSorter implements Serializable {

        @Override
        public void validate(MatrixProject mp) {
            // do nothing
        }

        @Override
        public int compare(MatrixConfiguration o1, MatrixConfiguration o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class CreatePropertyFileBuilder extends Builder {

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            listener.getLogger().println("Creating a file property.prop");

            FilePath workspace = build.getWorkspace();
            if (workspace == null) {
                throw new AbortException("Cannot get the workspace of the build");
            }
            workspace.child("property.prop").write("value=" + build.getProject().getName(), "UTF-8");

            return true;
        }
    }
}
