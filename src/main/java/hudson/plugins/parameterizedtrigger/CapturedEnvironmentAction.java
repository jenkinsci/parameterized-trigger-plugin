package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import java.util.Collections;
import jenkins.model.RunAction2;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CapturedEnvironmentAction extends InvisibleAction implements RunAction2 {

    static final String OLD_DATA_MESSAGE =
            "The build.xml contains captured environment variables at the time of building which could contain sensitive data.";
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
        if (env != null) {
            OldDataMonitor.report(r, Collections.singletonList(new AssertionError(OLD_DATA_MESSAGE)));
        }
        // If it is not null then we loaded old data that needs to be cleaned, if it is null then it needs to be
        // something.
        env = new EnvVars();
    }
}
