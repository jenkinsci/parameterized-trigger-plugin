package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ParametersAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;

import java.util.List;
import java.util.ArrayList;

/**
 * Ensure the given project's parameters with default values exist in the parameter list.
 *
 * If they do not, append them with the specified default value.
 */
public class DefaultParameterValuesActionsTransform implements ITransformProjectParametersAction {
    public ParametersAction transformParametersAction(ParametersAction a, AbstractProject<?,?> project) {
        return ParameterizedTriggerUtils.mergeParameters(getDefaultParameters(project), (ParametersAction)a);
    }

    private static ParametersAction getDefaultParameters(AbstractProject<?,?> project) {
        ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);

        if (property == null) {
            return new ParametersAction();
        }

        List<ParameterValue> parameters = new ArrayList<ParameterValue>();
        for (ParameterDefinition pd : property.getParameterDefinitions()) {
            ParameterValue param = pd.getDefaultParameterValue();
            if (param != null) {
                parameters.add(param);
            }
        }

        return new ParametersAction(parameters);
    }
}
