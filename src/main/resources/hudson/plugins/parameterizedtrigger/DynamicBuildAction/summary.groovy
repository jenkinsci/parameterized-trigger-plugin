package hudson.plugins.parameterizedtrigger.DynamicBuildAction

def f=namespace(lib.FormTagLib)
def j=namespace(lib.JenkinsTagLib)
def l=namespace(lib.LayoutTagLib)


def acts = my.builds
if (!acts.empty) {
    h2(_("Dynamic downstream projects"))
    h3("build count " + acts.size())
    my.builds.each { build ->
        ul(style: "list-style-type: none;") {
            li {
                if (null == build) {
                    text("Build is null")
                } else {
                    def prj = build.getProject()
                    text("build")
                    text(build.getDisplayName())
                    text(" number ")
                    text(build.getNumber())
                    if (null == prj) {
                        text("Project is null")
                    } else {
                        a(href: "${rootURL}/${build.getProject().getUrl()}", class: "model-link") {
                            img(src: "${imagesURL}/16x16/${build.getResult().color.getImage()}",
                                    alt: "${build.getResult().toString()}", height: "16", width: "16")
                            text(build.getProject().getFullDisplayName())
                        }
                        text("   ")
                        a(href: "${rootURL}/${build.url}", class: "model-link") {
                            text("build " + build.getDisplayName())
                        }
                    }
                }
            }
        }
    }
}
