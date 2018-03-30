package voodoo.pack.sk

data class SKLocation(
        var location: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SKLocation) return false

        return location == other.location
    }
}