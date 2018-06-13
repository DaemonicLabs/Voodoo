Want to make a modpack ? quickly test locally and then make it available on multiple platforms for users ?

voodoo is a set of tools to quickly prepare, build, test and deploy modpacks to users and servers

Voodoo might be for you if: 

:heavy_check_mark: You want a fast and mostly automated process  
:heavy_check_mark: You want to be able to update the modpack whenever you want  
:heavy_check_mark: You don't want to depend on anyone else  

or

:heavy_check_mark: You already use SKCraft Launcher  

It may not be for you if:

:small_orange_diamond: You do not feel comfortable using a shell  
:small_orange_diamond: You don't have a website or place for people to download files from  
:small_orange_diamond: You don't want anything to do with distributing the launcher or pack  

This applies to different modules of voodoo individually.. without a place to host files you can still export
a pack and upload it to curse, it will just be a much slower process due to approval and waiting time

## What We Do Right

Creating Modpacks with Voodoo requires minimal effort, just create one `yaml` definition

You can Test Any pack in MultiMC, creating a instance and launching it is completely automated, no more clicking than necessary

Modern Minecraft versions (1.6.+) and Forge are supported

Uses [SKCraft Launcher](https://github.com/SKCraft/Launcher#skcraft-launcher) Pack Format, but download all files,
dependencies and configures all file input based on the `yaml` definition

Reproducability: with a modpacks `.lock.json` file you can reproduce the modpack on any platform, server install or local testing
(given that the urls do not get taken down)

Minimalistic Packs: on all platforms that support it (SK and the multimc-wrapper) mods will be downloaded by the user from the original location,  
**No Rehosting of Mods!** completely automated by preparing `.url.txt` files for SKLauncher

## Docs

https://elytra.github.io/Voodoo

## Guides

- [Setup](docs/setup)
- [Building](docs/building)
- [Testing](docs/testing)

## Get it

### download

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.elytradev.com/job/elytra/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build)](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/artifact/)

`-fat` files are not modified by proguard in case something breaks randomly, \
but please report those errors too

### build

unix: `./gradlew build`  
windows: `./gradlew.bat build`

## usage

examples based on [Center of the Multiverse](https://github.com/elytra/Center-of-the-Multiverse)

other samples: [samples](samples)

flatten the yaml (this creates the main json) \
`java -jar voodoo.jar flatten cotm.yaml`

update the pack and write out the lockfile \
`java -jar voodoo.jar build cotm.json -o cotm.lock.json --force`

to update just a few mods in the update step \
`java -jar voodoo.jar build cotm.json -o cotm.lock.json -E Correlated -E "Magic Arsenal"`

compile the pack for sklauncher \
`java -jar pack.jar cotm.lock.json sk` \
now you can just upload the contents of `workspace/_upload`

## Server Deployment

create a server package \
`java -jar pack.jar cotm.lock.json server`

that creates a server *package* in `.server/`
 1. upload that package to **different** folder on your minecraft server
 2. stop the minecraft server and
 3. execute the server installer with the actual location of your minecraft server installation

this will:
 - update configs/files
 - install mods
 - install/update forge
 - create `forge.jar`

## MultiMC Integration / Deployment

To run a test instance use \
`java -jar voodoo.jar test cotm.lock.json mmc`

to compile a minimalistic MMC pack that selfupdates \
`java -jar voodoo.jar pack cotm.lock.json mmc` \
this expects a file `multimc/${packname}.url.txt` that points at the previously uploaded
skcraft data \
more specifically the json file of the pack \
eg: `https://centerofthemultiverse.net/launcher/cotm.json` in `cotm.url.txt`
optionally you can provide `multimc/$packname.icon.png` and the resulting pack will have a less default look
