package hudson.plugins.parameterizedtrigger.SubProjectsAction

import hudson.Functions

def f=namespace(lib.FormTagLib)
def j=namespace(lib.JenkinsTagLib)
def l=namespace(lib.LayoutTagLib)

def actions = my.subProjectActions
if (!actions.empty) {
    h2("Subprojects")
    my.subProjectActions.each { action ->
        ul(style:"list-style-type: none;") {
            action.configs.each { config ->
                config.getProjectList(my.project.parent, null).each { project ->
                    if (Functions.hasPermission(project, project.READ)) {
                        li {
                            j.jobLink(job:project)
                            text("(${config.block == null ? 'non-blocking' : 'blocking'})")
                        }
                    }
                }
            }
        }
    }
}