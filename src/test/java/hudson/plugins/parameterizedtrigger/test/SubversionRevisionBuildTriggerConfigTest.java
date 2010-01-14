package hudson.plugins.parameterizedtrigger.test;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.SubversionRevisionBuildParameters;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionTagAction;
import hudson.util.NullStream;

import java.io.IOException;
import java.io.PrintWriter;

import org.jvnet.hudson.test.HudsonTestCase;
import org.tmatesoft.svn.core.SVNException;

public class SubversionRevisionBuildTriggerConfigTest extends HudsonTestCase {

	protected void setJavaNetCredential() throws SVNException, IOException {
		// set the credential to access svn.dev.java.net
		hudson.getDescriptorByType(SubversionSCM.DescriptorImpl.class)
				.postCredential("https://svn.dev.java.net/svn/hudson/",
						"guest", "", null, new PrintWriter(new NullStream()));
	}

	public void testRevisionParameter() throws Exception {
		setJavaNetCredential();
		FreeStyleProject p1 = createFreeStyleProject();
		FreeStyleProject p2 = createFreeStyleProject();

		p1
				.setScm(new SubversionSCM(
						"https://svn.dev.java.net/svn/hudson/trunk/hudson/test-projects/trivial-ant@13000"));

		p2
				.setScm(new SubversionSCM(
						"https://svn.dev.java.net/svn/hudson/trunk/hudson/test-projects/trivial-ant"));

		p1.getPublishersList().add(
				new BuildTrigger(new BuildTriggerConfig(p2.getName(), ResultCondition.SUCCESS,
						new SubversionRevisionBuildParameters())));

		FreeStyleBuild b1 = p1.scheduleBuild2(0, new Cause.UserCause()).get();

		Thread.sleep(10000);

		FreeStyleBuild b2 = p2.getLastBuild();

		assertEquals(b1.getAction(SubversionTagAction.class).getTags().keySet()
				.iterator().next().revision,
				b2.getAction(SubversionTagAction.class).getTags().keySet()
						.iterator().next().revision);

	}
}
