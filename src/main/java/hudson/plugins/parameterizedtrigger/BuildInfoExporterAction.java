/*
 * The MIT License
 *
 * Copyright (c) 2011-2, Jørgen P. Tjernø <jorgenpt@gmail.com>
 *                       Chris Johnson
 *                       Geoff Cummings
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.parameterizedtrigger;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class BuildInfoExporterAction implements EnvironmentContributingAction {

  public static final String JOB_NAME_VARIABLE = "LAST_TRIGGERED_JOB_NAME";
  public static final String ALL_JOBS_NAME_VARIABLE = "TRIGGERED_JOB_NAMES";
  public static final String BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBER_";
  public static final String ALL_BUILD_NUMBER_VARIABLE_PREFIX = "TRIGGERED_BUILD_NUMBERS_";
  public static final String BUILD_RESULT_VARIABLE_PREFIX = "TRIGGERED_BUILD_RESULT_";
  public static final String BUILD_RUN_COUNT_PREFIX = "TRIGGERED_BUILD_RUN_COUNT_";
  public static final String RUN = "_RUN_";
  // now unused as part of map
  private transient String buildName;
  private transient int buildNumber;

  // used in version =< 2.21.
  // this is now migrated to this.builds.
  private transient Map<String, List<AbstractBuildReference>> buildRefs;

  private List<AbstractBuildReference> builds;

  // used in version =< 2.32.
  // this is now migrated to this.lastReference2.
  private transient BuildReference lastReference;
  private AbstractBuildReference lastReference2;

  public BuildInfoExporterAction(AbstractBuildReference buildRef) {
    super();

    this.builds = new ArrayList<AbstractBuildReference>();
    addBuild(buildRef);
    this.lastReference2 = buildRef;
  }

  public BuildInfoExporterAction(AbstractBuild<?, ?> parentBuild, AbstractBuildReference buildRef) {
    this(buildRef);
  }

  public BuildInfoExporterAction(String buildName, int buildNumber, AbstractBuild<?, ?> parentBuild, Result buildResult) {
    this(new BuildReference(buildName, buildNumber, buildResult));
  }

  static BuildInfoExporterAction addBuildInfoExporterAction(AbstractBuild<?, ?> parentBuild, QueueTaskFuture<? extends AbstractBuild> buildFuture) {
    BuildInfoExporterAction action = parentBuild.getAction(BuildInfoExporterAction.class);
    FutureBuildReference buildRef = new FutureBuildReference(buildFuture);
    if (action == null) {
      action = new BuildInfoExporterAction(parentBuild, buildRef);
      parentBuild.getActions().add(action);
    } else {
      action.addBuildReference(buildRef);
    }
    return action;
  }

  static BuildInfoExporterAction addBuildInfoExporterAction(AbstractBuild<?, ?> parentBuild, String triggeredProject) {
    BuildInfoExporterAction action = parentBuild.getAction(BuildInfoExporterAction.class);
    StaticBuildReference buildRef = new StaticBuildReference(triggeredProject);
    if (action == null) {
      action = new BuildInfoExporterAction(buildRef);
      parentBuild.getActions().add(action);
    } else {
      action.addBuildReference(buildRef);
    }
    return action;
  }

  private void addBuild(AbstractBuildReference br) {
    this.builds.add(br);

    if (br.getBuildNumber() != 0) {
      this.lastReference2 = br;
    }
  }

  public void addBuildReference(AbstractBuildReference buildRef) {
    addBuild(buildRef);
  }

  public static abstract class AbstractBuildReference {
    public abstract String getProjectName();

    public abstract int getBuildNumber();

    public abstract Result getBuildResult();

    public abstract void update();
  }

  /**
   * @deprecated kept for compatibility
   */
  @Deprecated
  public static class BuildReference extends AbstractBuildReference {

    public String projectName;
    public int buildNumber;
    public Result buildResult;

    public BuildReference(String projectName, int buildNumber, Result buildResult) {
      this.projectName = projectName;
      this.buildNumber = buildNumber;
      this.buildResult = buildResult;
    }

    public BuildReference(final String projectName) {
      this.projectName = projectName;
      this.buildNumber = 0;
      this.buildResult = Result.NOT_BUILT;
    }

    public String getProjectName() {
      return projectName;
    }

    public int getBuildNumber() {
      return buildNumber;
    }

    public Result getBuildResult() {
      return buildResult;
    }

    @Override
    public void update() {

    }
  }

  public static class StaticBuildReference extends AbstractBuildReference {

    public String projectName;
    public int buildNumber;
    public Result buildResult;

    public StaticBuildReference(String projectName, int buildNumber, Result buildResult) {
      this.projectName = projectName;
      this.buildNumber = buildNumber;
      this.buildResult = buildResult;
    }

    public StaticBuildReference(final String projectName) {
      this.projectName = projectName;
      this.buildNumber = 0;
      this.buildResult = Result.NOT_BUILT;
    }

    public String getProjectName() {
      return projectName;
    }

    public int getBuildNumber() {
      return buildNumber;
    }

    public Result getBuildResult() {
      return buildResult;
    }

    @Override
    public void update() {

    }
  }

  public static class FutureBuildReference extends AbstractBuildReference {

    private final transient QueueTaskFuture<? extends AbstractBuild> buildFuture;

    public String projectName = "";
    public int buildNumber = 0;
    public Result buildResult = Result.NOT_BUILT;

    public FutureBuildReference(QueueTaskFuture<? extends AbstractBuild> buildFuture) {
      this.buildFuture = buildFuture;
    }

    public void update() {
      if (buildFuture == null) {
        return;
      }

      Future<? extends AbstractBuild> startCondition = buildFuture.getStartCondition();
      if (!startCondition.isDone() || startCondition.isCancelled()) {
        return;
      }

      try {
        AbstractBuild build = startCondition.get();
        projectName = build.getParent().getFullName();
        buildNumber = build.getNumber();
        buildResult = build.getResult();
      } catch (CancellationException e) {
        return;
      } catch (InterruptedException e) {
        return;
      } catch (ExecutionException e) {
        return;
      }
    }

    public String getProjectName() {
      update();
      return projectName;
    }

    public int getBuildNumber() {
      update();
      return buildNumber;
    }

    public Result getBuildResult() {
      update();
      return buildResult;
    }
  }

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return null;
  }

  @Override
  public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {

    // Note: this will only indicate the last project in the list that is ran
    env.put(JOB_NAME_VARIABLE, getLastReference().getProjectName().replaceAll("[^a-zA-Z0-9]+", "_"));
    // all projects triggered.
    // this should not include projects that donot have a build item.
    String sanatizedProjectList = getProjectListString(",");
    env.put(ALL_JOBS_NAME_VARIABLE, sanatizedProjectList);

    for (String project : getProjectsWithBuilds()) {
      // for each project add the following variables once
      // all buildnumbers, lastbuildnumber
      // all Run results, last build result
      String sanatizedBuildName = project.replaceAll("[^a-zA-Z0-9]+", "_");
      List<AbstractBuildReference> refs = getBuildRefs(project);

      env.put(ALL_BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName, getBuildNumbersString(refs, ","));
      env.put(BUILD_RUN_COUNT_PREFIX + sanatizedBuildName, Integer.toString(refs.size()));
      for (AbstractBuildReference br : refs) {
        if (br.getBuildNumber() != 0) {
          String tiggeredBuildRunResultKey = BUILD_RESULT_VARIABLE_PREFIX + sanatizedBuildName + RUN + Integer.toString(br.getBuildNumber());
          env.put(tiggeredBuildRunResultKey, br.getBuildResult().toString());
        }
      }
      AbstractBuildReference lastBuild = null;
      for (int i = (refs.size()); i > 0; i--) {
        if (refs.get(i - 1).getBuildNumber() != 0) {
          lastBuild = refs.get(i - 1);
        }
        break;
      }
      if (lastBuild != null) {
        env.put(BUILD_NUMBER_VARIABLE_PREFIX + sanatizedBuildName, Integer.toString(lastBuild.getBuildNumber()));
        env.put(BUILD_RESULT_VARIABLE_PREFIX + sanatizedBuildName, lastBuild.getBuildResult().toString());
      }
    }
  }

  private List<AbstractBuildReference> getBuildRefs(String project) {
    List<AbstractBuildReference> refs = new ArrayList<AbstractBuildReference>();
    for (AbstractBuildReference br : builds) {
      if (br.getProjectName().equals(project))
        refs.add(br);
    }
    return refs;
  }

  /**
   * Gets all the builds triggered from this one, filters out the items that
   * were non blocking, which we don't have a builds for. Used in the UI for see
   * Summary.groovy
   *
   * @return a list of builds that are triggered by this build. May contains null if a project or a build is deleted.
   */
  @Exported(visibility = 1)
  public List<AbstractBuild<?, ?>> getTriggeredBuilds() {

    List<AbstractBuild<?, ?>> builds = new ArrayList<AbstractBuild<?, ?>>();

    for (AbstractBuildReference br : this.builds) {
      AbstractProject<?, ? extends AbstractBuild<?, ?>> project =
    		  Jenkins.getInstance().getItemByFullName(br.getProjectName(), AbstractProject.class);
        if (br.getBuildNumber() != 0) {
          builds.add((project != null)?project.getBuildByNumber(br.getBuildNumber()):null);
        }
    }
    return builds;
  }

  /**
   * Gets all the projects that triggered from this one which were non blocking,
   * which we don't have a builds for. Does not include builds that are returned
   * in #link{getTriggeredBuilds} Used in the UI for see Summary.groovy
   *
   * @return List of Projects that are triggered by this build. May contains null if a project is deleted.
   */
  @Exported(visibility = 1)
  public List<AbstractProject<?, ?>> getTriggeredProjects() {
    List<AbstractProject<?, ?>> projects = new ArrayList<AbstractProject<?, ?>>();

    for (AbstractBuildReference br : this.builds) {
      if (br.getBuildNumber() == 0) {
        AbstractProject<?, ? extends AbstractBuild<?, ?>> project =
        		Jenkins.getInstance().getItemByFullName(br.getProjectName(), AbstractProject.class);
        projects.add(project);
      }
    }
    return projects;
  }

  /**
   * Handle cases from older builds so that they still add old variables if
   * needed to. Should not show any UI as there will be no data added.
   *
   * @return
   */
  public Object readResolve() {
    if (this.lastReference2 == null) {
      this.lastReference2 = this.lastReference;
    }
    if (this.builds == null) {
      this.builds = new ArrayList<AbstractBuildReference>();
    }
    if (this.buildRefs != null) {
      for (List<AbstractBuildReference> buildReferences : buildRefs.values()) {
        this.builds.addAll(buildReferences);
      }
    }
    if (this.getLastReference() == null) {
      this.lastReference2 = new StaticBuildReference(this.buildName, this.buildNumber, Result.NOT_BUILT);
    }
    return this;
  }

  /**
   * Gets a string for all of the build numbers
   *
   * @param refs
   *          List of build references to process.
   * @param separator
   * @return String containing all the build numbers from refs, never null but
   *         can be empty
   */
  private String getBuildNumbersString(List<AbstractBuildReference> refs, String separator) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;

    for (AbstractBuildReference s : refs) {
      if (s.getBuildNumber() != 0) {
        if (first) {
          first = false;
        } else {
          buf.append(separator);
        }
        buf.append(s.getBuildNumber());
      }
    }
    return buf.toString();
  }

  /**
   * Get a list of projects as a string using the separator
   *
   * @param separator
   * @return list of projects separated by separator
   */
  protected String getProjectListString(String separator) {
    Set<String> refs = getProjectsWithBuilds();
    StringBuilder buf = new StringBuilder();
    boolean first = true;

    for (String s : refs) {
      if (first) {
        first = false;
      } else {
        buf.append(separator);
      }
      buf.append(s.replaceAll("[^a-zA-Z0-9]+", "_"));
    }
    return buf.toString();
  }

  /**
   * Gets the unique set of project names that have a linked build.
   *
   * @return Set of project names that have at least one build linked.
   */
  private Set<String> getProjectsWithBuilds() {
    Set<String> projects = new HashSet<String>();

    for (AbstractBuildReference br : this.builds) {
      if (br.getBuildNumber() != 0) {
        projects.add(br.getProjectName());
      }
    }
    return projects;
  }

  private AbstractBuildReference getLastReference() {
    // need to update last reference based on latest state of the references
    for (int i = this.builds.size() - 1; i >= 0; --i) {
      AbstractBuildReference br = this.builds.get(i);
      if (br.getBuildNumber() != 0) {
        lastReference2 = br;
        break;
      }
    }
    return lastReference2;
  }

  public void updateReferences() {
    for (AbstractBuildReference br : this.builds) {
      br.update();
    }
  }
}
