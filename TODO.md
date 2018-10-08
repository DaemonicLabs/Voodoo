# documentation generator | tome

use html-dsl in .kts files
maybe css-dsl
search for markdown-dsl

copy generated files into github pages docs folder
or other targets

<s>
https://github.com/korlibs/korte

- liquid templating
- markdown or html -> user choice
</s>

# updateall

formalize behaviour

- update all
- update selected entries

# generate gradle setup

requires testing the gradle setup before generating it

- create framework
- add pack

- update kscript annotations/header

# deprecate flat entries

entries are flattened fast enough from the nested format, thee is no reason for a multi step process anymore
keep flat entries on disk for debugging purposes only

# curse import -> kotlinpoet ?

https://github.com/square/kotlinpoet
requires more knowledge of how packs are going to be written
and imports are going to work

generate the NestedPack

# config tweaks

move more deployment options into the modpack configuration
examples:
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

ensure proper use of CoroutineContext everzwhere

## Actors
- use Actors instead of synchronized mutable lists
https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#actors
since the actor is in its own coroutine context it can modify its private state without locking issues
