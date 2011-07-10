package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.util.EnumConverter;
import java.io.IOException;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        }
}
