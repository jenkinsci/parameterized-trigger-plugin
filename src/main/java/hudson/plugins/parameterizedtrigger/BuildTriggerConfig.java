package hudson.plugins.parameterizedtrigger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters.DontTriggerException;
import hudson.plugins.promoted_builds.Promotion;
import hudson.tasks.Messages;
import hudson.util.FormValidation;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

public class BuildTriggerConfig implements Describable<BuildTriggerConfig> {

	private final List<AbstractBuildParameters> configs;
    private final List<AbstractBuildParameterFactory> configFactories;

	private String projects;
	private final ResultCondition condition;
	private boolean triggerWithNoParameters;

    public BuildTriggerConfig(String projects, ResultCondition condition,
            boolean triggerWithNoParameters, List<AbstractBuildParameterFactory> configFactories, List<AbstractBuildParameters> configs) {
        this.projects = projects;
        this.condition = condition;
        this.triggerWithNoParameters = triggerWithNoParameters;
        this.configFactories = configFactories;
        this.configs = Util.fixNull(configs);
    }

    @DataBoundConstructor
    public BuildTriggerConfig(String projects, ResultCondition condition,
            boolean triggerWithNoParameters, List<AbstractBuildParameters> configs) {
        this(projects, condition, triggerWithNoParameters, null, configs);
    }

	public BuildTriggerConfig(String projects, ResultCondition condition,
			AbstractBuildParameters... configs) {
		this(projects, condition, false, null, Arrays.asList(configs));
	}

	public BuildTriggerConfig(String projects, ResultCondition condition,
            List<AbstractBuildParameterFactory> configFactories,
			AbstractBuildParameters... configs) {
		this(projects, condition, false, configFactories, Arrays.asList(configs));
	}

	public List<AbstractBuildParameters> getConfigs() {
		return configs;
	}

    public List<AbstractBuildParameterFactory> getConfigFactories() {
        return configFactories;
    }

    public String getProjects() {
		return projects;
	}

    public String getProjects(EnvVars env) {
        return (env != null ? env.expand(projects) : projects);
    }

	public ResultCondition getCondition() {
		return condition;
	}

	public boolean getTriggerWithNoParameters() {
        return triggerWithNoParameters;
    }

    /**
     * @deprecated
     *      Use {@link #getProjectList(ItemGroup, EnvVars)}
     */
    public List<AbstractProject> getProjectList(EnvVars env) {
        return getProjectList(null,env);
    }

    /**
     * @param env Environment variables from which to expand project names; Might be {@code null}.
     * @param context
     *      The container with which to resolve relative project names.
     */
	public List<AbstractProject> getProjectList(ItemGroup context, EnvVars env) {
        List<AbstractProject> projectList = new ArrayList<AbstractProject>();
        projectList.addAll(Items.fromNameList(context, getProjects(env), AbstractProject.class));
		return projectList;
	}

    /**
     * Provides a SubProjectData object containing four set, each containing projects to be displayed on the project
     * view under 'Subprojects' section.<br>
     * <li>
     * The first set contains fixed (statically) configured project to be trigger.
     * The second set contains dynamically configured project, resolved by back tracking builds environment variables.
     * The third set contains other recently triggered project found during back tracking builds
     * The fourth set contains dynamically configured project that couldn't be resolved or project that doesn't exists.
     * </li>
     *
     * @param context   The container with which to resolve relative project names.
     * @return A data object containing sets with projects
     */
    public SubProjectData getProjectInfo(AbstractProject context) {

        SubProjectData subProjectData = new SubProjectData();

        iterateBuilds(context, projects, subProjectData);

        // We don't want to show a project twice
        subProjectData.getTriggered().removeAll(subProjectData.getDynamic());
        subProjectData.getTriggered().removeAll(subProjectData.getFixed());

        return subProjectData;
    }

    /**
     * Resolves fixed (static) project and iterating old builds to resolve dynamic and collecting triggered
     * projects.<br>
     * <br>
     * If fixed project and/or resolved projects exists they are returned in fixed or dynamic in subProjectData.
     * If old builds exists it tries to resolve projects by back tracking the last five builds and as a last resource
     * the last successful build.<br>
     * <br>
     * During the back tracking process all actually trigger projects from those builds are also collected and stored
     * in triggered in subProjectData.<br>
     * <br>
     *
     * @param context           The container with which to resolve relative project names.
     * @param projects          String containing the defined projects to build
     * @param subProjectData    Data object containing sets storing projects
     */
    private static void iterateBuilds(AbstractProject context, String projects, SubProjectData subProjectData) {

        StringTokenizer stringTokenizer = new StringTokenizer(projects, ",");
        while (stringTokenizer.hasMoreTokens()) {
            subProjectData.getUnresolved().add(stringTokenizer.nextToken().trim());
        }

        // Nbr of builds to back track
        final int BACK_TRACK = 5;

        if (!subProjectData.getUnresolved().isEmpty()) {

            AbstractBuild currentBuild = (AbstractBuild)context.getLastBuild();

            // If we don't have any build there's no point to trying to resolved dynamic projects
            if (currentBuild == null) {
                // But we can still get statically defined project
                subProjectData.getFixed().addAll(Items.fromNameList(context.getParent(), projects, AbstractProject.class));
                // Remove them from unsolved
                for (AbstractProject staticProject : subProjectData.getFixed()) {
                    subProjectData.getUnresolved().remove(staticProject.getFullName());
                }
                return;
            }

            // check the last build
            resolveProject(currentBuild, subProjectData);
            currentBuild = (AbstractBuild)currentBuild.getPreviousBuild();

            int backTrackCount = 0;
            // as long we have more builds to examine we continue,
            while (currentBuild != null && backTrackCount < BACK_TRACK) {
                resolveProject(currentBuild, subProjectData);
                currentBuild = (AbstractBuild)currentBuild.getPreviousBuild();
                backTrackCount++;
            }

            // If oldBuild is null then we have already examined LastSuccessfulBuild as well.
            if (currentBuild != null) {
                resolveProject((AbstractBuild)context.getLastSuccessfulBuild(), subProjectData);
            }
        }
    }

    /**
     * Retrieves the environment variable from a build and tries to resolves the remaining unresolved projects. If
     * resolved it ends up either in the dynamic or fixed in subProjectData. It also collect all actually triggered
     * project and store them in triggered in subProjectData.
     *
     * @param build             The build to retrieve environment variables from and collect triggered projects
     * @param subProjectData    Data object containing sets storing projects
     */
    private static void resolveProject(AbstractBuild build, SubProjectData subProjectData) {

        Iterator<String> unsolvedProjectIterator = subProjectData.getUnresolved().iterator();

        while (unsolvedProjectIterator.hasNext()) {

            String unresolvedProjectName = unsolvedProjectIterator.next();
            Set<AbstractProject> destinationSet = subProjectData.getFixed();

            // expand variables if applicable
            if (unresolvedProjectName.contains("$")) {

                EnvVars env = null;
                try {
                    env = build != null ? build.getEnvironment() : null;
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }

                unresolvedProjectName = env != null ? env.expand(unresolvedProjectName) : unresolvedProjectName;
                destinationSet = subProjectData.getDynamic();
            }

            AbstractProject resolvedProject = Jenkins.getInstance().getItem(unresolvedProjectName, build.getProject().getParent(), AbstractProject.class);
            if (resolvedProject != null) {
                destinationSet.add(resolvedProject);
                unsolvedProjectIterator.remove();
            }
        }

        if (build != null && build.getAction(BuildInfoExporterAction.class) != null) {
            String triggeredProjects = build.getAction(BuildInfoExporterAction.class).getProjectListString(",");
            subProjectData.getTriggered().addAll(Items.fromNameList(build.getParent().getParent(), triggeredProjects, AbstractProject.class));
        }
    }

	private static ParametersAction mergeParameters(ParametersAction base, ParametersAction overlay) {
		LinkedHashMap<String,ParameterValue> params = new LinkedHashMap<String,ParameterValue>();
		for (ParameterValue param : base.getParameters())
			params.put(param.getName(), param);
		for (ParameterValue param : overlay.getParameters())
			params.put(param.getName(), param);
		return new ParametersAction(params.values().toArray(new ParameterValue[params.size()]));
	}

	private static ParametersAction getDefaultParameters(AbstractProject<?,?> project) {
		ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);
		if (property == null) {
			return null;
		}

		List<ParameterValue> parameters = new ArrayList<ParameterValue>();
		for (ParameterDefinition pd : property.getParameterDefinitions()) {
			ParameterValue param = pd.getDefaultParameterValue();
			if (param != null) parameters.add(param);
		}

		return new ParametersAction(parameters);
	}

    List<Action> getBaseActions(AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException {
        return getBaseActions(configs, build, listener);
    }

    List<Action> getBaseActions(Collection<AbstractBuildParameters> configs, AbstractBuild<?,?> build, TaskListener listener)
            throws IOException, InterruptedException, DontTriggerException {
		List<Action> actions = new ArrayList<Action>();
		ParametersAction params = null;
		for (AbstractBuildParameters config : configs) {
			Action a = config.getAction(build, listener);
			if (a instanceof ParametersAction) {
				params = params == null ? (ParametersAction)a
					: mergeParameters(params, (ParametersAction)a);
			} else if (a != null) {
				actions.add(a);
			}
		}
		if (params != null) actions.add(params);
		return actions;
	}

	List<Action> getBuildActions(List<Action> baseActions, AbstractProject project) {
		List<Action> actions = new ArrayList<Action>(baseActions);

		ParametersAction defaultParameters = getDefaultParameters(project);
		if (defaultParameters != null) {
			Action a = null;
			for (ListIterator<Action> it = actions.listIterator(); it.hasNext();)
				if ((a = it.next()) instanceof ParametersAction) {
					it.set(mergeParameters(defaultParameters, (ParametersAction)a));
					break;
                                }
			if (!(a instanceof ParametersAction))
				actions.add(defaultParameters);
		}
		return actions;
	}

	/**
     * Note that with Hudson 1.341, trigger should be using
	 * {@link BuildTrigger#buildDependencyGraph(AbstractProject, hudson.model.DependencyGraph)}.
	 */
	public List<Future<AbstractBuild>> perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        try {
			if (condition.isMet(build.getResult())) {
                List<Future<AbstractBuild>> futures = new ArrayList<Future<AbstractBuild>>();

                for (List<AbstractBuildParameters> addConfigs : getDynamicBuildParameters(build, listener)) {
                    List<Action> actions = getBaseActions(
                            ImmutableList.<AbstractBuildParameters>builder().addAll(configs).addAll(addConfigs).build(),
                            build, listener);
                    for (AbstractProject project : getProjectList(build.getRootBuild().getProject().getParent(),env)) {
                        List<Action> list = getBuildActions(actions, project);

                        futures.add(schedule(build, project, list));
                    }
                }

                return futures;
			}
		} catch (DontTriggerException e) {
			// don't trigger on this configuration
		}
        return Collections.emptyList();
	}

    public ListMultimap<AbstractProject, Future<AbstractBuild>> perform2(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        try {
            if (getCondition().isMet(build.getResult())) {
                ListMultimap<AbstractProject, Future<AbstractBuild>> futures = ArrayListMultimap.create();

                for (List<AbstractBuildParameters> addConfigs : getDynamicBuildParameters(build, listener)) {
                    List<Action> actions = getBaseActions(ImmutableList.<AbstractBuildParameters>builder().addAll(configs).addAll(addConfigs).build(), build, listener);
                    for (AbstractProject project : getProjectList(build.getRootBuild().getProject().getParent(),env)) {
                        List<Action> list = getBuildActions(actions, project);

                        futures.put(project, schedule(build, project, list));
                    }
                }
                return futures;
            }
        } catch (DontTriggerException e) {
            // don't trigger on this configuration
        }
        return ArrayListMultimap.create();
    }

    /**
     * @return
     *      Inner list represents a set of build parameters used together for one invocation of a project,
     *      and outer list represents multiple invocations of the same project.
     */
    private List<List<AbstractBuildParameters>> getDynamicBuildParameters(AbstractBuild<?,?> build, BuildListener listener) throws DontTriggerException, IOException, InterruptedException {
        if (configFactories == null || configFactories.isEmpty()) {
            return ImmutableList.<List<AbstractBuildParameters>>of(ImmutableList.<AbstractBuildParameters>of());
        } else {
            // this code is building the combinations of all AbstractBuildParameters reported from all factories
            List<List<AbstractBuildParameters>> dynamicBuildParameters = Lists.newArrayList();
            dynamicBuildParameters.add(Collections.<AbstractBuildParameters>emptyList());
            for (AbstractBuildParameterFactory configFactory : configFactories) {
                List<List<AbstractBuildParameters>> newDynParameters = Lists.newArrayList();
                List<AbstractBuildParameters> factoryParameters = configFactory.getParameters(build, listener);
                // if factory returns 0 parameters we need to skip assigning newDynParameters to dynamicBuildParameters as we would add invalid list
                if(factoryParameters.size() > 0) {
                    for (AbstractBuildParameters config : factoryParameters) {
                        for (List<AbstractBuildParameters> dynamicBuildParameter : dynamicBuildParameters) {
                            newDynParameters.add(
                                    ImmutableList.<AbstractBuildParameters>builder()
                                            .addAll(dynamicBuildParameter)
                                            .add(config)
                                            .build());
                        }
                    }
                    dynamicBuildParameters = newDynParameters;
                }
            }
            return dynamicBuildParameters;
        }
    }

    /**
     * Create UpstreamCause that triggers a downstream build.
     * 
     * If the upstream build is a promotion, return the UpstreamCause
     * as triggered by the target of the promotion.
     * 
     * @param build an upstream build
     * @return UpstreamCause
     */
    protected Cause createUpstreamCause(AbstractBuild<?, ?> build) {
        if(Jenkins.getInstance().getPlugin("promoted-builds") != null) {
            // Test only when promoted-builds is installed.
            if(build instanceof Promotion) {
                Promotion promotion = (Promotion)build;
                
                // This cannot be done for PromotionCause#PromotionCause is in a package scope.
                // return new PromotionCause(build, promotion.getTarget());
                
                return new UpstreamCause((Run<?,?>)promotion.getTarget());
            }
        }
        return new UpstreamCause((Run) build);
    }

    protected Future schedule(AbstractBuild<?, ?> build, AbstractProject project, int quietPeriod, List<Action> list) throws InterruptedException, IOException {
        Cause cause = createUpstreamCause(build);
        return project.scheduleBuild2(quietPeriod,
                cause,
                list.toArray(new Action[list.size()]));
    }

    protected Future schedule(AbstractBuild<?, ?> build, AbstractProject project, List<Action> list) throws InterruptedException, IOException {
        return schedule(build, project, project.getQuietPeriod(), list);
    }

    /**
     * A backport of {@link Items#computeRelativeNamesAfterRenaming(String, String, String, ItemGroup)} in Jenkins 1.530.
     * 
     * computeRelativeNamesAfterRenaming contains a bug in Jenkins < 1.530.
     * Replace this to {@link Items#computeRelativeNamesAfterRenaming(String, String, String, ItemGroup)}
     * when updated the target version to >= 1.530.
     * 
     * @param oldFullName
     * @param newFullName
     * @param relativeNames
     * @param context
     * @return
     */
    private static String computeRelativeNamesAfterRenaming(String oldFullName, String newFullName, String relativeNames, ItemGroup<?> context) {
        if(!Jenkins.getVersion().isOlderThan(new VersionNumber("1.530"))) {
            return Items.computeRelativeNamesAfterRenaming(oldFullName, newFullName, relativeNames, context);
        }
        StringTokenizer tokens = new StringTokenizer(relativeNames,",");
        List<String> newValue = new ArrayList<String>();
        while(tokens.hasMoreTokens()) {
            String relativeName = tokens.nextToken().trim();
            String canonicalName = Items.getCanonicalName(context, relativeName);
            if (canonicalName.equals(oldFullName) || canonicalName.startsWith(oldFullName + "/")) {
                String newCanonicalName = newFullName + canonicalName.substring(oldFullName.length());
                // relative name points to the renamed item, let's compute the new relative name
                newValue.add( computeRelativeNameAfterRenaming(canonicalName, newCanonicalName, relativeName) );
            } else {
                newValue.add(relativeName);
            }
        }
        return StringUtils.join(newValue, ",");
    }

    private static String computeRelativeNameAfterRenaming(String oldFullName, String newFullName, String relativeName) {

        String[] a = oldFullName.split("/");
        String[] n = newFullName.split("/");
        assert a.length == n.length;
        String[] r = relativeName.split("/");

        int j = a.length-1;
        for(int i=r.length-1;i>=0;i--) {
            String part = r[i];
            if (part.equals("") && i==0) {
                continue;
            }
            if (part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                j--;
                continue;
            }
            if (part.equals(a[j])) {
                r[i] = n[j];
                j--;
                continue;
            }
        }
        return StringUtils.join(r, '/');
    }

    public boolean onJobRenamed(ItemGroup context, String oldName, String newName) {
        String newProjects = computeRelativeNamesAfterRenaming(oldName, newName, projects, context);
    	boolean changed = !projects.equals(newProjects);
        projects = newProjects;
    	return changed;
    }

    public boolean onDeleted(ItemGroup context, String oldName) {
        List<String> newNames = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(projects,",");
        List<String> newValue = new ArrayList<String>();
        while (tokens.hasMoreTokens()) {
            String relativeName = tokens.nextToken().trim();
            String fullName = Items.getCanonicalName(context, relativeName);
            if (!fullName.equals(oldName)) newNames.add(relativeName);
        }
        String newProjects = StringUtils.join(newNames, ",");
        boolean changed = !projects.equals(newProjects);
        projects = newProjects;
        return changed;
    }

    public Descriptor<BuildTriggerConfig> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Override
	public String toString() {
		return getClass().getName()+" [projects=" + projects + ", condition="
				+ condition + ", configs=" + configs + "]";
	}

    @Extension
    public static class DescriptorImpl extends Descriptor<BuildTriggerConfig> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

        public List<Descriptor<AbstractBuildParameters>> getBuilderConfigDescriptors() {
            return Hudson.getInstance().<AbstractBuildParameters,
              Descriptor<AbstractBuildParameters>>getDescriptorList(AbstractBuildParameters.class);
        }

        public List<Descriptor<AbstractBuildParameterFactory>> getBuilderConfigFactoryDescriptors() {
            return Hudson.getInstance().<AbstractBuildParameterFactory,
              Descriptor<AbstractBuildParameterFactory>>getDescriptorList(AbstractBuildParameterFactory.class);
        }

        /**
         * Form validation method.
         *
         * Copied from hudson.tasks.BuildTrigger.doCheck(Item project, String value)
         */
        public FormValidation doCheckProjects(@AncestorInPath Item project, @QueryParameter String value ) {
            // Require CONFIGURE permission on this project
            if(!project.hasPermission(Item.CONFIGURE)){
            	return FormValidation.ok();
            }
            StringTokenizer tokens = new StringTokenizer(Util.fixNull(value),",");
            boolean hasProjects = false;
            while(tokens.hasMoreTokens()) {
                String projectName = tokens.nextToken().trim();
                if (StringUtils.isNotBlank(projectName)) {
                	Item item = Jenkins.getInstance().getItem(projectName,project,Item.class); // only works after version 1.410
                    if(item==null){
                        return FormValidation.error(Messages.BuildTrigger_NoSuchProject(projectName,AbstractProject.findNearest(projectName).getName()));
                    }
                    if(!(item instanceof AbstractProject)){
                        return FormValidation.error(Messages.BuildTrigger_NotBuildable(projectName));
                    }
                    hasProjects = true;
                }
            }
            if (!hasProjects) {
//            	return FormValidation.error(Messages.BuildTrigger_NoProjectSpecified()); // only works with Jenkins version built after 2011-01-30
            	return FormValidation.error("No project specified");
            }

            return FormValidation.ok();
        }

        /**
         * Autocompletion method
         *
         * Copied from hudson.tasks.BuildTrigger.doAutoCompleteChildProjects(String value)
         *
         * @param value
         * @return
         */
        public AutoCompletionCandidates doAutoCompleteProjects(@QueryParameter String value, @AncestorInPath ItemGroup context) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
            for (Job job: jobs) {
                String relativeName = job.getRelativeNameFrom(context);
                if (relativeName.startsWith(value)) {
                    if (job.hasPermission(Item.READ)) {
                        candidates.add(relativeName);
                    }
                }
            }
            return candidates;
        }

    }
}