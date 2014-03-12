package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ParametersAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
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
    public ParametersAction transformParametersAction(ParametersAction a, AbstractProject<?,?> project) {
        Map<String, ParameterDefinition> parameterDefinitions = 
            getParameterDefinitionsMap(project);

        List<ParameterValue> params = new ArrayList<ParameterValue>();
        for (ParameterValue param : a.getParameters()) {
            params.add(convertToDefinedType(parameterDefinitions, param));
        }

        /* Add default values from defined params in the target job */
        return new ParametersAction(params);
    }

    private static Map<String, ParameterDefinition> getParameterDefinitionsMap(AbstractProject<?,?> project) {
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

    private static ParameterValue convertToDefinedType(Map<String, ParameterDefinition> defs, ParameterValue pv) {
        String name = pv.getName();

        if (defs.containsKey(name)) {
            ParameterDefinition def = defs.get(name);

            if(canConvert(def, pv)) {
                try {
                    return ((SimpleParameterDefinition)def).createValue(((StringParameterValue)pv).value);
                } catch (RuntimeException e) {
                    if (System.getProperty("hudson.plugins.parameterizedtrigger.ProjectSpecificParametersActionFactory.compatibility_mode","false").equals("true")) {
                        Logger.getLogger(ProjectSpecificParameterValuesActionTransform.class.getName())
                            .log(Level.WARNING,
                                 "Ignoring RuntimeException thrown while converting StringParameterValue. Falling back to original value.",
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
