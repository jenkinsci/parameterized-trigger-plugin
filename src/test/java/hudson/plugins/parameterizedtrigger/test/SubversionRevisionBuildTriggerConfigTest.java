package hudson.plugins.parameterizedtrigger.test;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.SubversionRevisionBuildParameters;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionTagAction;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class SubversionRevisionBuildTriggerConfigTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test @Ignore("https://groups.google.com/d/msg/jenkinsci-dev/8tLnOhHitKI/dCPJ53_wGAAJ")
	public void testRevisionParameter() throws Exception {
		FreeStyleProject p1 = r.createFreeStyleProject();
		FreeStyleProject p2 = r.createFreeStyleProject();
		p2.setQuietPeriod(1);

		p1.setScm(new SubversionSCM(
					"https://svn.jenkins-ci.org/trunk/hudson/test-projects/trivial-ant@13000"));

		p2.setScm(new SubversionSCM(
					"https://svn.jenkins-ci.org/trunk/hudson/test-projects/trivial-ant"));

		p1.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig(p2.getName(), ResultCondition.SUCCESS,
						new SubversionRevisionBuildParameters())));
		r.jenkins.rebuildDependencyGraph();

		FreeStyleBuild b1 = p1.scheduleBuild2(0, new Cause.UserCause()).get();
		Queue.Item q = r.jenkins.getQueue().getItem(p2);
		assertNotNull("p2 should be in queue (quiet period): " + JenkinsRule.getLog(b1), q);
		q.getFuture().get();

		FreeStyleBuild b2 = p2.getLastBuild();

		assertEquals(b1.getAction(SubversionTagAction.class).getTags().keySet()
				.iterator().next().revision,
				b2.getAction(SubversionTagAction.class).getTags().keySet()
						.iterator().next().revision);

	}
}
