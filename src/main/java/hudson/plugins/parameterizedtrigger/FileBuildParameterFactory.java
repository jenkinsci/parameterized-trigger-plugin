/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.LogTaskListener;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;


/**
 *
 * @author shoughton
 */
public class FileBuildParameterFactory extends AbstractBuildParameterFactory {

    private final String filePattern;

    @DataBoundConstructor
    public FileBuildParameterFactory(String filePattern) {
        this.filePattern = filePattern;
    }

    public String getFilePattern() {
        return filePattern;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        
        List<AbstractBuildParameters> result = Lists.newArrayList();
        
        try {
            FilePath workspace = getWorkspace(build);
            FilePath[] files = workspace.list(getFilePattern());
            for(FilePath f: files) {
                String parametersStr = f.readToString();
                Logger.getLogger(FileBuildParameterFactory.class.getName()).log(Level.INFO, null, "Triggering build with " + f.getBaseName());
                result.add(new PredefinedBuildParameters(parametersStr));
            }

        } catch (Exception ex) {
            Logger.getLogger(FileBuildParameterFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private FilePath getWorkspace(AbstractBuild build) {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            workspace = build.getProject().getSomeWorkspace();
        }
        return workspace;
    }
    
    //private PredefinedBuildParameters getParameterBlock() {
    //    String stringWithCount = Util.replaceMacro(paramExpr, ImmutableMap.of("CHANGELIST", changelist.toString()));
    //    return new PredefinedBuildParameters(stringWithCount);
    //}

    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.FileBuildParameterFactory_FileBuildParameterFactory();
        }
    }
}
