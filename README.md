# voodoo

voodoo is a set of tools to quickly build and prepare modpacks for SKCraft launcher

at the moment only the commandline app is working and being worked on

## build

unix: `./gradlew :builder:build`
windows: `./gradlew.bat :builder:build`

## usage

`java -jar /path/to/builder.jar pack_definition.yaml`

## MultiMC integration

to have voodoo build the pack and automatically copy it into a multimc instance
set the following as pre-launch command

`java -jar /path/to/builder.jar pack_definition.yaml -d /path/to/pack/dev -i $INST_DIR/.. --mmc`

example

`java -jar $HOME/dev/voodoo/builder/build/libs/builder-1.0-SNAPSHOT-all.jar test.yaml -d $HOME/dev/voodoo/builder/run/ -i $INST_DIR/.. --mmc`