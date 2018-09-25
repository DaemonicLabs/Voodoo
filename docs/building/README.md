[index](../../)

# Building

## Requirements

assuming you have a `.kt` file ready to use from
[Basics](../basics)

## Building

### kscript
```bash
# invoking kscript directly
kscript awesomepack.kt build
```
```bash
# with kscript in the shebang this is possible
./awesomepack.kt build
```

### gradle

#### with application plugin

```kotlin
application {
    mainClassName = "awesomepackKt"
}
```
can be executed via
```bash
./gradlew run --args "build"
```

#### with separate JavaExec task
```kotlin
task<JavaExec>("awesomepack") {
    classpath = sourceSets["main"].runtimeClasspath
    main = "awesomepackKt"
    this.description = "Awesome pack"
    this.group = "application"
}
```
can be executed via

```bash
./gradlew awesomepack --args "build"
```

continue with [Testing the Modpack](../testing)