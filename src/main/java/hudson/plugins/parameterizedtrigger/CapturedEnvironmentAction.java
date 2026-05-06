package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CapturedEnvironmentAction extends InvisibleAction implements RunAction2 {

    private transient volatile EnvVars env;

    public CapturedEnvironmentAction(EnvVars env) {
        this.env = env;
    }

    public EnvVars getCapturedEnvironment() {
        return env;
    }

    @Override
    public void onAttached(final Run<?, ?> r) {
        // noop
    }

    @Override
    public void onLoad(final Run<?, ?> r) {
        env = new EnvVars();
    }
}
