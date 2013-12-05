package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class FileBuildParameters extends AbstractBuildParameters {

	private final String propertiesFile;
	private final String encoding;
	private final boolean failTriggerOnMissing;

	@DataBoundConstructor
	public FileBuildParameters(String propertiesFile, String encoding, boolean failTriggerOnMissing) {
		this.propertiesFile = propertiesFile;
		this.encoding = Util.fixEmptyAndTrim(encoding);
		this.failTriggerOnMissing = failTriggerOnMissing;
	}

	public FileBuildParameters(String propertiesFile, boolean failTriggerOnMissing) {
		this(propertiesFile, null, failTriggerOnMissing);
	}

	public FileBuildParameters(String propertiesFile) {
		this(propertiesFile, false);
	}

	/**
	 * This function returns the Action that should be passed to the triggered build
	 * to not trigger the build it can throw the DontTriggerException
	 *
	 * @returns Action to be passed to the triggered build, can be Null if no parameters.
	 */
	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException, InterruptedException, DontTriggerException{

		EnvVars env = getEnvironment(build, listener);

		String resolvedPropertiesFile = env.expand(propertiesFile);

		String[] allFiles = Util.tokenize(resolvedPropertiesFile, ",");
		List<ParameterValue> values = new ArrayList<ParameterValue>();

		for(String file:allFiles) {
			FilePath f = build.getWorkspace().child(file);
			if (!f.exists()) {
				listener.getLogger().println("[parameterizedtrigger] Properties file "
					+ file + " did not exist.");
				if(getFailTriggerOnMissing() == true) {
					listener.getLogger().println("Not triggering due to missing file");
					throw new DontTriggerException();
				}
				// goto next file.
				continue;
			}

			String s = ParameterizedTriggerUtils.readFileToString(f, getEncoding());
			s = env.expand(s);
			Properties p = ParameterizedTriggerUtils.loadProperties(s);

			for (Map.Entry<Object, Object> entry : p.entrySet()) {
				values.add(new StringParameterValue(entry.getKey().toString(),
						entry.getValue().toString()));
			}
		}
		//Values might be empty, in that case don't return anything.
		return values.size() == 0 ? null :new ParametersAction(values);

	}

	public String getPropertiesFile() {
		return propertiesFile;
	}

	public String getEncoding() {
		return encoding;
	}

	public boolean getFailTriggerOnMissing() {
		return failTriggerOnMissing;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {
		@Override
		public String getDisplayName() {
			return "Parameters from properties file";
		}
		
		public FormValidation doCheckEncoding(@QueryParameter String encoding) {
			if (!StringUtils.isBlank(encoding)) {
				try {
					Charset.forName(encoding.trim());
				} catch(Exception e) {
					return FormValidation.error(e, "Unsupported Encoding");
				}
				if(!ParameterizedTriggerUtils.isSupportNonAsciiPropertiesFile()) {
					return FormValidation.warning("Non-ascii properties files are supported only since Java 1.6.");
				}
			}
			return FormValidation.ok();
		}
	}

}
