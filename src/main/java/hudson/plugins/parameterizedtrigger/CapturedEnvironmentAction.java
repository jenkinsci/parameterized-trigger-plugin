package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.model.Action;
import hudson.model.InvisibleAction;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CapturedEnvironmentAction extends InvisibleAction {

    private final EnvVars env;

    public CapturedEnvironmentAction(EnvVars env) {
        this.env = env;
    }

    public EnvVars getCapturedEnvironment() {
        return env;
    }
}
