package hudson.plugins.parameterizedtrigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CapturedEnvironmentActionTest {

    // @LocalData
    @Test
    @Issue("SECURITY-2185")
    void onLoad(JenkinsRule r) throws Throwable {
        assumeFalse(Functions.isWindows(), "test can not run on windows");

        URL url = CapturedEnvironmentActionTest.class.getResource(
                "/hudson/plugins/parameterizedtrigger/CapturedEnvironmentActionTest/onLoad");
        assertNotNull(url, "No test resources found!");

        assertEquals("file", url.getProtocol(), "Test data is not available in the file system: " + url);
        File home = new File(url.toURI());
        System.err.println("Loading $JENKINS_HOME from " + home);
        new FilePath(home).copyRecursiveTo("**/*", r.jenkins.getRootPath());

        r.restart();

        OldDataMonitor monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
        FreeStyleProject triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);
        FreeStyleBuild build = triggering.getLastBuild();

        assertTrue(monitor.isActivated(), "OldDataMonitor should be active.");
        Map<Saveable, OldDataMonitor.VersionRange> data = monitor.getData();
        assertThat(
                data,
                hasEntry(
                        sameInstance(build),
                        new HasExtra(containsString("AssertionError: " + CapturedEnvironmentAction.OLD_DATA_MESSAGE))));

        build.save();
        data = monitor.getData();
        assertThat(data, anEmptyMap());

        r.restart();

        monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
        triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);

        assertFalse(monitor.isActivated(), "OldDataMonitor should not be active.");

        build = triggering.getLastBuild();
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

        r.restart();

        monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
        triggering = r.jenkins.getItem("triggering", r.jenkins, FreeStyleProject.class);

        assertFalse(monitor.isActivated(), "OldDataMonitor should not be active.");

        build = triggering.getLastBuild();
        assertNotNull(build);
        assertEquals(2, build.getNumber());
        action = build.getAction(CapturedEnvironmentAction.class);
        assertNotNull(action);
        assertThat(action.getCapturedEnvironment(), anEmptyMap());
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
