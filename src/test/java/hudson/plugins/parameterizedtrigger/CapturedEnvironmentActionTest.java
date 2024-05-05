package hudson.plugins.parameterizedtrigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Functions;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Saveable;
import java.io.File;
import java.net.URL;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class CapturedEnvironmentActionTest {

    @Rule
    public JenkinsSessionRule j = new JenkinsSessionRule();

    @Test
    @Issue("SECURITY-2185") // @LocalData
    public void onLoad() throws Throwable {
        Assume.assumeFalse("test can not run on windows", Functions.isWindows());
        j.then(r -> {
            final URL url = CapturedEnvironmentActionTest.class.getResource(
                    "/hudson/plugins/parameterizedtrigger/CapturedEnvironmentActionTest/onLoad");
            if (url == null) {
                fail("No test resources found!");
            }
            if (!url.getProtocol().equals("file"))
                throw new AssertionError("Test data is not available in the file system: " + url);
            File home = new File(url.toURI());
            System.err.println("Loading $JENKINS_HOME from " + home);
            new FilePath(home).copyRecursiveTo("**/*", r.jenkins.getRootPath());
        });
        j.then(r -> {
            final OldDataMonitor monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
            final FreeStyleProject triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);
            final FreeStyleBuild build = triggering.getLastBuild();

            assertTrue("OldDataMonitor should be active.", monitor.isActivated());
            Map<Saveable, OldDataMonitor.VersionRange> data = monitor.getData();
            assertThat(
                    data,
                    hasEntry(
                            sameInstance(build),
                            new HasExtra(
                                    containsString("AssertionError: " + CapturedEnvironmentAction.OLD_DATA_MESSAGE))));

            build.save();
            data = monitor.getData();
            assertThat(data, anEmptyMap());
        });
        j.then(r -> {
            final OldDataMonitor monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
            final FreeStyleProject triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);

            assertFalse("OldDataMonitor should not be active.", monitor.isActivated());

            FreeStyleBuild build = triggering.getLastBuild();
            assertNotNull(build);
            assertEquals(1, build.getNumber());
            CapturedEnvironmentAction action = build.getAction(CapturedEnvironmentAction.class);
            assertNotNull(action);
            assertThat(action.getCapturedEnvironment(), anEmptyMap());

            r.buildAndAssertSuccess(triggering);
            r.waitUntilNoActivity();

            build = triggering.getLastBuild();
            assertNotNull(build);
            assertEquals(2, build.getNumber());
            action = build.getAction(CapturedEnvironmentAction.class);
            assertNotNull(action);
            assertThat(action.getCapturedEnvironment(), not(anEmptyMap()));
        });
        j.then(r -> {
            final OldDataMonitor monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
            final FreeStyleProject triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);

            assertFalse("OldDataMonitor should not be active.", monitor.isActivated());

            FreeStyleBuild build = triggering.getLastBuild();
            assertNotNull(build);
            assertEquals(2, build.getNumber());
            CapturedEnvironmentAction action = build.getAction(CapturedEnvironmentAction.class);
            assertNotNull(action);
            assertThat(action.getCapturedEnvironment(), anEmptyMap());
        });
    }

    private static class HasExtra extends TypeSafeMatcher<OldDataMonitor.VersionRange> {
        private final Matcher<? super String> valueMatcher;

        private HasExtra(final Matcher<? super String> valueMatcher) {
            this.valueMatcher = valueMatcher;
        }

        @Override
        protected boolean matchesSafely(final OldDataMonitor.VersionRange item) {
            return valueMatcher.matches(item.extra);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText(" a VersionRange with extra ").appendDescriptionOf(valueMatcher);
        }

        @Override
        protected void describeMismatchSafely(
                final OldDataMonitor.VersionRange item, final Description mismatchDescription) {
            mismatchDescription.appendText(" extra was ");
            valueMatcher.describeMismatch(item, mismatchDescription);
        }
    }
}
