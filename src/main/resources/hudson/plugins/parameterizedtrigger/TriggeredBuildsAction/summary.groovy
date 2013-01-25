package hudson.plugins.parameterizedtrigger.TriggeredBuildsAction

def f=namespace(lib.FormTagLib)
def j=namespace(lib.JenkinsTagLib)
def l=namespace(lib.LayoutTagLib)

def builds = my.triggeredBuilds

if (builds.empty) {
    return
}

h2("Subproject Builds");

ul(style:"list-style-type: none;") {
    builds.each({ build ->
    li {
        a(href:"${rootURL}/${build.project.url}", class:"model-link") {
            text(build.project.displayName)
        }
        a(href:"${rootURL}/${build.url}", class:"model-link") {
            img(src:"${imagesURL}/16x16/${build.buildStatusUrl}",
                    alt:"${build.iconColor.description}", height:"16", width:"16")
            text(build.displayName)
        }
    }
    })
}

