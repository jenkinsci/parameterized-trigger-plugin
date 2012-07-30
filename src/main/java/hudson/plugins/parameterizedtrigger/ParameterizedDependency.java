package hudson.plugins.parameterizedtrigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.TaskListener;

import java.util.List;

/**
 * Invoke downstream projects with applicable parameters using
 * DependencyGraph.Dependency interface.
 * @author Alan Harder
 */
public class ParameterizedDependency extends Dependency {
	private BuildTriggerConfig config;

	public ParameterizedDependency(AbstractProject upstream, AbstractProject downstream,
			BuildTriggerConfig config) {
		super(upstream, downstream);
		this.config = config;
	}

	public static void add(AbstractProject upstream, AbstractProject downstream,
			BuildTriggerConfig config, DependencyGraph graph) {
		// Keeping graph.addDependency() call in this class so classloader
		// won't look for DependencyGraph.Dependency when running on older Hudson
		graph.addDependency(new ParameterizedDependency(upstream, downstream, config));
	}

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            
            final ParameterizedDependency that = (ParameterizedDependency) obj;
            return this.getUpstreamProject() == that.getUpstreamProject() || this.getDownstreamProject() == that.getDownstreamProject()
                || this.config == that.config;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.getUpstreamProject().hashCode();
            hash = 23 * hash + this.getDownstreamProject().hashCode();
            hash = 23 * hash + this.config.hashCode();
            return hash;
        }

	@Override
	public boolean shouldTriggerBuild(AbstractBuild build, TaskListener listener, List<Action> actions) {
		if (!config.getCondition().isMet(build.getResult())){
			return false;
		}
		try {
			List<Action> actionList = config.getBaseActions(build, listener);
			if (!actionList.isEmpty()) {
				actions.addAll(config.getBuildActions(actionList, getDownstreamProject()));
				return true;
			}

            if (config.getTriggerWithNoParameters()) {
                return true;
            }
            listener.getLogger().println("[parameterized-trigger] Downstream builds will not be triggered as no parameter is set.");
            return false;
		} catch (AbstractBuildParameters.DontTriggerException ex) {
			// don't trigger on this configuration
			return false;
		} catch (Exception ex) {
			listener.error("Failed to build parameters to trigger project: "
				+ getDownstreamProject().getName());
			ex.printStackTrace(listener.getLogger());
			return false;
		}
	}
}
