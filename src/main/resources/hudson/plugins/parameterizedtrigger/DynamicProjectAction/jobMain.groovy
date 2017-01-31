package hudson.plugins.parameterizedtrigger.DynamicProjectAction

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
