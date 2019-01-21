[index](../../)

# Building

## Requirements

assuming you have a `.voodoo.kts` file ready to use from
[Basics](../basics)

## Building with gradle

the voodoo plugin will detect any `*.kt` files in the configured pack folder
and generate gradle tasks for them  
`awesomepack.kt` woud generate the task name `awesomepack` it is always lowercase

it can be executed via

```bash
./gradlew awesomepack --args "build"
```

continue with [Testing the Modpack](../testing)