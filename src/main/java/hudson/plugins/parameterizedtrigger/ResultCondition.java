package hudson.plugins.parameterizedtrigger;

import hudson.model.Result;
import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkins_ci.plugins.run_condition.core.StatusCondition;

/* StatusCondition - The current build status
    must be equal to, or better than the Worst status and equal to,
    or worse than the Best status for the build step to run

    public StatusCondition(final Result worstResult, final Result bestResult)
*/

public enum ResultCondition {

	SUCCESS("Stable") {
		boolean isMet(Result result) {
			return result == Result.SUCCESS;
		}
        RunCondition getRuncondition() {
            return new StatusCondition(Result.SUCCESS, Result.SUCCESS);
        }
	},
    UNSTABLE("Unstable") {
        boolean isMet(Result result) {
            return result == Result.UNSTABLE;
        }
        RunCondition getRuncondition() {
            return new StatusCondition(Result.UNSTABLE, Result.UNSTABLE);
        }
    },
	UNSTABLE_OR_BETTER("Stable or unstable but not failed") {
		boolean isMet(Result result) {
			return result.isBetterOrEqualTo(Result.UNSTABLE);
		}
        RunCondition getRuncondition() {
            return new StatusCondition(Result.UNSTABLE, Result.SUCCESS);
        }
	},
    UNSTABLE_OR_WORSE("Unstable or Failed but not stable") {
		boolean isMet(Result result) {
			return result.isWorseOrEqualTo(Result.UNSTABLE);
		}
        RunCondition getRuncondition() {
            return new StatusCondition(Result.ABORTED, Result.UNSTABLE);
        }
	},
	FAILED("Failed") {
		boolean isMet(Result result) {
			return result == Result.FAILURE;
		}
        RunCondition getRuncondition() {
            return new StatusCondition(Result.FAILURE, Result.FAILURE);
        }
	},
	ALWAYS("Complete (always trigger)") {
		boolean isMet(Result result) {
			return true;
		}
        RunCondition getRuncondition() {
            return new AlwaysRun();
        }
	},
    UNUSED("UNUSED default for new versions") {
		boolean isMet(Result result) {
			return false;
		}
        /* same as default */
        RunCondition getRuncondition() {
            return new AlwaysRun();
        }

	};

	private ResultCondition(String displayName) {
		this.displayName = displayName;
	}

	private final String displayName;

	public String getDisplayName() {
		return displayName;
	}

	abstract boolean isMet(Result result);
    /**
        Method to return relevant runcondition for older plugin versions.
    **/
    abstract RunCondition getRuncondition();

}
