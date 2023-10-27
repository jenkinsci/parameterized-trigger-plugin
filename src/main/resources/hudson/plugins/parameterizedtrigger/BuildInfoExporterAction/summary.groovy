package hudson.plugins.parameterizedtrigger.BuildInfoExporterAction

import static hudson.plugins.parameterizedtrigger.ParameterizedTriggerUtils.DISABLE_ACTION_VIEWS_KEY

if (System.getProperty(DISABLE_ACTION_VIEWS_KEY) != null) {
	return
}

def l = namespace(lib.LayoutTagLib)

def builds = my.triggeredBuilds
if(builds.size() > 0) {
	h2("Subproject Builds")

	ul(style:"list-style-type: none;") {
		for (item in builds) {
			li {
				if (item != null) {
					a(href:"${rootURL}/${item.project.url}", class:"model-link") {
						text(item.project.displayName)
					}
					a(href:"${rootURL}/${item.url}", class:"model-link") {
						l.icon(class: "${item.iconColor.iconClassName} icon-sm", alt:"${item.iconColor.description}")
						text(item.displayName)
					}
				}
			}
		}
	} 
}

def projects = my.triggeredProjects
if (projects.size() > 0) {
	h2("Subprojects triggered but not blocked for")

	ul(style:"list-style-type: none;") {
		for (item in projects) {
			li {
				if (item != null) {
					a(href:"${rootURL}/${item.url}", class:"model-link") {
						text(item.displayName)
					}
				}
			}
		}
	}
}

