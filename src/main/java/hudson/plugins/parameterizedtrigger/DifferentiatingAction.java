package hudson.plugins.parameterizedtrigger;

import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Queue;
import java.util.List;

/**
 * Force unique scheduling of the child job.
 *
 * <p>
 * Queue in Jenkins try to merge together two "identical" tasks into one. For example, if you try to trigger
 * the same project twice while it is still in the queue, you'll only see one getting invoked.
 *
 * <p>
 * This is most likely harmful in the context of the parameterized trigger when used in the subroutine semantics,
 * where you expect what you triggered to fire regardless of other things going on. This hidden "hack" parameter
 * prevents the submitted task from getting merged with others.
 *
 * @author Kohsuke Kawaguchi
 */
public class DifferentiatingAction extends InvisibleAction implements Queue.QueueAction {
    public boolean shouldSchedule(List<Action> actions) {
        return true;
    }
}
