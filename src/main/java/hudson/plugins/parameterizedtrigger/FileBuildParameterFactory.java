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
 * @author Chris Johnson
 */
public class FileBuildParameterFactory extends AbstractBuildParameterFactory {

    /**
     *  Enum containing the action that could occur when there are no files found in the workspace
     *
     */
    public enum NoFilesFoundEnum {
        SKIP("Don't trigger these projects"){ // previous behaviour (default)
            @Override
            public void failCheck(TaskListener listener) throws AbstractBuildParameters.DontTriggerException {
                listener.getLogger().println(Messages.FileBuildParameterFactory_NoFilesFoundSkipping());
                throw new AbstractBuildParameters.DontTriggerException();
        }},
        NOPARMS("Skip these parameters"){
            @Override
            public void failCheck(TaskListener listener) throws AbstractBuildParameters.DontTriggerException {
                listener.getLogger().println(Messages.FileBuildParameterFactory_NoFilesFoundIgnore());
        }},
        FAIL("Fail the build step"){
            @Override
            public void failCheck(TaskListener listener) throws AbstractBuildParameters.DontTriggerException {
                listener.getLogger().println(Messages.FileBuildParameterFactory_NoFilesFoundTerminate());
                throw new RuntimeException();
        }};

        private String description;

        public String getDescription() {
            return description;
        }

        NoFilesFoundEnum(String description) {
            this.description = description;
        }

        public abstract void failCheck(TaskListener listener) throws AbstractBuildParameters.DontTriggerException;
    }

    private final String filePattern;
    private final NoFilesFoundEnum noFilesFoundAction;

    @DataBoundConstructor
    public FileBuildParameterFactory(String filePattern, NoFilesFoundEnum noFilesFoundAction) {
        this.filePattern = filePattern;
        this.noFilesFoundAction = noFilesFoundAction;
    }

    public FileBuildParameterFactory(String filePattern) {
        this(filePattern, NoFilesFoundEnum.SKIP);
    }

    public String getFilePattern() {
        return filePattern;
    }

    public NoFilesFoundEnum getNoFilesFoundAction() {
        return noFilesFoundAction;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {

        List<AbstractBuildParameters> result = Lists.newArrayList();

        try {
            FilePath workspace = getWorkspace(build);
            FilePath[] files = workspace.list(getFilePattern());
            if(files.length == 0) {
                noFilesFoundAction.failCheck(listener);
            } else {
                for(FilePath f: files) {
                    String parametersStr = f.readToString();
                    Logger.getLogger(FileBuildParameterFactory.class.getName()).log(Level.INFO, null, "Triggering build with " + f.getName());
                    result.add(new PredefinedBuildParameters(parametersStr));
                }
            }
        } catch (IOException ex) {
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

    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.FileBuildParameterFactory_FileBuildParameterFactory();
        }
    }
}
