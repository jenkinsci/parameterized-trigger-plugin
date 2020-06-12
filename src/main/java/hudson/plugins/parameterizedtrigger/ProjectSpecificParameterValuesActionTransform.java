package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.RuntimeException;

/**
 * Convert Generic ParameterValues to the type indicated by the Project's ParameterDefinitions
 */
public class ProjectSpecificParameterValuesActionTransform implements ITransformProjectParametersAction {
    public ParametersAction transformParametersAction(ParametersAction a, Job<?,?> project) {
        Map<String, ParameterDefinition> parameterDefinitions = 
            getParameterDefinitionsMap(project);

        List<ParameterValue> params = new ArrayList<ParameterValue>();
        for (ParameterValue param : a.getParameters()) {
            params.add(convertToDefinedType(parameterDefinitions, param));
        }

        /* Add default values from defined params in the target job */
        return new ParametersAction(params);
    }

    private static Map<String, ParameterDefinition> getParameterDefinitionsMap(Job<?,?> project) {
        HashMap<String, ParameterDefinition> m = new HashMap<String, ParameterDefinition>();

        ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);

        if (property != null) {
            for (ParameterDefinition pd : property.getParameterDefinitions()) {
                m.put(pd.getName(), pd);
            }
        }

        return m;
    }

    private static boolean canConvert(ParameterDefinition def, ParameterValue v) {
        return def instanceof SimpleParameterDefinition &&
            !(def instanceof StringParameterDefinition) &&
            v.getClass().equals(StringParameterValue.class);
    }

    private static String getCurrentBuildName() {
        Executor e = Executor.currentExecutor();
        if(e == null) {
            return null;
        }
        
        Queue.Executable task = e.getCurrentExecutable();
        if(task == null || !(task instanceof AbstractBuild)) {
            return null;
        }
        
        return ((AbstractBuild<?,?>)task).getFullDisplayName();
    }

    private static ParameterValue convertToDefinedType(Map<String, ParameterDefinition> defs, ParameterValue pv) {
        String name = pv.getName();

        if (defs.containsKey(name)) {
            ParameterDefinition def = defs.get(name);

            if(canConvert(def, pv)) {
                try {
                    StringParameterValue spv = (StringParameterValue) pv;
                    Object value = (spv).getValue();
                    return ((SimpleParameterDefinition)def).createValue((String)value);
                } catch (RuntimeException e) {
                    if (System.getProperty("hudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode","false").equals("true")) {
                        String buildName = getCurrentBuildName();
                        Logger.getLogger(ProjectSpecificParameterValuesActionTransform.class.getName())
                            .log(Level.WARNING,
                                 String.format(
                                         "Ignoring RuntimeException thrown while converting StringParameterValue %s on %s. Falling back to original value.",
                                         pv.getName(),
                                         (buildName != null)?buildName:"PROJECT_CANNOT_RESOLVED"
                                 ),
                                 e);
                        return pv;
                    } else {
                        throw e;
                    }
                }
            }
        }

        return pv;
    }
}
