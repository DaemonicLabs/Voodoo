package voodoo.provider

import mu.KLogging

// TODO: use sealed classes
// enum class Provider(val base: ProviderBase) {
//    CURSE(CurseProvider),
//    DIRECT(DirectProvider),
//    LOCAL(LocalProvider),
//    JENKINS(JenkinsProvider),
//    JSON(UpdateJsonProvider)
// }

object Providers : KLogging() {
    private val providers = hashMapOf<String, ProviderBase>()

    init {
        register(
            "CURSE" to CurseProvider,
            "JENKINS" to JenkinsProvider,
            "DIRECT" to DirectProvider,
            "JSON" to UpdateJsonProvider,
            "LOCAL" to LocalProvider
        )
    }

    fun register(vararg pairs: Pair<String, ProviderBase>) {
        pairs.forEach { (key, provider) ->
            providers[key.toUpperCase()]?.let { existing ->
                logger.warn("overriding existing provider ${existing.name}")
            }

            providers[key.toUpperCase()] = provider
        }
    }

    operator fun get(key: String) = providers[key.toUpperCase()]
        ?: throw IllegalArgumentException("cannot find provider for key '${key.toUpperCase()}'")

    fun getId(provider: ProviderBase): String? {
        for ((id, registeredProvider) in providers) {
            if (provider == registeredProvider) {
                return id
            }
        }
        logger.error("found no matching registered provider")
        return null
    }
}