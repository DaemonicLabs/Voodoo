
http://45.79.138.67/launcher/packages.json

```
{
  "minimumVersion": 1,
  "packages": [
    {
      "title": "Center of the Multiverse",
      "name": "Center of the Multiverse",
      "version": "CotM - 2.1.1",
      "location": "center-of-the-multiverse.json",
      "priority": 0
    }
  ]
}
```

http://45.79.138.67/launcher/center-of-the-multiverse.json
https://testificate.xen.prgmr.com/craft/repo/openpack.json

echo $INST_NAME $INST_ID $INST_DIR $INST_MC_DIR $INST_JAVA $INST_JAVA_ARGS
Test Test /home/nikky/.local/share/multimc/instances/Test /home/nikky/.local/share/multimc/instances/Test/.minecraft java java_ARGS
$INST_NAME Test
$INST_ID Test
$INST_DIR /home/nikky/.local/share/multimc/instances/Test
$INST_MC_DIR /home/nikky/.local/share/multimc/instances/Test/.minecraft
$INST_JAVA java

```
$INST_JAVA -jar $INST_DIR/installer.jar \
    https://testificate.xen.prgmr.com/craft/repo/openpack.json \
    --id $INST_ID \
    --inst $INST_DIR \
    --mc $INST_MC_DIR
```
