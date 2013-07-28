package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.tasks.BuildStep;
import hudson.util.EnumConverter;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
import org.kohsuke.stapler.Stapler;

public class Plugin extends hudson.Plugin {

	@Override
	public void start() throws Exception {
		Stapler.CONVERT_UTILS.register(new EnumConverter(),
				ResultCondition.class);
	}

        /**
         * If a job is renamed, update all parameterized-triggers with the new name.
         */
        @Extension
        public static final class RenameListener extends ItemListener {
            @Override
            public void onRenamed(Item item, String oldName, String newName) {
                for (Project<?,?> p : Hudson.getInstance().getProjects()) {
                    boolean changed = false;
                    //iterate over post build triggers
                    BuildTrigger bt = p.getPublishersList().get(BuildTrigger.class);
                    if (bt != null) {
                        for (BuildTriggerConfig c : bt.getConfigs()){
                            changed |= c.onJobRenamed(oldName, newName);
                        }
                    }
                    //iterate over build step triggers
                    TriggerBuilder tb = p.getBuildersList().get(TriggerBuilder.class);
                    if (tb != null) {
                    	for (BuildTriggerConfig co : tb.getConfigs()){
                    		changed |= co.onJobRenamed(oldName, newName);
                    	}
                    }
                    
                    //iterate the BuildTriggers within conditional buildsteps
                    if(isConditionalBuildStepInstalled()) {
                        changed |= renameInConditionalBuildStep(p, oldName, newName);
                    }
                    
                    //if something changed, save the project
                    if (changed){
	                   	try {
	                    	p.save();
	                    } catch (IOException e) {
	                    	Logger.getLogger(RenameListener.class.getName()).log(Level.WARNING,
	                    			"Failed to persist project setting during rename from "+oldName+" to "+newName, e);
	                    }
                    }
                    
                }
            }

            @Override
            public void onDeleted(Item item) {
                for (Project<?,?> p : Hudson.getInstance().getProjects()) {
                    String oldName = item.getName();
                    boolean changed = false;
                    //iterate over post build triggers
                    BuildTrigger bt = p.getPublishersList().get(BuildTrigger.class);
                    if (bt != null) {
                        for (ListIterator<BuildTriggerConfig> btc = bt.getConfigs().listIterator(); btc.hasNext();) {
                            BuildTriggerConfig c = btc.next();
                            if (c.onDeleted(oldName)) {
                                changed = true;
                                if (c.getProjects().length() == 0){
                                    btc.remove();
                                }
                            }
                        }
                    }
                  //iterate over build step triggers
                    TriggerBuilder tb = p.getBuildersList().get(TriggerBuilder.class);
                    if (tb != null) {
                        for (ListIterator<BlockableBuildTriggerConfig> bbtc = tb.getConfigs().listIterator(); bbtc.hasNext();) {
                            BuildTriggerConfig c = bbtc.next();
                            if (c.onDeleted(oldName)) {
                                changed = true;
                                if (c.getProjects().length() == 0){
                                    bbtc.remove();
                                }
                            }
                        }
                    }
                    
                    //iterate the BuildTriggers within conditional buildsteps
                    if(isConditionalBuildStepInstalled()) {
                        changed |= deleteInConditionalBuildStep(p, oldName);
                    }
                    
                    //if something changed, save the project
                    if (changed){
	                    try {
	                    	if (bt!=null && bt.getConfigs().isEmpty()){
	                    		p.getPublishersList().remove(bt);
	                    	}
	                    	if (tb!=null && tb.getConfigs().isEmpty()){
	                    		p.getBuildersList().remove(tb);
	                    	}
	                    	p.save();
	                    } catch (IOException e) {
	                    	Logger.getLogger(RenameListener.class.getName()).log(Level.WARNING,
	                    			"Failed to persist project setting during remove of "+oldName, e);
	                    }
                    }
                }
            }
            
            
            /**
             * renames the project references within all {@link TriggerBuilder}s which are wrapped by a conditional buildsteps
             * @param p the project the check
             * @param oldName the old project name
             * @param newName the new project name
             * @return whether a change has been made
             */
            private boolean renameInConditionalBuildStep(Project<?,?> p, String oldName, String newName) {
                boolean changed = false;
                List<ConditionalBuilder> cbuilders = p.getBuildersList().getAll(ConditionalBuilder.class);
                for (ConditionalBuilder conditionalBuilder : cbuilders) {
                    final List<BuildStep> cbs = conditionalBuilder.getConditionalbuilders();
                    for (BuildStep buildStep : cbs) {
                        if(buildStep instanceof TriggerBuilder) {
                            TriggerBuilder ctb = (TriggerBuilder)buildStep;
                            if (ctb != null) {
                                for (BuildTriggerConfig co : ctb.getConfigs()){
                                    changed |= co.onJobRenamed(oldName, newName);
                                }
                            }
                        }
                    }
                }
                
                
                List<SingleConditionalBuilder> scb = p.getBuildersList().getAll(SingleConditionalBuilder.class);
                for (SingleConditionalBuilder singleConditionalBuilder : scb) {
                    final BuildStep buildStep = singleConditionalBuilder.getBuildStep();
                    if(buildStep instanceof TriggerBuilder) {
                        TriggerBuilder ctb = (TriggerBuilder)buildStep;
                        if (ctb != null) {
                            for (BuildTriggerConfig co : ctb.getConfigs()){
                                changed |= co.onJobRenamed(oldName, newName);
                            }
                        }
                    }                        
                }
                return changed;
            }
            
            /**
             * removes the project references within all {@link TriggerBuilder}s which are wrapped by a conditional buildsteps
             * @param p the project the check
             * @param oldName the old project name
             * @return whether a change has been made
             */
            private boolean deleteInConditionalBuildStep(Project<?,?> p, String oldName) {
                boolean changed = false;
                
                List<ConditionalBuilder> cbuilders = p.getBuildersList().getAll(ConditionalBuilder.class);
                for (ConditionalBuilder conditionalBuilder : cbuilders) {
                    final List<BuildStep> cbs = conditionalBuilder.getConditionalbuilders();
                    for (BuildStep buildStep : cbs) {
                        if(buildStep instanceof TriggerBuilder) {
                            TriggerBuilder tb = (TriggerBuilder)buildStep;
                            for (ListIterator<BlockableBuildTriggerConfig> bbtc = tb.getConfigs().listIterator(); bbtc.hasNext();) {
                                BuildTriggerConfig c = bbtc.next();
                                if (c.onDeleted(oldName)) {
                                    changed = true;
                                    if (c.getProjects().length() == 0){
                                        bbtc.remove();
                                    }
                                }
                            }
                        }
                    }
                }
                
                
                List<SingleConditionalBuilder> scb = p.getBuildersList().getAll(SingleConditionalBuilder.class);
                for (SingleConditionalBuilder singleConditionalBuilder : scb) {
                    final BuildStep buildStep = singleConditionalBuilder.getBuildStep();
                    if(buildStep instanceof TriggerBuilder) {
                        TriggerBuilder tb = (TriggerBuilder)buildStep;
                        for (ListIterator<BlockableBuildTriggerConfig> bbtc = tb.getConfigs().listIterator(); bbtc.hasNext();) {
                            BuildTriggerConfig c = bbtc.next();
                            if (c.onDeleted(oldName)) {
                                changed = true;
                                if (c.getProjects().length() == 0){
                                    bbtc.remove();
                                }
                            }
                        }

                    }                        
                }
                return changed;
            }
            
        }
        
        public static boolean isConditionalBuildStepInstalled(){
            final hudson.Plugin plugin = Jenkins.getInstance().getPlugin("conditional-buildstep");
            return plugin != null ? plugin.getWrapper().isActive() : false;
        }
}
