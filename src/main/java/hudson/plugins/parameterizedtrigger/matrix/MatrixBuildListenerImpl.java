package hudson.plugins.parameterizedtrigger.matrix;

import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.listeners.MatrixBuildListener;

/**
 * {@link MatrixBuildListener} that chooses the subset to run based on {@link MatrixSubsetAction}
 * @author Kohsuke Kawaguchi
 */
@Extension
public class MatrixBuildListenerImpl extends MatrixBuildListener {
    @Override
    public boolean doBuildConfiguration(MatrixBuild b, MatrixConfiguration c) {
        MatrixSubsetAction a = b.getAction(MatrixSubsetAction.class);
        if (a==null)    return true;

        // run the filter and restrict the subset to run
        return c.getCombination().evalGroovyExpression(b.getParent().getAxes(),a.getFilter());
    }
}
