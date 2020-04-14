# voodoo 2

## changelog v2

steps:
1. build  
   delete `.meta/${pack.id}`  
   generate `pack.meta.json` and `entries.meta.json`
2. pin version (create git tag)
3. generate changelogs  
   list git tags, filter by prefix
   `git archive -o "${tag}_meta.zip" "${tag}:.meta/${pack.id}"`  
   `git archive -o "${tag}Source.zip" "${tag}:${pack.id}"`  
   `unzip`

important: meta folder and packid make the meta files resolvable
files can be retrieved from old versions annotated by tag

## modules

- core  
  core definitions and DSL  
  all you need to create a pack from code
  - DSL
- code generators  
  for Forge / Mods / Minecraft versions generating .kt files
- script host  
  script definitions  
  depends on `code generations`



## workflow

script -> pack instructions -> fixed pack (serializable) -> packaging




# migrate hosting from jenkins to maven

- [ ] maven-publish shadowJars



routine to report errors:
- switch to main thread
- close all pools
- delay a second
- report error

change ServerPack to zip up pack
and ServerInstaller to unzip

# Tweaker

use a tweakClass for forge (bundle bootstrapper in tweakClass ?)

# Includes

- [x] allow splitting up modpack definition into multiple methods
- [x] add `@Include(file)` (file relative to include directory)
- [ ] add a wiki entry

# Customizing generated files

- [ ] read annotations
- [ ] customize generated Mod.kt, Forge.kt

# MCUpdater support

- [ ] manual xml building
  - [ ] use same urls as skcraft
  - [ ] add synthetic Modules for config and features
- [ ] fastpack
  - [ ] ensure url pointers work for fastpack
  - [x] download fastpack and execute it as `java -jar` process

# Scripting

- [x] Script Definition
- [x] parse script folder and init pack with filename as `id`
- [x] add extra scripts for defining tome / doc generators

# Reorganize buildscripts / Repo

- [x] no code in the root project
- [x] all dependencies in `buildSrc`
- [ ] move kotlin `src` one level higher ? (`src/main/kotlin` is unnecessary)

# documentation generator | tome

- [x] use html-dsl in .kts files, maybe css-dsl
- [ ] search for markdown-dsl or make one
- [x] copy generated files into docs folder

# diff / history

- [x] archive state of the pack from git history, using tags
- [x] unzip archive
- [x] load old pack
- [x] diff packs (output to a m.d file)
  - [x] diff entries
- [x] diff files (output as a .diff file)

# condense module graph

improve buildspeed

# analyze
## list optional dependencies gradle task

list all optional dependencies of curse mods
- curse mods only
- print code to copy-paste

use modalyzer on all mod jars

## suggest forge version

build first
use modalyzer output of all jars
suggest named forge versions (copy-paste ready)

# curse import

- match client and server pack contents to determine mods that are common or clientside
- currently impossible to find which projects mods are from that are server-only

# config tweaks

move more deployment options into the modpack configuration
examples:
 - [ ] skcraft
 - [x] multimc
 - [ ] curse
   deployment: `id`, `name`, `description`

**make sure to keep these options pack-specific and setup-agnostic**

# multim mc integration

trigger by holding **shift**
figure out alternative ways of detecting keyboard state   **without listeners**

options:
  - change feature selection
  - force reinstall


sort out windows file locking issues

# misc

curse-server zip export

# coroutines

ensure proper use of CoroutineContext / Scope everywhere

## Actors

- use Actors instead of synchronized mutable lists
https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#actors
since the actor is in its own coroutine context it can modify its private state without locking issues
