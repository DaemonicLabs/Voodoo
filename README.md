# voodoo

voodoo is a set of tools to quickly build and prepare modpacks for SKCraft launcher

at the moment only the commandline app is working and being worked on

## download

please grab the latest binaries from the [buildserver](https://ci.elytradev.com/job/elytra/job/voodoo/job/master/)

## build

unix: `./gradlew :builder:build`
windows: `./gradlew.bat :builder:build`

## usage

`java -jar /path/to/builder.jar pack_definition.yaml`

samples: [sample](/samples)

## config file

every value in the configuration file can be overridden with commandline options

```yaml
workingDirectory: '.'
output: modpacks
instances: instances
instance: = null
```

the config file is expected to be either `config.yaml` or to be set with the `--config` commandline option

## MultiMC integration

to have voodoo build the pack and automatically copy it into a multimc instance
set the following as pre-launch command

`java -jar /path/to/builder.jar pack_definition.yaml -d /path/to/pack/dev -i $INST_DIR/.. --mmc`

example

`java -jar $HOME/dev/voodoo/builder/build/libs/builder-1.0.jar test.yaml -d $HOME/dev/voodoo/builder/run/ -i $INST_DIR/.. --mmc`