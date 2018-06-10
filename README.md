# voodoo

voodoo is a set of tools to quickly build and prepare modpacks for SKCraft launcher
and other launchers

## get it

### download

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.elytradev.com/job/elytra/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build)](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/artifact/)

`-fat` files are not modified by proguard in case something breaks randomly, \
but please report those errors too

### build

unix: `./gradlew build`
windows: `./gradlew.bat build`

## Docs

https://elytra.github.io/Voodoo

## usage
examples based on [Center of the Multiverse](https://github.com/elytra/Center-of-the-Multiverse)

other samples: [samples](/samples)

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
this expects a file `multimc/$packname.url.txt` that points at the previously uploaded
skcraft data \
more specifically the json file of the pack \
eg: `https://centerofthemultiverse.net/launcher/cotm.json` in `cotm.url.txt`
optionally you can provide `multimc/$packname.icon.png` and the resulting pack will have a less default look
