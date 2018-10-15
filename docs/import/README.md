[index](../../)

# Import

Importing packs from curseforge

## Import from curse zip

```bash
./gradlew import --id somepack --url https://minecraft.curseforge.com/projects/someproject/files/1234567/download
```

only clientside mods can be imported, potentially filenames could be matched against mods in the server pack when available..
but userfriendlyness and failsafe are exclusive features here
