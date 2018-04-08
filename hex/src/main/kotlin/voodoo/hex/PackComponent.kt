package voodoo.hex

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PackComponent(
        var uid: String = "",
        var version: String = "",
        var cachedName: String = "",
        var cachedRequires: Any? = null,
        var cachedVersion: String = "",
        var important: Boolean = false,
        var cachedVolatile: Boolean = false,
        var dependencyOnly: Boolean = false
)