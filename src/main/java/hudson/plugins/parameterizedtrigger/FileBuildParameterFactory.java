/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.VariableResolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
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

//    @Override
    public List<hudson.plugins.parameterizedtrigger.AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        
        try {
            FilePath workspace = getWorkspace(build);
            
            return workspace.act(new FilePath.FileCallable<List<AbstractBuildParameters> >() {

                public List<AbstractBuildParameters> invoke(File ws, VirtualChannel channel) throws IOException {
                    final long nowSlave = System.currentTimeMillis();
                    FileSet fs = Util.createFileSet(new File(ws, ""), getFilePattern());
                    DirectoryScanner ds = fs.getDirectoryScanner();
                    String[] files = ds.getIncludedFiles();

                    if (files.length == 0) {
                        // no test result. Most likely a configuration error or fatal problem
                        return null;

                    }
                    
                    ArrayList<AbstractBuildParameters> params = Lists.newArrayList();
                    
                    for(int i=0; i < files.length; ++i) {
                        BufferedReader reader = null;
                        
                        try {
                            String filename = ds.getBasedir() + System.getProperty("file.separator") + files[i];
                            Logger.getLogger(FileBuildParameterFactory.class.getName()).log(Level.INFO, null, "Reading parameter file " + files[i]);
                            reader = new BufferedReader(new FileReader(filename));
                            String text = null;
                            StringBuffer contents = new StringBuffer();
                            while((text = reader.readLine()) != null) {
                                contents.append(text).append(System.getProperty("line.separator"));
                            }

                            params.add(new PredefinedBuildParameters(contents.toString()));
                            
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            } catch (IOException e) {
                            e.printStackTrace();
                            } finally {
                            try {
                                if (reader != null) {
                                reader.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    return params;
                }

            });

        } catch (Exception ex) {
            Logger.getLogger(FileBuildParameterFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
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

    private static final VariableResolver<String> EMPTY_STRING_VARIABLE_RESOLVER = new VariableResolver<String>() {

        @Override
        public String resolve(String name) {
            return "";
        }
    };
    
     public String getFilePattern() {
        return filePattern;
    }
   
}
