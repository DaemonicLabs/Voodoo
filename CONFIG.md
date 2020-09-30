
```

version = 1.1.2
mcVersion = 1.12.2
icon = ./icon.png
authors = NikkyAi, Stephie

selfupateurl = http://blub

multimc:MaxMemAlloc=4096
multimc:MinMemAlloc=2048

userfiles:include = config/quark.cfg
userfiles:exclude = blub
userfiles:flags = whatever

forge = 1.12.2-recommended
# or
fabric:intermediary = 1.16.2
fabric:loader = ...
fabric:installer = ...

curse:matterlink side=client foo=bar 

curse:matterlink \
    side=client \
    foo=bar 

curse:customId:123456

direct:micdoodlecore:http://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/MicdoodleCore-1.12.2-4.0.2.190.jar

side=client {
    jenkins:rebornStorage:http://jenkins.modmuss50.me/job/Team%20Reborn/job/RebornStorage/job/1.14.4/
}

```

obsidian fabric
```
title     = obsidian 1.16 Fabric Modpack
authors   = copygirl
version   = 0.1.0
icon      = icon.png

mcVersion = 1.16.2
fabric:intermediary = 1.16.2

selfupdateurl = http://meowface.org/copygirl/obsidian_fabric/obsidian_fabric.json

releaseTypes = release,beta,alpha \
validMcVersion = 1.16,1.16.1 \ # allow 1.16.* ?
invalidMcVersion = Forge \ # should be set by default by using fabric
{
    # Optimization and Fixes
    curse:lithium  # General-purpose server optimization
    # TODO: curse:phosphor # Optimize the lighting engine

    # Tweaks and Utilities
    curse:appleskin              # Displays hunger/saturation information
    curse:darkLoadingScreen      # Red is dead!
    curse:hwyla                  # Shows what you're looking at
    curse:roughlyEnoughItems     # Like JEI, displays recipes and uses
    curse:roughlyEnoughResources # Like JER, displays block and mob drops
    curse:shulkerboxtooltip      # Show shulker box contents when hovering over them

    # Gameplay Changes
    curse:anvilFix              # Removes the "Too Expensive" limit
    curse:chainsLink            # Pistons move connected chain blocks
    curse:easiervillagertrading # Trades are only one click away
    curse:fabricPassableLeaves  # No more jumping on trees!
    curse:fairenchanting        # Pay fairly in XP, rather than levels
    curse:grindEnchantments     # Use Grindstone to remove enchantments
    curse:stackablepotions      # Stack potions up to 16
    curse:suitablyStackableStew # Stews stack up to 16

    # ------------------
    # == CONTENT MODS ==
    # ------------------

    # World Generation
    curse:endRebellion               { common.validMcVersions = setOf("1.16.2") }
                                    # Content for the End
    # TODO: curse:heartOfTheMachine          # Dimension of abandoned machineries
    curse:ohTheBiomesYoullGoFabric   # Over 70 varied biomes
    curse:repurposedStructuresFabric # More structures and variants
    # TODO: curse:theBumblezoneFabric        # Dimension full of beeeeeees!

    # Decoration
    curse:adorn                     # Decorate your home!
    curse:bambooTweaks              # Bamboo building blocks
    curse:bedspreadsFabric          # Banners on beds
    curse:colorfulCut               # Slabs, stairs and walls made of concrete and terracotta
    curse:modernGlassDoors          # Adds glass door variants of Vanilla doors
    curse:ohHowTheCraftingHasTabled # Crafting Tables made of all the woods
    curse:woodsPlus                 # Additional wooden planks variants

    # Various
    # TODO: curse:antiqueAtlas           # Map mod in an item
    curse:aquarius               # Expanding on Aquatic Update content
    curse:campanion              # Camping companion mod adding various things
    curse:doubleJumpMod          # Enchantment (level I to III) that adds double-jumps
    curse:ducts                  # Expanding on Hoppers
    curse:immersivePortalsMod    # See-through portals and seamless travel
    curse:linkedStorage          # Basically Ender Storage
    curse:netheriteHorseArmor    # Upgrade diamond horse armor to netherite tier
    curse:respawnablePets        # Item that allows your pets to respawn when sleeping
    curse:rswires                # RedPower2-like redstones wires and bundled cables
    curse:sandwichable           # Make custom sandwiches with many ingredients
    curse:theParakeetMod         # More birbs!
    curse:towelette              # "Waterlog" most blocks with non-water fluids
    curse:wolvesWithArmor        # Adds armor for your (overly vulnerable) wolves
    curse:woodcutter             # Like the Stonecutter, but for wood!

    # Technology
    # TODO: curse:astromineMain        # Space! (Not compatible with Immersive Portals)
    # TODO: These mods (including Lacrimis) require Cotton Resources.
    # curse:biomechanics         # Meat Machines
    # curse:mechanizedSteamPower # Basically Flaxbeard's Steam Power

    # Magic
    # TODO: curse:lacrimis # Powered by Crying Obsidian

    # SERVER OPTIONAL MODS
    side = server \
    optional:selected = false \
    {
        # curse:btfuContinuousRsyncIncrementalBackup \
        #     name = "BTFU" \
        #     description = "Best backup mod in existence! (setup required)"
    }

    # CLIENT MODS
    side = Side.CLIENT {
        # CLIENT REQUIRED MODS
        curse:modmenu

        # CLIENT RECOMMENDED MODS
        optional:selected = true {
            curse:betterEnchantedBooks description = "Unique looks for different enchantment books"
            curse:controllingForFabric description = "Improves controls with search, showing conflicts"
            curse:dynamicFps           description = "Reduce FPS when window is in background"
            curse:illuminations        description = "Adds pretty-looking glowing parties"
            # curse:sodium               description = "Render engine replacement (high FPS boost)"
        }

        # CLIENT OPTIONAL MODS
        optional:selected = false {
            curse:dynamicSoundFilters description = "Affects how sounds are played depending on location"
            curse:itemScroller        description = "Use scroll wheel and other shortcuts to move items"
            curse:lightOverlay        description = "Show blocks mobs can spawn by pressing F7"
            curse:presenceFootsteps   description = "Footstep and shuffling sounds depending on materials"
            curse:voxelmap            description = "Minimap and world map"
            curse:xaerosMinimap       description = "Minimap to go along with Xaero's World Map"
            curse:xaerosWorldMap      description = "World map to go along with Xaero's Minimap"
        }
    }
}
```