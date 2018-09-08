# documentation generator | tome

https://github.com/korlibs/korte

- liquid templating
- markdown or html -> user choice

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

## Actors
- use Actors instead of synchronized mutable lists
https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#actors
since the actor is in its own coroutine context it can modify its private state without locking issues
