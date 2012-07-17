package hudson.plugins.parameterizedtrigger.matrix;

import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.model.InvisibleAction;

/**
 * Restricts the execution of matrix to a subset at runtime.
 *
 * @author Kohsuke Kawaguchi
 */
public class MatrixSubsetAction extends InvisibleAction {
    /**
     * Filter Groovy expression to be run in {@link Combination#evalGroovyExpression(AxisList, String)}
     */
    private final String filter;

    public MatrixSubsetAction(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }
}
