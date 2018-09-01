rootProject.name = "voodoo"

include("base", "base:cmd")
include("core")
include("multimc", "multimc:installer")
include("util")
include("fuel-coroutines")
include("importer", "builder", "pack", "pack-test", "voodoo")
include("server-installer")
include("bootstrap")

include("Jankson")

include("skcraft:launcher")
include("skcraft:launcher-builder")