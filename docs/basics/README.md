[index](../../)

# Basics

## Requirements

assuming you have your gradle set up and a `.voodoo.kts` file ready
[Setup](../setup)

every pack needs a id, preferably lowercase and without spaces, the id is extracted from the filename
for this guide we will pick the id `awesomepack`

you can start out with a kotlin script file `packs/awesomepack.voodoo.kts`)

for the purpose of this guide we will only look at the pack creation and skip documentation, modlist generation, etc

**required** we know its gonna be for minecraft 1.12.2

```kotlin
mcVersion = "1.12.2"
```

the rest of the properties are optional

The pack will be called `Awesome Pack`

```kotlin
title = "Awesome Pack"
```


and we want to use forge.. for now just the recommended version
this supports `latest`, `recommended` or the build number eg. `2705`

```kotlin
forge = Forge.recommended
```

lets make sure you ae properly credited too..

```kotlin
authors = listOf("SomeDude", "OtherDude")
```

we also need to have a pack version.. to keep track of changes and releases..
lets start at 1.0

```kotlin
version = "1.0"
```

this means it will look for a folder called `src` relative to `root`


our pack now looks like this

```kotlin
mcVersion = "1.12.2"
title = "Awesome Pack"
version = "1.0"
forge = Forge.recommended
authors = listOf("SomeDude", "OtherDude")
```

one optional step is to define userFiles,
on supported platforms these will be user editable, any other files will be automatically reset to what the pack defines

exclude rules are useful if your include rules are too broad and you want to exclude just some of the files

```kotlin
userFiles = UserFiles(
    include = listOf(
        "options.txt",
        "quark.cfg",
        "foamfix.cfg"
    ),
    exclude = listOf("")
)
```


# Adding Mods

Now we are getting to the interesting bits, entries are being added in a nested format 
that applies all properties of parents to children unless specifically redefined

lets take a simple sample like this

```kotlin
root = rootEntry(CurseProvider) {
    validMcVersions = setOf("1.12.1", "1.12")
    optionals = false
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
    list {
        add(Mod.thermalDynamics)
        add(Mod.thermalexpansion)
        add(Mod.thermalInnovation)
        
        group {
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
        }.list {
            addMod.rftools)
            add(Mod.rftoolsDimensions)
        }
    }
}
```

the root is always a single entry.. any entry can have child entries and all properties of the entry are assigned to its
children
in this case we use alpha-release on a root scope but specify the RFTools mods to only use releases and beta versions

## Providers

well only having curse available would be boring..
so we also feature jenkins, direct urls, local files and the forge updateJson format

for brevity i will just showcase some

### Direct

Direct Entries require a url along with the name.. so a short notation is not possible

```kotlin
withProvider(DirectProvider).list {
    +"betterBuilderWands" configure {
        name = "Better Builder's Wands"
        url = "https://centerofthemultiverse.net/launcher/mirror/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar"
    }
    // inline url declration
    +"nutrition" url "https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar"
}
```

### Jenkins

Kinda self explaining.. jenkins needs the base url and the name of the job
also supported but now shows is the `jenkinsFileNameRegex` it has a sane default but maybe you need to match a
different file or make sure your generated docs are not used as mod jar? then use that

```kotlin
withProvider(JenkinsProvider) {
    jenkinsUrl = "https://ci.elytradev.com"
}.list { // Jenkins provider context
    +"fruitPhone" job "elytra/FruitPhone/1.12.2"
    +"probeDataProvider" job "elytra/ProbeDataProvider/1.12"
    
    +"magicArsenal" configure {
        name = "Magic Arsenal"
        job = "elytra/MagicArsenal/master"
    }
    
    // without a job specfied, the id will be implicitely used as job
    +"elytra/MatterLink/master"
}
```

### Local

you can install files from your local filesystem into the pack, but before that these files will
be packaged and cannot be downloaded by users from anywhere else.. so they increase the modpack size,
they also make pack dev less portable

anyway.. local entries will take a relative path and pull the file from the `localDir`

```kotlin
withProvider(LocalProvider).list { // Local provider Context
    +"someMod" configure {
        name = "SomeMod"
        fileName = "SomeMod.jar"
        // relative to localDir
        fileSrc = "someMod/build/libs/SomeMod-1.0.jar"
    }
}
```

## Sides

you needed to make 2 packs .. one for clients and one for the server?
well forget that crap..

Voodoo will automatically install the mods for the chosen side on export / packaging step for you

### Clientside

just some of the basics.. all examples use curse mods, but they could be from any provider

```kotlin
group {
    side = Side.CLIENT
}.list {
    add(Mod.toastControl)
    add(Mod.wawlaWhatAreWeLookingAt)
    add(Mod.wailaHarvestability)
    add(Mod.jeiIntegration)
}
```

### Serverside

Backup Solutions and universal chatbridge

```kotlin
group {
    side = Side.SERVER
}.list {
    add(Mod.btfuContinuousRsyncIncrementalBackup)
    add(Mod.swingthroughgrass)
    add(Mod.colorchat)
    add(Mod.shadowfactsForgelin)
    
    withProvider(JenkinsProvider) {
        jenkinsUrl = "https://ci.elytradev.com"
    }.list {
        +"matterLink" configure {
            job = "elytra/MatterLink/master"
        } 
    }
}
```

## Optionals

But what if some of my players don't like `insert mod here`
Well then.. you are probably gonna love the optional features

to make any entry into a optional feature.. just add `feature: { selected: treu/false }`
you can give it a description too

the `recommendation` property can be set to to `starred`, `avoid`, `null`
the default value (`null`) is to show no preference

```kotlin
group {
    feature {
        selected = true
        recommendation = Recommendation.starred
    }
}.list {
    add(Mod.journeymap) configure {
        description =
            "You know what this is. Only disable if you really need to save RAM or don't like minimaps."
    }

    add(Mod.mage) description "Configurable graphics enhancements. Highly recomended."

    add(Mod.neat) configure {
        description = "Simple health and unit frames."
    }

    add(Mod.clientTweaks) configure {
        description = "Various client related fixes and tweaks, all in a handy menu."
    }

    add(Mod.mouseTweaks) configure {
        description = "Add extra mouse gestures for inventories and crafting grids."
    }
}
group {
    feature {
        selected = false
    }
}.list {
    add(Mod.itemScroller) configure {
        description = "Alternative to MouseTweaks."
    }

    add(Mod.xaerosMinimap) configure {
        description = "Lightweight alternative to JourneyMap."
    }

    add(Mod.minemenu) configure {
        description = "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
    }

    add(Mod.itemzoom) configure {
        description = "Check this if you like to get a closer look at item textures."
    } 
}
```

finally we can not only download mods.. but also resource packs

```kotlin
+TexturePack::unity configure {
    fileName = "Unity.zip"
    // curse resource packs are automatically 
    // set to use the correct folder
}

withProvider(LocalProvider).list {
    +"slice" configure {
        folder = "resourcepacks"
        fileSrc = "ressourcepacks/Slice.zip"
    }
}
```

this also showcases you can modify the filename and/or the target folder of files
we modify the filename of Unity because then we can provide default options that have Unity.zip enabled by default

the pack definition file may now look like [samples/awesomepack.voodoo.kts](https://github.com/elytra/Voodoo/blob/master/samples/awesomepack.voodoo.kts)

continue with [Build the Pack](../building)