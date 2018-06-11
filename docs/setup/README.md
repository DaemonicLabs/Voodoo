[index](../../)

# Setup

Lets make a Pack.. 
we call it Awesome Pack

```yaml
title: Awesome Pack
```

every pack also needs a simple name, preferably without spaces

```yaml
name: awesomePack
```

we know its gonna be for 1.12.2

```yaml
mcVersion: 1.12.2
```

and we want to use forge.. for now just the recommended version
this supports `latest`, `recommended` or the build number eg. `2705`

```yaml
forge: recommended
```

lets make sure your name is saved on the pack..

```yaml
authors:
 - SomeDude
 - OtherDude
```

we also need to have a pack version.. just to keep track of changes and releases..
lets start at 1.0


```yaml
version: 1.0
```

where will the pack find files ? that means.. configs, scripts, more configs and a bunch of config files

```yaml
minecraftDir: .minecraft
```

this means it will look for a folder called `.minecraft` from wherever you invoke voodoo later,
keep in mind this is a relaitve path .. full paths are supüported but discouraged as it makes any pack
configuration very specific to one machine


one optional step is to define userFiles,
on supported platforms these will be user editable, any other files will be automatically reset to what the pack defines

explude rules are useful if your include rules are too broad and you want to exclude jsut some of the files

```yaml
  include:
  - options.txt
  - quark.cfg

  exclude: []

```



# Adding Mods

Now we are getting to the interesting bits, before we can add mods though i need to explain to you how the nested
format works

lets take a simple sample like this

```yaml
root:
  validMcVersions: [1.12.1, '1.12']
  curseOptionalDependencies: false
  curseReleaseTypes: [ alpha, beta, release ]
  entries:

  - Thermal Dynamics
  - Thermal Expansion
  - Thermal Innovation

  - curseReleaseTypes: [ beta, release ]
    # because alphas are buggy
    entries:
    - RFTools
    - RFTools Dimensions
```

the root is always a single entry.. any entry can have child entries and all properties of the entry are assigned to its
children
in this case we use alpha-release on a broad scope but specify the RFTools mods to only use releases and beta versions

## simple string and name notation

so you have seen we can refer to mods by just their name.. which is usually the projectname on curse,
but what if we want to specify more info ?

```yaml
- OpenComputers
```

is gonna be expanded to something like this

```yaml
- name: OpenComputers
```

so you can also add flags

```yaml
- name: OpenComputers
  version: 1.2.3
```

this would restrict the files it downloads to those containing that string in the filename
this is just a example.. all properties of a entry can be set like this


## Providers

well only having curse available would be boring..
so we also feature jenkins, direct urls, local files and the forge updateJson format

for brevity i will just showcase some

### Direct

Direct Entries require a url along with the name.. so a short notation is not possible

```yaml
- provider: DIRECT
  entries:

  - url: https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar
    name: Nutrition

  - url: https://centerofthemultiverse.net/launcher/mirror/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar
    name: Better Builder's Wands
```

### Jenkins

Kinda self explainging.. jenkins needs the base url and the name of the job
also supported but now shows is the `jenkinsFileNameRegex` it has a sane default but maybe you need to match a
different file or make sure your generated docs are not used as mod jar? then use that

´´´yaml
- provider: JENKINS
  jenkinsUrl: https://ci.elytradev.com
  entries:

  - job: elytra/MagicArsenal/master
    name: Magic Arsenal

  - job: elytra/FruitPhone/1.12.2
    name: FruitPhone

  - job: elytra/ProbeDataProvider/1.12
    name: ProbeDataProvider
```

### Local

you can install files from your local filesystem into the pack, but before that these files will
be packaged and cannot be downloaded by users from anywhere else.. so they increase the modpack size,
they also make pack dev less portable

anyway.. local entries will take a relative path and pull the file from the `localDir`

```
localDir: local

root:
  validMcVersions: [1.12.1, '1.12']
  curseOptionalDependencies: false
  curseReleaseTypes: [ alpha, beta, release ]
  entries:

  - provider: LOCAL
    name: SomeMod
    fileSrc: someMod/build/libs/SomeMod-1.0.jar

```

## Sides

you needed to make 2 packs .. one for clients and one for the server?
well forget that crap..

Voodoo will automatically only install the mods for the chosen side on export / packaging step for you

### Clientside

just some of the basics.. all pulled from curse

```yaml
- side: CLIENT
  entries:
  - "IKWID (I Know What I'm Doing)"
  - Wawla - What Are We Looking At
  - Waila Harvestability
  - JEI Integration
  - AppleSkin
  - BetterFps
  - NoNausea
  - Better Placement
  - Controlling
  - Default Options
  - Fullscreen Windowed (Borderless) for Minecraft
  - Mod Name Tooltip
  - Neat
  - ReAuth
  - CleanView
  - Vise
  - Smooth Font
```

### Serverside

Backup Solution and universal chatbridge

```yaml
- side: SERVER
  entries:
  - "BTFU continuous rsync incremental backup"
  - SwingThroughGrass

  - job: elytra/MatterLink/master
    name: MatterLink
```

## Optionals

But what if some of my players don't like `insert mod here`
Well then.. you are probably gonna love the optional features

to make any entry into a optional feature.. just add `feature: { selected: treu/false }`
you can give it a description too

the `recommendation` property can be set to to `starred`, `avoid`
the default value is to show no preference

```yaml
- feature:
    selected: true
    recommendation: starred
  entries:

    - name: JourneyMap
      description: "You know what this is. Only disable if you really need to save RAM or don't like minimaps."

    - name: SmoothWater
      description: "Makes the surface of water nicer, better underwater lighting, can cost some FPS."

    - name: Client Tweaks
      description: "Various client related fixes and tweaks, all in a handy menu."

    - name: Mouse Tweaks
      description: "Add extra mouse gestures for inventories and crafting grids."

- feature:
    selected: false
  entries:

    - name: Item Scroller
      description: Alternative to MouseTweaks

    - name: Fancy Block Particles
      description: "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps."

    - name: Keyboard Wizard
      description: Visual keybind editor.

    - name: Xaero's Minimap
      description: Lightweight alternative to JourneyMap
```

finally we can not only download mods.. but also resource packs

```
- name: Unity
  fileName: Unity.zip

- name: Slice
  provider: LOCAL
  folder: resourcepacks
  fileSrc: ressourcepacks/Slice.zip
```

this also showcases you can modify the filename and/or the target folder of files
the filename of Unity we modify because then we can provide default options that have Unity.zip enabled by default

the pack definition file may now look like [samples/awesomepack.yaml](https://github.com/elytra/Voodoo/blob/master/samples/awesomepack.yaml)

continue with [Build the Pack](../building)