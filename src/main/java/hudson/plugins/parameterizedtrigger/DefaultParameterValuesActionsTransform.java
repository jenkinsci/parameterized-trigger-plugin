package hudson.plugins.parameterizedtrigger;

import hudson.model.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Ensure the given project's parameters with default values exist in the parameter list.
 *
 * If they do not, append them with the specified default value.
 */
public class DefaultParameterValuesActionsTransform implements ITransformProjectParametersAction {
    public ParametersAction transformParametersAction(ParametersAction a, Job<?,?> project) {
        return ParameterizedTriggerUtils.mergeParameters(getDefaultParameters(project), a);
    }

    private static ParametersAction getDefaultParameters(Job<?,?> project) {

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
