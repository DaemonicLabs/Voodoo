# Refactor

requirements:

- jobs have
  - `ready` condition that usually tests if a entry has all the required fields set
    could also make sure a job ran and assume the field is set ? likely going to lead to errors
  - `label` for referencing
  - `requirements` jobs that have to be run on the whole modpack before (multiple stages of execution)

providers (curse, jenkin, etc) iterate through the entries in the modpack and
- test if entry is marked as processed
  - if true: continue
- test if entry meets requirements
  if false: continue
- test if all requirements are marked as processed
  if false: continue


jobs are accumulated
jobs contain the name of Entry and ready-condition ad well as execution instructions

special names can reference multiple entry ? maybe `@all`, `@curse`, etc

# TODO

* diff 2 fully resolved modpacks

## Builder

* re-add a validate step

* fix cursedata parsing slowdown

* clean up unneeded duplication between features classes

**TEST** manual dependencies with optionals


https://media.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245%2B69d0d70.jar
https://files.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar

https://media.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245%252B69d0d70.jar
https://media.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245%2B69d0d70.jar

https://media.forgecdn.net/files/2456/43/BiblioCraft%5bv2.4.3%5d%5bMC1.12.0%5d.jar
https://media.forgecdn.net/files/2456/43/BiblioCraft%255bv2.4.3%255d%255bMC1.12.0%255d.jar

[main] INFO - downloading Better Builder's Wands to C:\Users\nikky\AppData\Roaming\voodoo\Cache\CURSE\238403\2443194\BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar
[main] INFO - downloading https://files.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar
[main] INFO - following to https://media.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245%2B69d0d70.jar
[main] ERROR - invalid statusCode 403 from https://files.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar
[main] ERROR - connection url: https://media.forgecdn.net/files/2443/194/BetterBuildersWands-1.12-0.11.1.245%252B69d0d70.jar
[main] ERROR - content: <?xml version="1.0" encoding="UTF-8"?>
<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>E5E8BC3E69BEE079</RequestId><HostId>zXib0/ItivuBIEw87tCNyXZb3SXLlAfblq+29wqXjwmv+ytbfkD+t4Iyf9prVK2qHXlFqM4ShjA=</HostId></Error>
[main] INFO - downloading BiblioCraft to C:\Users\nikky\AppData\Roaming\voodoo\Cache\CURSE\228027\2456043\BiblioCraft[v2.4.3][MC1.12.0].jar
[main] INFO - downloading https://files.forgecdn.net/files/2456/43/BiblioCraft[v2.4.3][MC1.12.0].jar
[main] INFO - following to https://media.forgecdn.net/files/2456/43/BiblioCraft%5bv2.4.3%5d%5bMC1.12.0%5d.jar
[main] ERROR - invalid statusCode 403 from https://files.forgecdn.net/files/2456/43/BiblioCraft[v2.4.3][MC1.12.0].jar
[main] ERROR - connection url: https://media.forgecdn.net/files/2456/43/BiblioCraft%255bv2.4.3%255d%255bMC1.12.0%255d.jar
[main] ERROR - content: <?xml version="1.0" encoding="UTF-8"?>
<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>021351DEE938761C</RequestId><HostId>sogNGbNqIfhZppLlMNpJeLJ9YbIq5h5p4+ECNqC7x+ZA4ZzO4b8skWIZT8NrGP8mHIhQukyvqOU=</HostId></Error>
[main] INFO - downloading The Disenchanter Mod to C:\Users\nikky\AppData\Roaming\voodoo\Cache\CURSE\245769\2490587\disenchanter-[1.12]1.5.jar
[main] INFO - downloading https://files.forgecdn.net/files/2490/587/disenchanter-[1.12]1.5.jar
[main] INFO - following to https://media.forgecdn.net/files/2490/587/disenchanter-%5b1.12%5d1.5.jar
[main] ERROR - invalid statusCode 403 from https://files.forgecdn.net/files/2490/587/disenchanter-[1.12]1.5.jar
[main] ERROR - connection url: https://media.forgecdn.net/files/2490/587/disenchanter-%255b1.12%255d1.5.jar
[main] ERROR - content: <?xml version="1.0" encoding="UTF-8"?>
<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>1952ADEE9B40257C</RequestId><HostId>nnt68Pf758TWPQDFcvxqKRzpda14h31XoLW0rRpa2yBt6DLhRvCtRSBmU2jvKlxDYu+62oh859E=</HostId></Error>
[main] INFO - downloading Pam's HarvestCraft to C:\Users\nikky\AppData\Roaming\voodoo\Cache\CURSE\221857\2527142\Pam's HarvestCraft 1.12.2o.jar
[main] INFO - downloading https://files.forgecdn.net/files/2527/142/Pam's HarvestCraft 1.12.2o.jar
[main] INFO - following to https://media.forgecdn.net/files/2527/142/Pam%27s+HarvestCraft+1.12.2o.jar
[main] ERROR - invalid statusCode 403 from https://files.forgecdn.net/files/2527/142/Pam's HarvestCraft 1.12.2o.jar
[main] ERROR - connection url: https://media.forgecdn.net/files/2527/142/Pam%2527s+HarvestCraft+1.12.2o.jar
[main] ERROR - content: <?xml version="1.0" encoding="UTF-8"?>
<Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>503E55632D9FDD52</RequestId><HostId>BkU/9YSRkAdl5+paLEiKK5S/Fos7Rks3v8q16m7cuEr6z0Cdh3WY1kTFaOGKC3uKVRuEurU4BbM=</HostId></Error>