package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ImmutableMap;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import com.google.common.collect.Lists;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LinesFromFileBuildParameterFactory extends AbstractBuildParameterFactory {

    private String filePath;
    private String paramExpr;
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    @DataBoundConstructor
    public LinesFromFileBuildParameterFactory(String filePath, String paramExpr) {
        this.filePath = filePath;
        this.paramExpr = paramExpr;
    }

    @Override
    public List<AbstractBuildParameters> getParameters(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException, AbstractBuildParameters.DontTriggerException {
        EnvVars envVars = build.getEnvironment(listener);
        String expandedFilePath = envVars.expand(filePath);
        ArrayList<AbstractBuildParameters> params = Lists.newArrayList();
        String fileContents = build.getWorkspace().child(expandedFilePath).readToString();
        for (String line : NEWLINE_PATTERN.split(fileContents)) {
            line = line.trim();
            if(line != null && line.length() > 0) {
                params.add(new PredefinedBuildParameters(Util.replaceMacro(paramExpr, ImmutableMap.of("LINE", line))));
            }
        }
        return params;
    }

    @Extension
    public static class DescriptorImpl extends AbstractBuildParameterFactoryDescriptor {
        @Override
        public String getDisplayName() {
            return "Lines From File Parameter Factory";
        }

        public FormValidation doCheckFilePath(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }
    }

    public String getParamExpr() {
        return paramExpr;
    }

    public String getFilePath() {
        return filePath;
    }
}
