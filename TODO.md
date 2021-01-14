## cli commands

- generateSchema (also generates autocompletions)
- evalScript (kts -> json)
- build
- changelog (TODO: figure out how to store diffs, etc)
- inspect (visualize versions and dependencies)

## another rewrite

- use ONE version (0.6.0-SNAPSHOT)
- publish to ~~bintray~~ own maven
- use github actions
- add github action TESTS with java version matrix
- add script-less entry point
- add json-schema

## version listing

- [ ] add Tabs or dropdown in MULTIMC installer
- [x] change uploaded (voodoo) format

### reasons for all versions in source

old pack versions will be automagically rebuilt if the format breaks

diffs / changelogs / modlists can be generated

users cna pick from version list

### reasons against

versions will be reset? how is that any useful ?


stable format needs to contain all the lock-file and lock-entry information


`build` command: creates `$version.lock.pack.json` including lock-entries
`package` picks all `.lock.pack.json` files, sorts by semver

add `packageThis=false` to versions that should be used for changelog purposes but not be published
configure between `rollingRelease`/`autoUpdate` and ability to choose version in modpack.meta.json


voodoo-format needs to handle version listing

marker file in multimc instance to choose version (picked once by user)

popup allows user to pick version


TODO: add changelogs/docs to the format manifest and upload
