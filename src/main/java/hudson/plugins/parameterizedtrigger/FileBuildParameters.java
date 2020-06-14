package hudson.plugins.parameterizedtrigger;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.TextParameterValue;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jenkins.util.VirtualFile;

public class FileBuildParameters extends AbstractBuildParameters {
	private final String propertiesFile;
	private final String encoding;
	private final boolean failTriggerOnMissing;
	private final boolean textParamValueOnNewLine;
	
	/*properties used for a matrix project*/
	private final boolean useMatrixChild;
	private final String combinationFilter;
	private final boolean onlyExactRuns;

	@DataBoundConstructor
	public FileBuildParameters(String propertiesFile, String encoding, boolean failTriggerOnMissing, boolean useMatrixChild, String combinationFilter, boolean onlyExactRuns, boolean textParamValueOnNewLine) {
		this.propertiesFile = propertiesFile;
		this.encoding = Util.fixEmptyAndTrim(encoding);
		this.failTriggerOnMissing = failTriggerOnMissing;
		this.useMatrixChild = useMatrixChild;
		if (this.useMatrixChild) {
			this.combinationFilter = combinationFilter;
			this.onlyExactRuns = onlyExactRuns;
		} else {
			this.combinationFilter = null;
			this.onlyExactRuns = false;
		}
		this.textParamValueOnNewLine = textParamValueOnNewLine;
	}

	public FileBuildParameters(String propertiesFile, String encoding, boolean failTriggerOnMissing, boolean useMatrixChild, String combinationFilter, boolean onlyExactRuns) {
		this(propertiesFile, encoding, failTriggerOnMissing, useMatrixChild, combinationFilter, onlyExactRuns, false);
	}

	public FileBuildParameters(String propertiesFile, String encoding, boolean failTriggerOnMissing) {
		this(propertiesFile, encoding, failTriggerOnMissing, false, null, false);
	}

	public FileBuildParameters(String propertiesFile, boolean failTriggerOnMissing) {
		this(propertiesFile, null, failTriggerOnMissing);
	}

	public FileBuildParameters(String propertiesFile) {
		this(propertiesFile, false);
	}

	/**
	 * This function returns the Action that should be passed to the triggered build
	 * to not trigger the build it can throw the {@link DontTriggerException}
	 *
	 * @return Action to be passed to the triggered build, can be Null if no parameters.
	 */
	public Action getAction(AbstractBuild<?,?> build, TaskListener listener)
			throws IOException, InterruptedException, DontTriggerException{

		EnvVars env = getEnvironment(build, listener);

		String resolvedPropertiesFile = env.expand(propertiesFile);

		String[] allFiles = Util.tokenize(resolvedPropertiesFile, ",");
		String[] trimmedFiles = new String[allFiles.length];
		for (int i = 0; i < allFiles.length; i++)
			trimmedFiles[i] = allFiles[i].trim();

		List<ParameterValue> values = new ArrayList<>();

		// builds to scan.
		Collection<? extends AbstractBuild<?,?>> targetBuilds = getTargetBuilds(build);
		
		for (AbstractBuild<?,?> targetBuild: targetBuilds) {
			values.addAll(extractAllValues(targetBuild, listener, trimmedFiles));
		}
		//Values might be empty, in that case don't return anything.
		return values.size() == 0 ? null :new ParametersAction(values);
	}
	
	private List<ParameterValue> extractAllValues(AbstractBuild<?,?> build, TaskListener listener, String[] allFiles) throws IOException, InterruptedException, DontTriggerException {
		List<ParameterValue> values = new ArrayList<>();
		EnvVars env = getEnvironment(build, listener);
		for(String file:allFiles) {
			String s = null;
			VirtualFile artifact = build.getArtifactManager().root().child(file);
			if (artifact.isFile()) {
			    s = ParameterizedTriggerUtils.readFileToString(artifact);
			}

			if (s == null) {
				FilePath workspace = build.getWorkspace();
				if (workspace == null) {
					listener.getLogger().printf(Plugin.LOG_TAG + " Could not load workspace of build %s%n", build.getFullDisplayName());
				} else {
					FilePath f = workspace.child(file);
					if (f.exists()) {
						s = ParameterizedTriggerUtils.readFileToString(f, getEncoding());
					}
				}
			}

			if (s == null) {
				listener.getLogger().println(Plugin.LOG_TAG + " Properties file "
						+ file + " did not exist.");
				if (getFailTriggerOnMissing()) {
					listener.getLogger().println("Not triggering due to missing file - did you archive it as a build artifact ?");
					throw new DontTriggerException();
				}
				// goto next file.
				continue;
			}

			s = env.expand(s);
			Properties p = ParameterizedTriggerUtils.loadProperties(s);

			for (Map.Entry<Object, Object> entry : p.entrySet()) {
				// support multi-line parameters correctly
				s = entry.getValue().toString();
				if(textParamValueOnNewLine && s.contains("\n")) {
					values.add(new TextParameterValue(entry.getKey().toString(), s));
				} else {
					values.add(new StringParameterValue(entry.getKey().toString(), s));
				}
			}
		}
		return values;
	}

	private Collection<? extends AbstractBuild<?, ?>> getTargetBuilds(AbstractBuild<?, ?> build) {
		if ((build instanceof MatrixBuild) && isUseMatrixChild()) {
			return Collections2.filter(
				isOnlyExactRuns()?((MatrixBuild)build).getExactRuns():((MatrixBuild)build).getRuns(),
				new Predicate<MatrixRun>() {
					public boolean apply(@Nullable MatrixRun run) {
						if (run == null) {
							return false;
						}
						if (StringUtils.isBlank(getCombinationFilter())) {
							// no combination filter stands for all children.
							return true;
						}
						Combination c = run.getParent().getCombination();
						AxisList axes = run.getParent().getParent().getAxes();
						
						return c.evalGroovyExpression(axes, getCombinationFilter());
					}
				}
			);
		} else {
			return Arrays.<AbstractBuild<?,?>>asList(build);
		}
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

	public boolean getTextParamValueOnNewLine() {
		return textParamValueOnNewLine;
	}

	public boolean isUseMatrixChild() {
		return useMatrixChild;
	}
	
	public String getCombinationFilter() {
		return combinationFilter;
	}
	
	public boolean isOnlyExactRuns() {
		return onlyExactRuns;
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
				} catch(UnsupportedCharsetException e) {
					return FormValidation.error("Unsupported Encoding");
				} catch(IllegalCharsetNameException e) {
					return FormValidation.error("Bad Encoding Name");
				}
			}
			return FormValidation.ok();
		}

		/**
		 * Check whether the configuring model is {@link MatrixProject}. Called from jelly.
		 * 
		 * Note: Caller should pass it for the model is not bound to
		 * {@link org.kohsuke.stapler.StaplerRequest#findAncestorObject(Class)}
		 * when called via hetero-list.
		 * 
		 * @param it Object to check
		 * @return true if the target model is {@link MatrixProject}
		 */
		public boolean isMatrixProject(Object it) {
			return (it != null) && (it instanceof MatrixProject);
		}
	}

}
