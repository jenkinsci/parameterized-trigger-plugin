package hudson.plugins.parameterizedtrigger.SubProjectsAction

import hudson.Functions

def f=namespace(lib.FormTagLib)
def j=namespace(lib.JenkinsTagLib)
def l=namespace(lib.LayoutTagLib)

def actions = my.subProjectActions
if (!actions.empty) {
    h2(_("Subprojects"))
    my.subProjectActions.each { action ->
        ul(style:"list-style-type: none;") {
            action.configs.each { config ->
                config.getProjectInfo(my.project).eachWithIndex { projectList, i  ->
                    if (!projectList.empty) {
                        switch (i) {
                            case 0:
                                h3(_("Static"))
                                break
                            case 1:
                                h3(_("Dynamic"))
                                break
                            case 2:
                                h3(_("Other executed recently"))
                                break
                            case 3:
                                h3(_("Unresolved"))
                                break
                            default:
                                h3("")
                        }
                    }
                    projectList.each { project ->
                        if(i<3) {
                            if (Functions.hasPermission(project, project.READ)) {
                                li {
                                    j.jobLink(job:project)
                                    text(_("(${config.block == null ? 'non-blocking' : 'blocking'})"))
                                }
                            }
                        } else {
                            li {
                                text(project)
                            }
                        }
                    }
                }
            }
        }
    }
}