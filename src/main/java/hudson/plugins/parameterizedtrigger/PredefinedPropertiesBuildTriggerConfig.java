package hudson.plugins.parameterizedtrigger;

import java.util.ArrayList;
import java.util.List;

/**
 * For backwards data compatibility
 */
@Deprecated
public class PredefinedPropertiesBuildTriggerConfig extends BuildTriggerConfig {

    public PredefinedPropertiesBuildTriggerConfig(String projects,
			ResultCondition condition, AbstractBuildParameters[] configs) {
		super(projects, condition, configs);
	}
    
	private String projectsValue;
    private String properties;
    private ResultCondition condition;
    private boolean triggerWithNoParameters;
    private boolean includeCurrentParameters;
    private String batchCondition;
    
    public Object readResolve() {
    	List<AbstractBuildParameters> configs = new ArrayList<AbstractBuildParameters>();
    	if (includeCurrentParameters) {
    		configs.add(new CurrentBuildParameters());
    	}
    	if (properties != null) {
    		configs.add(new PredefinedBuildParameters(properties));
    	}
		return new BuildTriggerConfig(projectsValue, condition, triggerWithNoParameters, configs);
    }
}
