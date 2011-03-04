package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.kohsuke.stapler.DataBoundConstructor;

public class FileBuildParameters extends AbstractBuildParameters {
	private static final Logger LOGGER = Logger.getLogger(FileBuildParameters.class.getName());

	private final String propertiesFile;
	private final boolean failTriggerOnMissing;
	private final String sourceNode;
	private final Map<String,String> nodeAxis;

	@DataBoundConstructor
	public FileBuildParameters(String propertiesFile, boolean failTriggerOnMissing, String sourceNode) {
		this.propertiesFile = propertiesFile;
		this.failTriggerOnMissing = failTriggerOnMissing;
		this.sourceNode = sourceNode;
		if(sourceNode != null){
			String[] matrix = StringUtils.split( sourceNode, "," );
			LOGGER.log(Level.INFO,"matrix =" + matrix);
			this.nodeAxis = new HashMap<String, String>();
			for(String axis : matrix){
				LOGGER.log(Level.INFO,"AXIS =" + axis);
				String[] vals = StringUtils.split(axis, "=");
				if(vals.length == 2){
					nodeAxis.put(vals[0], vals[1]);
				}
			}
			LOGGER.log(Level.INFO,"FileBuildParameter:" + this.nodeAxis);
		} else {
			/* Empty hash because we got nothing as sourceNode */
			this.nodeAxis = new HashMap<String, String>();
		}
	}

	public FileBuildParameters(String propertiesFile) {
		this(propertiesFile, false, null);
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

		final AbstractProject project = build.getProject();

		String resolvedPropertiesFile = env.expand(propertiesFile);
		final boolean isMatrixProject = MatrixBuild.class.isAssignableFrom(build.getClass());
		String[] allFiles = Util.tokenize(resolvedPropertiesFile, ",");
		List<ParameterValue> values = new ArrayList<ParameterValue>();

		for(String file:allFiles) {
			FilePath f = null;
			if(isMatrixProject){
				if(nodeAxis.isEmpty()){
					// No axis provided, check master first then look for it in a slace 
					listener.getLogger().println("[parameterizedtrigger] Trying master for property file: " + file + ".");
					f = build.getWorkspace().child(file);
				}
				// Either we found nothing on master or we had constraints
				if(f == null || !f.exists()) {
					if(nodeAxis.isEmpty()){
						listener.getLogger().println("[parameterizedtrigger] No file on master matching property file: " +
													 file + ".");
					}
					MatrixBuild matrixBuild = (MatrixBuild)build;

					List<MatrixRun> runs = matrixBuild.getRuns();
					for (MatrixRun run : runs) {
						// LOGGER.log(Level.INFO,"Got run VARS:" + run.getBuildVariables() + " isDone:" + run.isBuilding());
						// LOGGER.log(Level.INFO, "Build Set :" + run.getBuildVariables().entrySet() +
						// 		   " nodeAxis Set:" +this.nodeAxis.entrySet() );
						if(run.getBuildVariables().entrySet().containsAll(this.nodeAxis.entrySet())){
							f = run.getWorkspace().child(file);
							if(f != null && f.exists()) {
								// Stop at first match
								listener.getLogger().println("[parameterizedtrigger] Config: " +
															 run.getBuildVariables().entrySet() +
															 " has a matching property file for: " + file + ".");
								break;
							}
						}
					}
				}
			} else {
				f = build.getWorkspace().child(file);
			}
		
			if (f == null || !f.exists()) {
				listener.getLogger().println("[parameterizedtrigger] Properties file "
											 + file + " did not exist.");
				if(getFailTriggerOnMissing() == true) {
					listener.getLogger().println("Not triggering due to missing file");
					throw new DontTriggerException();
				}
				// goto next file.
				continue;
			}
			String s = f.readToString();
			s = env.expand(s);
			Properties p = new Properties();
			p.load(new StringInputStream(s));
			
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
	public String getSourceNode() {
		return sourceNode;
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
	}

}
