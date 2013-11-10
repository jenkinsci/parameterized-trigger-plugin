package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;
import hudson.model.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuildStepHelper;

import com.google.common.collect.ImmutableList;

/**
 * Exposes the {@link SubProjectsAction} for maven projects. As Maven projects do not extend {@link Project}, project triggers are not displayed in the UI without this. (this could also be refactored
 * to work the same way for normal projects)
 * 
 * @see TriggerBuilder#getProjectActions(AbstractProject)
 * @author Dominik Bartholdi (imod)
 */
@Extension(optional = true)
public class TransientMavenSubProjectsActionFactory extends TransientProjectActionFactory {

    public TransientMavenSubProjectsActionFactory() {
    }

    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        if (MavenModuleSet.class.isAssignableFrom(target.getClass())) {
            final List<BlockableBuildTriggerConfig> configs = new ArrayList<BlockableBuildTriggerConfig>();

            MavenModuleSet ms = (MavenModuleSet) target;

            final List<TriggerBuilder> builders = getTriggerBuilders(ms);

            if (Plugin.isConditionalBuildStepInstalled()) {
                builders.addAll(getTriggerBuildersFromConditionalBuilder(ms));
            }

            for (TriggerBuilder builder : builders) {
                configs.addAll(builder.getConfigs());
            }

            return ImmutableList.of(new SubProjectsAction(target, configs));
        }
        return null;
    }

    private List<TriggerBuilder> getTriggerBuilders(MavenModuleSet ms) {
        final List<TriggerBuilder> builders = ms.getPrebuilders().getAll(TriggerBuilder.class);
        builders.addAll(ms.getPostbuilders().getAll(TriggerBuilder.class));
        return builders;
    }

    private List<TriggerBuilder> getTriggerBuildersFromConditionalBuilder(MavenModuleSet ms) {
        return ConditionalBuildStepHelper.getContainedBuilders(ms, TriggerBuilder.class);
    }
}
