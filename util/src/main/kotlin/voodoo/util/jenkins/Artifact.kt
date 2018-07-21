package voodoo.util.jenkins

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Created by nikky on 03/02/18.
 * @author Nikky
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Artifact(
        val displayPath: String,
        val fileName: String,
        val relativePath: String
)