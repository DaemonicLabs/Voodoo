package voodoo.provider

import mu.KLogging
import voodoo.data.flat.Entry

object Providers : KLogging() {
    private val providers = hashMapOf<String, ProviderBase>()

    init {
        register(
            "CURSE" to CurseProvider,
            "JENKINS" to JenkinsProvider,
            "DIRECT" to DirectProvider,
            "LOCAL" to LocalProvider,
            "NOOP" to NoopProvider
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