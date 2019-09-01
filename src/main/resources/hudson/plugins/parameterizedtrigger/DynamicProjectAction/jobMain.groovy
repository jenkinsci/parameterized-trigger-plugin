package hudson.plugins.parameterizedtrigger.DynamicProjectAction

import static hudson.plugins.parameterizedtrigger.ParameterizedTriggerUtils.DISABLE_ACTION_VIEWS_KEY

if (System.getProperty(DISABLE_ACTION_VIEWS_KEY) != null) {
    return
}

def acts = my.projects
if (!acts.empty) {
    h2(_("Dynamic downstream projects"))
    my.projects.each { project ->
        ul(style: "list-style-type: none;") {
            li {
                text(project)
            }
        }
    }
}
