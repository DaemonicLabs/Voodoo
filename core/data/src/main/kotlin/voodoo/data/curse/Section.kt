package voodoo.data.curse

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by nikky on 26/05/18.
 * @author Nikky
 */
enum class Section {
    @JsonProperty("modpacks") MODPACKS,
    @JsonProperty("mc-mods") MCMODS,
    @JsonProperty("texture-packs") TEXTUREPACKS,
    @JsonProperty("worlds") WORLDS;
}