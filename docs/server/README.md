[index](../../)

# Deploying a Server

## Requirements

assuming you have a `.kt` file and `.lock.hjson` ready to use from
[Building](../building)

## Packing the Server

Voodoo packages all data needed to install a server into a folder

```bash
./gradlew awesomepack --args "pack server"
```

You will find a `.server/awesomepack/` folder has been created

To continue you must upload that folder to the intended server host
and navigate into the uploaded folder

example:
```bash
rsync -avh --delete .server/awesomepack/ user@server.hst.com:~/server/upload/awesomepack
ssh server.host.com
cd ~/server/upload/awesomepack
```

stop the minecraft server (if it is running)
and execute the server installer with the target run directory

```bash
cd ~/server/upload/awesomepack
java -jar server-installer.jar ~/server/run/awesomepack
```

the forge installer has been executed and the executable jar has been copied to `forge.jar`
now the minecraft server can be safely restarted

```bash
java -jar forge.jar
```