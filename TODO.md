# documentation generator | tome

<s>
https://github.com/korlibs/korte

- liquid templating
- markdown or html -> user choice
</s>

use html-dsl in .kts files
maybe css-dsl
search for markdown-dsl

# deprecate flat entries

entries are flattened fast enough from the nested format, thee is no reason for a multi step process anymore
keep flat entries on disk for debugging purposes only

# cursemods generated constants / enum

https://github.com/square/kotlinpoet

generate 
```kotlin
enum class CurseMods(id: Int) {
    SomeMod(1234),
    OtherMod(6543)
}
```

# curse import -> kotlinpoet ?

https://github.com/square/kotlinpoet
requires more knowledge of how packs are going to be written
and imports are going to work

generate the NestedPack

# config tweaks

move more deployment options into the modpack configuration
exampeles:
 - skcraft
 - multimc
 - curse
   deployment: `id`, `name`, `description`

# multim mc integration

trigger by holding **shift**
figre out alternative ways of detecting keyboard state at startup

options:
  - change feature selection
  - force reinstall
  
  
sort out windows file locking issues


# misc

curse-server zip export

# coroutines

replace fuel with ktor-http client
replace Jackson with kotlinx-serialization

## Actors
- use Actors instead of synchronized mutable lists
https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#actors
since the actor is in its own coroutine context it can modify its private state without locking issues
