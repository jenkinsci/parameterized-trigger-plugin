package hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;

def f=namespace(lib.FormTagLib)
def j=namespace(lib.JenkinsTagLib)
def l=namespace(lib.LayoutTagLib)

if (my.first) {
    h2("Subproject Builds");
}

def build = my.triggeredBuild

ul(style:"list-style-type: none;") {
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
}

