# voodoo

voodoo is a set of tools to quickly build and prepare modpacks for SKCraft launcher

at the moment only the commandline app is working and being worked on

recently a bootstrap application was added, but it is slightly smaller and just downloads and calls the voodoo binary (from the buildserver4

## get it

### download

please grab the latest binaries from the [buildserver](https://ci.elytradev.com/job/elytra/job/Voodoo/job/2.0/)

### build

unix: `./gradlew :builder:build`
windows: `./gradlew.bat :builder:build`

## usage


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