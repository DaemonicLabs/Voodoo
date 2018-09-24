package voodoo.data.curse

import kotlinx.serialization.SerialName

/**
 * Created by nikky on 26/05/18.
 * @author Nikky
 */
enum class Section {
    @SerialName("modpacks")
    MODPACKS,
    @SerialName("mc-mods")
    MCMODS,
    @SerialName("texture-packs")
    TEXTURE_PACKS,
    @SerialName("worlds")
    WORLDS;
}