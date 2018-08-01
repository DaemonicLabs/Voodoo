[index](../../)

# Setup

Lets make a Pack.. 
we call it Awesome Pack

```yaml
title: Awesome Pack
```

every pack also needs a id, preferably lowercase and without spaces

```yaml
id: awesomepack
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
sourceDir: src
```

this means it will look for a folder called `src` relative to wherever you invoke voodoo later,
keep in mind this is a **relative** path .. full paths are supported but discouraged as it makes any pack
configuration very specific to one machine and non-portable


one optional step is to define userFiles,
on supported platforms these will be user editable, any other files will be automatically reset to what the pack defines

exclude rules are useful if your include rules are too broad and you want to exclude just some of the files

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
  provider: CURSE
  validMcVersions: [ 1.12.1, '1.12' ]
  curseOptionalDependencies: false
  curseReleaseTypes: [ alpha, beta, release ]
  entries:

  - thermal-dynamics
  - thermalexpansion
  - thermal-innovation

  - curseReleaseTypes: [ beta, release ]
    # because alphas are buggy
    entries:
    - rftools
    - rftools-dimensions
```

the root is always a single entry.. any entry can have child entries and all properties of the entry are assigned to its
children
in this case we use alpha-release on a broad scope but specify the RFTools mods to only use releases and beta versions

## simple string and name notation

so you have seen we can refer to mods by just their url slug.. which is usually the project name on curse lowercased and spaces replaced with `-`,

but what if we want to specify more info ?
the short form is gonna be expanded to the `id`

```yaml
- opencomputers

# is equivalent to

- id: opencomputers
```

so you can add more properties

```yaml
- id: opencomputers
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
    id: Nutrition

  - url: https://centerofthemultiverse.net/launcher/mirror/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar
    name: Better Builder's Wands
    id: better-builder-wands
```

### Jenkins

Kinda self explaining.. jenkins needs the base url and the name of the job
also supported but now shows is the `jenkinsFileNameRegex` it has a sane default but maybe you need to match a
different file or make sure your generated docs are not used as mod jar? then use that

```yaml
- provider: JENKINS
  jenkinsUrl: https://ci.elytradev.com
  entries:

  - job: elytra/MagicArsenal/master
    id: magic-arsenal
    name: Magic Arsenal

  - job: elytra/FruitPhone/1.12.2
    id: fuit-phone

  - id: elytra/ProbeDataProvider/1.12
    name: ProbeDataProvider
    
  - elytra/MatterLink/master
```
if a key `job` cannot be found then `id` will be used, which makes it possible to use a even shorter notation
`id` is ALWAYS required

### Local

you can install files from your local filesystem into the pack, but before that these files will
be packaged and cannot be downloaded by users from anywhere else.. so they increase the modpack size,
they also make pack dev less portable

anyway.. local entries will take a relative path and pull the file from the `localDir`

```yaml

localDir: local

root:
  validMcVersions: [1.12.1, '1.12']
  curseOptionalDependencies: false
  curseReleaseTypes: [ alpha, beta, release ]
  entries:

  - provider: LOCAL
    id: some-mod
    name: SomeMod
    fileSrc: someMod/build/libs/SomeMod-1.0.jar

```

## Sides

you needed to make 2 packs .. one for clients and one for the server?
well forget that crap..

Voodoo will automatically only install the mods for the chosen side on export / packaging step for you

### Clientside

just some of the basics.. all examples use curse, but they could be from any provider

```yaml
- side: CLIENT
  entries:
    - toast-control
    - wawla-what-are-we-looking-at
    - waila-harvestability
    - jei-integration
    - appleskin
    - betterfps
    - nonausea
    - better-placement
    - controlling
    - custom-main-menu
    - default-options
    - fullscreen-windowed-borderless-for-minecraft
    - mod-name-tooltip
    - reauth
    - cleanview
    - crafting-tweaks
```

### Serverside

Backup Solution and universal chatbridge

```yaml
- side: SERVER
  entries:
    - btfu-continuous-rsync-incremental-backup
    - swingthroughgrass
    - colorchat
    - shadowfacts-forgelin

    - id: elytra/MatterLink/master
      name: MatterLink
        dependencies:
          REQUIRED:
            - shadowfacts-forgelin
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

    - name: journeymap
      description: "You know what this is. Only disable if you really need to save RAM or don't like minimaps."

    - name: mage
      description: "Configurable graphics enhancements. Highly recomended."

    - name: neat
      description: "Simple health and unit frames."

    - name: client-tweaks
      description: "Various client related fixes and tweaks, all in a handy menu."

    - name: mouse-tweaks
      description: "Add extra mouse gestures for inventories and crafting grids."

- feature:
    selected: false
  entries:

    - name: item-scroller
      description: "Alternative to MouseTweaks."

    - name: xaeros-minimap
      description: "Lightweight alternative to JourneyMap."

    - name: minemenu
      description: "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."

    - name: itemzoom
      description: "Check this if you like to get a closer look at item textures."
```

finally we can not only download mods.. but also resource packs

```yaml
- id: unity
  fileName: Unity.zip

- id: Slice
  provider: LOCAL
  folder: resourcepacks
  fileSrc: ressourcepacks/Slice.zip
```

this also showcases you can modify the filename and/or the target folder of files
we modify the filename of Unity because then we can provide default options that have Unity.zip enabled by default

the pack definition file may now look like [samples/awesomepack.yaml](https://github.com/elytra/Voodoo/blob/master/samples/awesomepack.yaml)

continue with [Build the Pack](../building)