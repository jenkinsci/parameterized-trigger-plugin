package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.FileParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.FileBuildParameterFactory.NoFilesFoundEnum;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates a {@link FileParameterValue} for each matching file.
 *
 * @author Kohsuke Kawaguchi
 */
public class BinaryFileParameterFactory extends AbstractBuildParameterFactory {
    private final String parameterName;
    private final String filePattern;
    private final NoFilesFoundEnum noFilesFoundAction;

    @DataBoundConstructor
    public BinaryFileParameterFactory(String parameterName, String filePattern, NoFilesFoundEnum noFilesFoundAction) {
        this.parameterName = parameterName;
        this.filePattern = filePattern;
        this.noFilesFoundAction = noFilesFoundAction;
    }

    public BinaryFileParameterFactory(String parameterName, String filePattern) {
        this(parameterName,filePattern, NoFilesFoundEnum.SKIP);
    }

    public String getParameterName() {
        return parameterName;
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
            // save them into the master because FileParameterValue might need files after the slave workspace have disappeared/reused
            FilePath target = new FilePath(build.getRootDir()).child("parameter-files");
            int n = build.getWorkspace().copyRecursiveTo(getFilePattern(), target);

            if (n==0) {
                noFilesFoundAction.failCheck(listener);
            } else {
                for(final FilePath f: target.list(getFilePattern())) {
                    LOGGER.fine("Triggering build with " + f.getName());

                    result.add(new AbstractBuildParameters() {
                        @Override
                        public Action getAction(AbstractBuild<?,?> build, TaskListener listener) throws IOException, InterruptedException, DontTriggerException {
                            assert f.getChannel()==null;    // we copied files locally. This file must be local to the master
                            FileParameterValue fv = new FileParameterValue(parameterName, new File(f.getRemote()), f.getName());

                            if ($setLocation!=null) {
                                try {
                                    $setLocation.invoke(fv,parameterName);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    // be defensive as the core might change
                                }
                            }
                            return new ParametersAction(fv);
                        }
                    });
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to compute binary file parameters from " + getFilePattern(), ex);
        }

        return result;
    }


    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.BinaryFileParameterFactory_DisplayName();
        }
    }

    private static Method $setLocation;

    static {
        // work around NPE fixed in the core at 4a95cc6f9269108e607077dc9fd57f06e4c9af26
        try {
            $setLocation = FileParameterValue.class.getDeclaredMethod("setLocation",String.class);
            $setLocation.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }
    private static final Logger LOGGER = Logger.getLogger(BinaryFileParameterFactory.class.getName());
}
