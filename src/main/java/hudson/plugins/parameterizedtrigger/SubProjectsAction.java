package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableList;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.model.Action;
import hudson.tasks.BuildStep;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
/**
 * Action added Projects to track what projects are
 * were triggered from the trigger builds on other projects buildstep.
 *
 * Provides a section on the project page indicating the triggered projects.
 * see jobMain.groovy
 *
 * @author wolfs
 */
public class SubProjectsAction implements Action {

    private AbstractProject<?,?> project;
    private List<BlockableBuildTriggerConfig> configs;

    public SubProjectsAction(AbstractProject<?,?> project, List<BlockableBuildTriggerConfig> configs) {
        this.project = project;
        this.configs = configs;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public List<BlockableBuildTriggerConfig> getConfigs() {
        if(Plugin.isConditionalBuildStepInstalled()) {
            // if the conditional buildstep plugin is installed, a TriggerBuilder can be wrapped within a condtional buildstep
            // we need to get the config of these too
            List<BlockableBuildTriggerConfig> all = new ArrayList<BlockableBuildTriggerConfig>();
            all.addAll(configs);
            all.addAll(getConditionalTriggerConfigs());
            return all;
        } 
        return configs;
    }

    public List<SubProjectsAction> getSubProjectActions() {
        if (isFirst()) {
            return project.getActions(SubProjectsAction.class);
        }
        return ImmutableList.of();
    }

    public AbstractProject<?,?> getProject() {
        return project;
    }

    private boolean isFirst() {
        return project.getAction(SubProjectsAction.class) == this;
    }
    
    
    /**
     * Gets the configurations of the TriggerBuilder's wrapped within the conditional buildsteps of the current project
     * @return list of configs 
     */
    private List<BlockableBuildTriggerConfig> getConditionalTriggerConfigs(){
        List<BlockableBuildTriggerConfig> all = new ArrayList<BlockableBuildTriggerConfig>();
        List<SingleConditionalBuilder> cSingleBuilders = ((Project)project).getBuildersList().getAll(SingleConditionalBuilder.class);
        for (SingleConditionalBuilder conditionalBuilder : cSingleBuilders) {
            BuildStep bs = conditionalBuilder.getBuildStep();
            if(bs instanceof TriggerBuilder) {
                TriggerBuilder ctb = (TriggerBuilder)bs;
                if (ctb != null) {
                    all.addAll(ctb.getConfigs());
                }
            }
        }
        if(project instanceof Project<?, ?>){
            List<ConditionalBuilder> cbuilders = ((Project)project).getBuildersList().getAll(ConditionalBuilder.class);
            for (ConditionalBuilder conditionalBuilder : cbuilders) {
                final List<BuildStep> cbs = conditionalBuilder.getConditionalbuilders();
                for (BuildStep buildStep : cbs) {
                    if(buildStep instanceof TriggerBuilder) {
                        TriggerBuilder ctb = (TriggerBuilder)buildStep;
                        if (ctb != null) {
                            all.addAll(ctb.getConfigs());
                        }
                    }
                }
            }
        }
        return all;
    }
    
}
