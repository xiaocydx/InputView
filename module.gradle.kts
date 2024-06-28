import groovy.json.JsonOutput
import groovy.json.JsonSlurper

apply<ModulePlugin>()

class ModulePlugin : Plugin<Settings> {

    override fun apply(settings: Settings) = settings.gradle.settingsEvaluated {
        val json = readSettingsJson() ?: return@settingsEvaluated
        applySettingsConfig(SettingsConfig.fromJson(json))
    }

    private fun Settings.readSettingsJson(): String? {
        val file = File(rootDir, "settings.json")
        if (!file.exists()) {
            file.writeText(SettingsConfig.defaultJson())
            return null
        }
        return file.readText()
    }

    private fun Settings.applySettingsConfig(config: SettingsConfig) {
        val moduleList = config.moduleList.filter {
            val useLocal = config.allUseLocal || it.useLocal
            useLocal && it.localPath.isNotEmpty() && it.remoteGav.isNotEmpty()
        }
        if (moduleList.isEmpty()) return

        val modulePath = mutableMapOf<SettingsConfig.Module, String>()
        moduleList.forEach {
            val projectDir = File(it.localPath)
            val path = ":${projectDir.name}"
            include(path)
            project(path).projectDir = projectDir
            modulePath[it] = path
        }

        gradle.projectsEvaluated {
            allprojects {
                configurations.all {
                    resolutionStrategy.dependencySubstitution {
                        moduleList.forEach {
                            val path = modulePath[it]!!
                            substitute(module(it.remoteGav)).using(project(path))
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsConfig(
    val allUseLocal: Boolean = false,
    val moduleList: List<Module> = emptyList()
) {

    data class Module(
        val useLocal: Boolean = false,
        val localPath: String = "",
        val remoteGav: String = ""
    )

    fun toJson(): String {
        val outcomeMap = mapOf(
            "allUseLocal" to allUseLocal,
            "moduleList" to moduleList.map {
                mapOf(
                    "useLocal" to it.useLocal,
                    "localPath" to it.localPath,
                    "remoteGav" to it.remoteGav,
                )
            }
        )
        return JsonOutput.prettyPrint(JsonOutput.toJson(outcomeMap))
    }

    companion object {

        fun defaultJson(): String {
            val moduleList = listOf(Module())
            return SettingsConfig(moduleList = moduleList).toJson()
        }

        fun fromJson(json: String): SettingsConfig {
            val jsonSlurper = JsonSlurper()
            val outcomeMap = jsonSlurper.parseText(json) as Map<*, *>
            val allUseLocal = outcomeMap["allUseLocal"] as? Boolean ?: false
            val moduleList = outcomeMap["moduleList"] as? List<*> ?: emptyList<Any>()
            return SettingsConfig(
                allUseLocal = allUseLocal,
                moduleList = moduleList.map { module ->
                    val moduleMap = module as? Map<*, *>
                    Module(
                        useLocal = moduleMap?.get("useLocal") as? Boolean ?: false,
                        localPath = moduleMap?.get("localPath") as? String ?: "",
                        remoteGav = moduleMap?.get("remoteGav") as? String ?: ""
                    )
                }
            )
        }
    }
}