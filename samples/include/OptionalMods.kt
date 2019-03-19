import voodoo.dsl.builder.curse.CurseListBuilder
import voodoo.provider.CurseProvider

fun included() {
    println("i was included")
}

fun CurseListBuilder<CurseProvider>.addOptionalMods() {
    group {
        validMcVersions = setOf("1.12.2", "1.12.1", "1.12")
        optional {
            selected = false
        }
    }.list {
        +Mod.itemScroller configure {
            description = "Alternative to MouseTweaks."
        }
        +Mod.xaerosMinimap configure {
            description = "Alternative to MouseTweaks."
        }
        +Mod.minemenu configure {
            description =
                "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
        }
        +Mod.itemzoom configure {
            description = "Check this if you like to get a closer look at item textures."
        }
        +Mod.lightLevelOverlayReloaded configure {
            description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
        }
        +Mod.durabilityShow configure {
            description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
        }
        +Mod.fancyBlockParticles configure {
            description =
                "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
        }
        +Mod.dynamicSurroundings configure {
            description =
                "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
        }
        +Mod.rpgHud configure {
            description =
                "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"
        }
        +Mod.betterFoliage configure {
            description =
                "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
        }
        +Mod.keyboardWizard configure {
            description = "Visual keybind manager."
        }
        +Mod.chunkAnimator configure {
            description = "Configurable chunk pop-in animator."
        }
        +Mod.fasterLadderClimbing configure {
            description = "Helps you control ladder climb speed and allows you to go a bit faster."
        }

        // Resource packs
        +TexturePack.unity configure {
            fileName = "Unity.zip"
            description =
                "Multi-mod compatible resource pack. Very nice, but does have some broken textures here and there."
        }
    }
}