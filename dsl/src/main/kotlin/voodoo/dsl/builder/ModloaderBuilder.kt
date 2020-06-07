package voodoo.dsl.builder

import voodoo.data.ModloaderPattern
import voodoo.dsl.VoodooDSL
import voodoo.fabric.InstallerVersion
import voodoo.fabric.IntermediaryVersion
import voodoo.fabric.LoaderVersion
import voodoo.property
import kotlin.reflect.KMutableProperty0
@VoodooDSL
class ModloaderBuilder(
    internal var mcVersion: String?,
    internal var modloader: ModloaderPattern
) {
    @VoodooDSL
    fun forge(version: String) {
        modloader = ModloaderPattern.Forge(
            version = version
        )
        if(mcVersion == null) {
            mcVersion = version.substringBefore('-')
        }
    }


    @VoodooDSL
    fun fabric(
        intermediary: IntermediaryVersion,
        loader: LoaderVersion? = null,
        installer: InstallerVersion? = null
    ) {
        modloader = ModloaderPattern.Fabric(
                intermediateMappingsVersion = intermediary.version,
                loaderVersion = loader?.version,
                installerVersion = installer?.version
            )

        if(mcVersion == null) {
            mcVersion = intermediary.version
        }
    }
}

