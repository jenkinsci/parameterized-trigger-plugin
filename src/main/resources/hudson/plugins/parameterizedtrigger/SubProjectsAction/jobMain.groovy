package hudson.plugins.parameterizedtrigger.SubProjectsAction

import hudson.Functions

import static hudson.plugins.parameterizedtrigger.ParameterizedTriggerUtils.DISABLE_ACTION_VIEWS_KEY

if (System.getProperty(DISABLE_ACTION_VIEWS_KEY) != null) {
    return
}

def j=namespace(lib.JenkinsTagLib)

def actions = my.subProjectActions
if (!actions.empty) {
    h2(_("Subprojects"))
    my.subProjectActions.each { action ->
        ul(style:"list-style-type: none;") {
            action.configs.each { config ->
                def projectInfo =  config.getProjectInfo(my.project)
                [projectInfo.fixed, projectInfo.dynamic, projectInfo.triggered].eachWithIndex { projectSet, i ->
                    if (!projectSet.empty) {
                        h3(_(["Static","Dynamic","Other executed recently"][i]))
                        projectSet.each { project ->
                            if (Functions.hasPermission(project, project.READ)) {
                                li {
                                    j.jobLink(job:project)
                                    text(_("(${config.block == null ? 'non-blocking' : 'blocking'})"))
                                }
                            }
                        }
                    }
                }
                if (!projectInfo.unresolved.empty) {
                    h3(_("Unresolved"))
                    projectInfo.unresolved.each  { project ->
                        li {
                            text(project)
                        }
                    }
                }
            }
        }
    }
}
