package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractProject;
import hudson.model.Action;

import java.util.List;

/**
 * Convert the given List<Action> into project specific actions.
 *
 * Eg, merging default project parameters into the supplied parameters and replacing StringParameterValues with
 * the project appropriate type.
 */
public abstract class AbstractProjectSpecificBuildActionsFactory {
    /**
     * Transform a list of build actions into project specific build actions
     *
     * @param actions
     *      The base set of actions to transform
     * @param project
     *      Project defintion used to transform the given set of actions
     *      
     * @return can be empty but never null.
     */
    public abstract List<Action> getProjectSpecificBuildActions(List<Action> baseActions, AbstractProject<?,?> project);
}
