package hudson.plugins.parameterizedtrigger;

import hudson.model.Result;

public enum ResultCondition {

	SUCCESS("Stable") {
		boolean isMet(Result result, Result previousResult) {
			return result == Result.SUCCESS;
		}
	},
	UNSTABLE("Unstable") {
    	boolean isMet(Result result, Result previousResult) {
			return result == Result.UNSTABLE;
    	}
	},
	FAILED_OR_BETTER("Stable, unstable or failed, but not aborted") {
    	boolean isMet(Result result, Result previousResult) {
    		return result.isBetterOrEqualTo(Result.FAILURE);
	  	}
	},
	UNSTABLE_OR_BETTER("Stable or unstable but not failed") {
        boolean isMet(Result result, Result previousResult) {
	  		return result.isBetterOrEqualTo(Result.UNSTABLE);
		}
	},
	UNSTABLE_OR_WORSE("Unstable or Failed but not stable") {
		boolean isMet(Result result, Result previousResult) {
			return result.isWorseOrEqualTo(Result.UNSTABLE);
		}
	},
	FAILED("Failed") {
		boolean isMet(Result result, Result previousResult) {
			return result == Result.FAILURE;
		}
	},
	FIXED("Fixed - Unstable or Failure previously. Success now.") {
		boolean isMet(Result result, Result previousResult) {
			if(result == Result.SUCCESS) {
				if(previousResult.equals(Result.UNSTABLE) || previousResult.equals(Result.FAILURE)) {
					return true;
				}
			}
			return false;
		}
	},
	ALWAYS("Complete (always trigger)") {
		boolean isMet(Result result, Result previousResult) {
			return true;
		}
	};

	private ResultCondition(String displayName) {
		this.displayName = displayName;
	}

	private final String displayName;
	
	public String getDisplayName() {
		return displayName;
	}

	abstract boolean isMet(Result result, Result previousResult);
	
}
