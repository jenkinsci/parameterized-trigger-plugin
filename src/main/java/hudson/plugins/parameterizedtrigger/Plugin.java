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
                    BuildTrigger t = p.getPublishersList().get(BuildTrigger.class);
                    if (t != null) {
                        boolean changed = false;
                        for (BuildTriggerConfig c : t.getConfigs())
                            changed |= c.onJobRenamed(oldName, newName);
                        if (changed) try {
                            p.save();
                        } catch (IOException e) {
                            Logger.getLogger(RenameListener.class.getName()).log(Level.WARNING,
                                    "Failed to persist project setting during rename from "+oldName+" to "+newName, e);
                        }
                    }
                }
            }

            @Override public void onDeleted(Item item) {
                for (Project<?,?> p : Hudson.getInstance().getProjects()) {
                    String oldName = item.getName();
                    BuildTrigger t = p.getPublishersList().get(BuildTrigger.class);
                    if (t != null) {
                        boolean changed = false;
                        for (ListIterator<BuildTriggerConfig> it = t.getConfigs().listIterator();
                             it.hasNext();) {
                            BuildTriggerConfig c = it.next();
                            if (c.onDeleted(oldName)) {
                                changed = true;
                                if (c.getProjects().length() == 0)
                                    it.remove();
                            }
                        }
                        if (changed) try {
                            if (t.getConfigs().size() == 0)
                                p.getPublishersList().remove(t);
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
