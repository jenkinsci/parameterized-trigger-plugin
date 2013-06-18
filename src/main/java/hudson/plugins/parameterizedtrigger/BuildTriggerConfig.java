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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
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

        // expand variables if applicable
        StringBuilder projectNames = new StringBuilder();
        StringTokenizer tokens = new StringTokenizer(projects,",");
        while(tokens.hasMoreTokens()) {
            if(projectNames.length() > 0) {
                projectNames.append(',');
            }
            projectNames.append(env != null ? env.expand(tokens.nextToken().trim()) : tokens.nextToken().trim());
        }

        projectList.addAll(Items.fromNameList(context, projectNames.toString(), AbstractProject.class));
		return projectList;
	}

    /**
     * Provides a list containing four set, each containing projects to be displayed on the project view
     * for projects using the parameterized trigger plugin under 'Subprojects'.<br>
     * <li>
     * The first set contains statically configured project to trigger.
     * The second set contains dynamically configured project, resolved by back tracking builds environment variables.
     * The third set contains other triggered project found during back tracking builds
     * The fourth set contains project that couldn't be resolved or project that doesn't exists.
     * </li>
     *
     * @param context The container with which to resolve relative project names.
     * @return A list containing sets with Projects
     */
    public List<Set<?>> getProjectInfo(AbstractProject context) {

        Comparator customComparator = new Comparator<AbstractProject>() {
            public int compare(AbstractProject abstractProject1, AbstractProject abstractProject2) {
                return abstractProject1.getFullName().compareTo(abstractProject2.getFullName());
            }
        };

        Set<AbstractProject> dynamicProject = new TreeSet<AbstractProject>(customComparator);
        Set<AbstractProject> staticProject = new TreeSet<AbstractProject>(customComparator);
        Set<AbstractProject> triggeredProject = new TreeSet<AbstractProject>(customComparator);
        Set<String> unresolvedProject = new TreeSet<String>();

        iterateBuilds(context, projects, dynamicProject, staticProject, triggeredProject, unresolvedProject);

        // We don't want to show a project twice
        triggeredProject.removeAll(dynamicProject);
        triggeredProject.removeAll(staticProject);

        return Arrays.asList(staticProject, dynamicProject, triggeredProject, unresolvedProject);
    }

    /**
     * Resolves static project and iterating old builds to resolve dynamic builds and collecting triggered builds.<br>
     * <br>
     * If static defined project and/or unresolved projects exists they are returned. If old builds exists it tries
     * to resolve them by back tracking the last five builds and as a last resource the last successful build.<br>
     * <br>
     * During the back tracking process all actually trigger projects from those builds are also collected.<br>
     * <br>
     *
     * @param context              The container with which to resolve relative project names.
     * @param projects             String containing the defined projects to build
     * @param dynamicProjectSet    A Set where to add dynamic project
     * @param staticProjectSet     A Set where to add static project
     * @param triggeredProjectSet  A Set where to add triggered project
     * @param unsolvedProjectNames A Set where to add unsolved project
     */
    private static void iterateBuilds(AbstractProject context, String projects,
                                      Set<AbstractProject> dynamicProjectSet, Set<AbstractProject> staticProjectSet,
                                      Set<AbstractProject> triggeredProjectSet, Set<String> unsolvedProjectNames) {

        StringTokenizer stringTokenizer = new StringTokenizer(projects, ",");
        while (stringTokenizer.hasMoreTokens()) {
            unsolvedProjectNames.add(stringTokenizer.nextToken().trim());
        }

        // Nbr of builds to back track
        final int BACK_TRACK = 5;

        if (!unsolvedProjectNames.isEmpty()) {

            AbstractBuild currentBuild = (AbstractBuild)context.getLastBuild();

            // If we don't have any build there's no point to trying to resolved dynamic projects
            if (currentBuild == null) {
                // But we can still get statically defined project
                staticProjectSet.addAll(Items.fromNameList(context.getParent(), projects, AbstractProject.class));
                // Remove them from unsolved
                for (AbstractProject staticProject : staticProjectSet) {
                    unsolvedProjectNames.remove(staticProject.getFullName());
                }
                return;
            }

            // check the last build
            resolveProject(currentBuild, dynamicProjectSet, staticProjectSet, triggeredProjectSet, unsolvedProjectNames);
            currentBuild = (AbstractBuild)currentBuild.getPreviousBuild();

            int backTrackCount = 0;
            // as long we have more builds to examine we continue,
            while (currentBuild != null && backTrackCount < BACK_TRACK) {
                resolveProject(currentBuild, dynamicProjectSet, staticProjectSet, triggeredProjectSet, unsolvedProjectNames);
                currentBuild = (AbstractBuild)currentBuild.getPreviousBuild();
                backTrackCount++;
            }

            // If oldBuild is null then we have already examined LastSuccessfulBuild as well.
            if (currentBuild != null) {
                resolveProject((AbstractBuild)context.getLastSuccessfulBuild(), dynamicProjectSet, staticProjectSet, triggeredProjectSet, unsolvedProjectNames);
            }
        }
    }

    /**
     * Retrieves the environment variable from a build and tries to resolves the remaining unresolved projects. If
     * resolved it ends up either in the dynamic or static Set of project. It also collect all actually triggered
     * project and store them in a Set.
     *
     * @param build                The build to retrieve environment variables from and collect triggered projects
     * @param dynamicProjectSet    A Set where to add dynamic project
     * @param staticProjectSet     A Set where to add static project
     * @param triggeredProjectSet  A Set where to add triggered project
     * @param unsolvedProjectNames A Set where to add unsolved project
     */
    private static void resolveProject(AbstractBuild build,
                                       Set<AbstractProject> dynamicProjectSet, Set<AbstractProject> staticProjectSet,
                                       Set<AbstractProject> triggeredProjectSet, Set<String> unsolvedProjectNames) {

        Iterator<String> unsolvedProjectIterator = unsolvedProjectNames.iterator();

        while (unsolvedProjectIterator.hasNext()) {

            String unresolvedProjectName = unsolvedProjectIterator.next();
            Set<AbstractProject> destinationSet = staticProjectSet;

            // expand variables if applicable
            if (unresolvedProjectName.contains("$")) {

                EnvVars env = null;
                try {
                    env = build != null ? build.getEnvironment() : null;
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }

                unresolvedProjectName = env != null ? env.expand(unresolvedProjectName) : unresolvedProjectName;
                destinationSet = dynamicProjectSet;
            }

            AbstractProject resolvedProject = Jenkins.getInstance().getItem(unresolvedProjectName, build.getProject().getParent(), AbstractProject.class);
            if (resolvedProject != null) {
                destinationSet.add(resolvedProject);
                unsolvedProjectIterator.remove();
            }
        }

        if (build != null && build.getAction(BuildInfoExporterAction.class) != null) {
            String triggeredProjects = build.getAction(BuildInfoExporterAction.class).getProjectListString(",");
            triggeredProjectSet.addAll(Items.fromNameList(build.getParent().getParent(), triggeredProjects, AbstractProject.class));
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

    public boolean onJobRenamed(String oldName, String newName) {
    	boolean changed = false;
    	String[] list = projects.split(",");
    	for (int i = 0; i < list.length; i++) {
    		if (list[i].trim().equals(oldName)) {
    			list[i] = newName;
    			changed = true;
    		}
    	}
    	if (changed) {
    		StringBuilder buf = new StringBuilder();
    		for (int i = 0; i < list.length; i++) {
    			if (list[i] == null) continue;
    			if (buf.length() > 0){
    				buf.append(',');
    			}
    			buf.append(list[i]);
    		}
    		projects = buf.toString();
    	}
    	return changed;
    }

    public boolean onDeleted(String oldName) {
    	return onJobRenamed(oldName, null);
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
        public AutoCompletionCandidates doAutoCompleteProjects(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<Job> jobs = Hudson.getInstance().getItems(Job.class);
            for (Job job: jobs) {
                if (job.getFullName().startsWith(value)) {
                    if (job.hasPermission(Item.READ)) {
                        candidates.add(job.getFullName());
                    }
                }
            }
            return candidates;
        }

    }
}