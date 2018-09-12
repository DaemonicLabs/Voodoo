package voodoo.data.curse

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName

/**
 * Created by nikky on 26/05/18.
 * @author Nikky
 */
enum class Section {
    @JsonProperty("modpacks")
    @SerialName("modpacks")
    modpacks,
    @JsonProperty("mc-mods")
    @SerialName("mc-mods")
    `mcmods`,
    @JsonProperty("texture-packs")
    @SerialName("texture-packs")
    `texture-packs`,
    @JsonProperty("worlds")
    @SerialName("worlds")
    worlds;
}