package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.Arrays;

public enum ResultCondition {

	SUCCESS("Stable") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			return build.getResult() == Result.SUCCESS;
		}
	},
    UNSTABLE("Unstable") {
        boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
            return build.getResult() == Result.UNSTABLE;
        }
    },
	UNSTABLE_OR_BETTER("Stable or unstable but not failed") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			return build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
		}
	},
    UNSTABLE_OR_WORSE("Unstable or Failed but not stable") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			return build.getResult().isWorseOrEqualTo(Result.UNSTABLE);
		}
	},
	FAILED("Failed") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			return build.getResult() == Result.FAILURE;
		}
	},
	ALWAYS("Complete (always trigger)") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			return true;
		}
	},
	EXPLICIT("Explitly requested (by environment variable)") {
		boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException {
			EnvVars env = build.getEnvironment(listener);
			String name = downstream.getName();

			// See if there is a variable with the same name as the downstream job
			if ("true".equals(env.get(name))) {
				return true;
			}

			// See if the downstream job name is listed in the ALL_JOBS_NAME_VARIABLE variable
			String jobs = env.get("TRIGGER_JOB_NAMES");
			if (jobs != null) {
				for (String job : jobs.trim().split("\\s*,\\s*")) {
					if (name.equals(job)) {
						return true;
					}
				}
			}
			return false;
		}
	};

	private ResultCondition(String displayName) {
		this.displayName = displayName;
	}

	private final String displayName;

	public String getDisplayName() {
		return displayName;
	}

	abstract boolean isMet(AbstractBuild<?, ?> build, TaskListener listener, AbstractProject<?, ?> downstream) throws IOException, InterruptedException;
}
