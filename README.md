# voodoo

voodoo is a set of tools to quickly build and prepare modpacks for SKCraft launcher
and other launchers

## get it

### download

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.elytradev.com/job/elytra/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build)](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/artifact/)

### build

unix: `./gradlew build`
windows: `./gradlew.bat build`

## usage

### single command

`./voodoo.sh `

flatten the yaml (this created the main json)
`java -jar flatten.jar cotm.yaml`

update the pack and write out the lockfile
`java -jar builder.jar cotm.json -o cotm.lock.json --save`

create a sklauncher workspace for it
`java -jar pack.jar cotm.lock.json sk`


samples: [samples](/samples)

## MultiMC integration

WIP, currently being reimplemented

<!--
to have voodoo build the pack and automatically copy it into a multimc instance
set the following as pre-launch command

`java -jar /path/to/builder.jar pack_definition.yaml -d /path/to/pack/dev -i $INST_DIR/.. --mmc`

example

`java -jar $HOME/dev/voodoo/builder/build/libs/builder-1.0.jar test.yaml -d $HOME/dev/voodoo/builder/run/ -i $INST_DIR/.. --mmc`

-->