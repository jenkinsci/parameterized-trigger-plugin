package hudson.plugins.parameterizedtrigger.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.*;
import hudson.tasks.Builder;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LinesFromFileParameterFactoryTest extends HudsonTestCase {

    private static final String TESTLINES_TXT = "testlines.txt";
    private static final String UNIX_NEWLINE = "\n";
    private static final String WINDOWS_NEWLINE = "\r\n";

    private static class AddParmeterLinesTextFileBuilder extends Builder {
        private int numOfParameters = 0;
        private int numOfExtraLines = 0;
        private String newLine;

        private AddParmeterLinesTextFileBuilder(int numOfParameters, int numOfExtraLines, String newLine) {
            this.numOfParameters = numOfParameters;
            this.numOfExtraLines = numOfExtraLines;
            this.newLine = newLine;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            FilePath ws = build.getWorkspace();
            String output = "";
            for (int i = 1; i <= numOfParameters; i++) {
                output += "Line" + i + newLine;
            }
            for (int i = 1; i <= numOfExtraLines; i++) {
                output += newLine;
            }
            ws.child(TESTLINES_TXT).write(output, "UTF8");
            return true;
        }
    }

    public void testWithOneParameterAndTwoLines_unixNewLine() throws Exception {
        Project<?, ?> projectA = createFreeStyleProject();
        projectA.getBuildersList().add(new AddParmeterLinesTextFileBuilder(2, 2, UNIX_NEWLINE));
        Project projectB = createFreeStyleProject();
        projectA.getBuildersList().add(
                new TriggerBuilder(
                        new BlockableBuildTriggerConfig(projectB.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(new LinesFromFileBuildParameterFactory(TESTLINES_TXT, "TEST=Foo$LINE")),
                                Collections.<AbstractBuildParameters>emptyList())));

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals("should be 2 builds triggered", 2, builds.size());
        Set<String> values = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals("lines should be included in parameter values", ImmutableSet.of("FooLine1", "FooLine2"), values);
    }

    public void testWithOneParameterAndTwoLines_windowsNewLine() throws Exception {
        Project<?, ?> projectA = createFreeStyleProject();
        projectA.getBuildersList().add(new AddParmeterLinesTextFileBuilder(2, 2, WINDOWS_NEWLINE));
        Project projectB = createFreeStyleProject();
        projectA.getBuildersList().add(
                new TriggerBuilder(
                        new BlockableBuildTriggerConfig(projectB.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(new LinesFromFileBuildParameterFactory(TESTLINES_TXT, "TEST=Foo$LINE")),
                                Collections.<AbstractBuildParameters>emptyList())));

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals("should be 2 builds triggered", 2, builds.size());
        Set<String> values = Sets.newHashSet();
        for (FreeStyleBuild build : builds) {
            EnvVars buildEnvVar = builder.getEnvVars().get(build.getId());
            assertTrue(buildEnvVar.containsKey("TEST"));
            values.add(buildEnvVar.get("TEST"));
        }
        assertEquals("lines should be included in parameter values", ImmutableSet.of("FooLine1", "FooLine2"), values);
    }

    public void testWithEmptyFile() throws Exception {
        Project<?, ?> projectA = createFreeStyleProject();
        projectA.getBuildersList().add(new AddParmeterLinesTextFileBuilder(0, 0, UNIX_NEWLINE));
        Project projectB = createFreeStyleProject();
        projectA.getBuildersList().add(
                new TriggerBuilder(
                        new BlockableBuildTriggerConfig(projectB.getName(),
                                new BlockingBehaviour(Result.FAILURE, Result.UNSTABLE, Result.FAILURE),
                                ImmutableList.<AbstractBuildParameterFactory>of(new LinesFromFileBuildParameterFactory(TESTLINES_TXT, "TEST=Foo$LINE")),
                                Collections.<AbstractBuildParameters>emptyList())));

        CaptureAllEnvironmentBuilder builder = new CaptureAllEnvironmentBuilder();
        projectB.getBuildersList().add(builder);
        projectB.setQuietPeriod(0);
        hudson.rebuildDependencyGraph();

        projectA.scheduleBuild2(0, new Cause.UserCause()).get();
        waitUntilNoActivity();
        List<FreeStyleBuild> builds = projectB.getBuilds();
        assertEquals("should be zero builds triggered", 0, builds.size());
    }

}
