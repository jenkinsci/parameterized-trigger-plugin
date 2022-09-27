package hudson.plugins.parameterizedtrigger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

/**
 * For backwards data compatibility
 */
@Deprecated
public class FileBuildTriggerConfig extends BuildTriggerConfig {

    public FileBuildTriggerConfig(String projects,
			ResultCondition condition, AbstractBuildParameters[] configs) {
		super(projects, condition, configs);
	}
    
	private String projectsValue;
    private String propertiesFile;
    private ResultCondition condition;
    private boolean triggerWithNoParameters;
    private boolean includeCurrentParameters;
	
    public Object readResolve() {
    	List<AbstractBuildParameters> configs = new ArrayList<>();
    	if (includeCurrentParameters) {
    		configs.add(new CurrentBuildParameters());
    	}
    	if (propertiesFile != null) {
    		configs.add(new FileBuildParameters(propertiesFile));
    	}
		return new BuildTriggerConfig(projectsValue, condition, triggerWithNoParameters, configs);
    }
}
