package voodoo.util.jenkins

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get

/**
 * Created by nikky on 03/02/18.
 * @author Nikky
 * @version 1.0
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Build(
        val number: Int,
        val url: String
) {
    fun details(): BuildWithDetails? {
        val r = get(url + "/api/json")
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BuildWithDetails(
        val number: Int,
        val url: String,
        val artifacts: List<Artifact>
)
