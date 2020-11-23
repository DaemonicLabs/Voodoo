package voodoo.config

import com.github.ricky12awesome.jss.encodeToSchema
import voodoo.data.nested.NestedPack
import voodoo.pack.VersionPack
import voodoo.util.json

fun NestedPack.Companion.generateSchema() = json.encodeToSchema(serializer())
    .replace("\"replace_with_curseforge_projects\"",
        Autocompletions.curseforge.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_forge_versions\"",
        Autocompletions.forge.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_intermediaries\"",
        Autocompletions.fabricIntermediaries.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_loaders\"",
        Autocompletions.fabricLoaders.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_installers\"",
        Autocompletions.fabricInstallers.keys.joinToString(",") { "\"$it\"" }
    )

fun VersionPack.Companion.generateSchema(overridesKeys: Set<String>) = json.encodeToSchema(serializer())
    .replace("\"replace_with_overrides\"",
        overridesKeys.joinToString(",") { "\"${it}\"" }
    )
    .replace("\"replace_with_curseforge_projects\"",
        Autocompletions.curseforge.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_forge_versions\"",
        Autocompletions.forge.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_intermediaries\"",
        Autocompletions.fabricIntermediaries.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_loaders\"",
        Autocompletions.fabricLoaders.keys.joinToString(",") { "\"$it\"" }
    )
    .replace("\"replace_with_fabric_installers\"",
        Autocompletions.fabricInstallers.keys.joinToString(",") { "\"$it\"" }
    )