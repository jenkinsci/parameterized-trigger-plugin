package hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;

def builds = my.triggeredBuilds
if(builds.size() > 0) {
	h2("Subproject Builds");

	ul(style:"list-style-type: none;") {
		for (item in builds) {
			li {
				if (item != null) {
					a(href:"${rootURL}/${item.project.url}", class:"model-link") {
						text(item.project.displayName)
					}
					a(href:"${rootURL}/${item.url}", class:"model-link") {
						img(src:"${imagesURL}/16x16/${item.buildStatusUrl}",
								alt:"${item.iconColor.description}", height:"16", width:"16")
						text(item.displayName)
					}
				}
			}
		}
	} 
}

def projects = my.triggeredProjects
if (projects.size() > 0) {
	h2("Subprojects triggered but not blocked for");

	ul(style:"list-style-type: none;") {
		for (item in projects) {
			li {
				print "${item}"
				if (item != null) {
					a(href:"${rootURL}/${item.url}", class:"model-link") {
						text(item.displayName)
					}
				}
			}
		}
	}
}

