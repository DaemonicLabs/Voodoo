##!/usr/bin/env bash
#
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
#PWD=$(pwd)
#
#cd $DIR
#
#$DIR/gradlew clean publishToMavenLocal :poet
#if [ ! $? -eq 0 ]; then
#    echo "Error compiling voodoo"
#    exit 1
#fi
#
#pack=$1
#mkdir -p "$DIR/profile"
#PROFILE="$DIR/profile/$pack.profile.txt"
#rm $PROFILE
#
#[ ! -e samples ] && mkdir samples
#cd samples
#
#[ ! -e run ] && mkdir run
#cd run
#
#kscript --package "$DIR/samples/$pack.kt"
#
#[ ! -e "$pack" ] && mkdir "$pack"
#cd "$pack"
#
#find . -name \*.entry.hjson -type f -delete
#find . -name \*.lock.hjson -type f -delete
#
## TIMEFORMAT="%E"
#
#echo
#echo "building $1"
#echo
#
#exec 3>&1 4>&2
#build_time=$({ time java -jar "$pack-capsule.jar" build $2 1>&3 2>&4; } 2>&1)
#status=$?
#exec 3>&- 4>&-
#if [ ! $status -eq 0 ]; then
#    echo "Error Building $pack"
#    exit 1
#fi
#
#echo
#echo "packaging $1"
#echo
#
#
#exec 3>&1 4>&2
#pack_time=$({ time java -jar "$pack-capsule.jar" pack sk 1>&3 2>&4; } 2>&1)
#status=$?
#exec 3>&- 4>&-
#if [ ! $status -eq 0 ]; then
#    echo "Error Packing $pack"
#    exit 1
#fi
#
#cd $DIR
#
#cat << EOL > "$PROFILE"
#build time: $build_time
#pack time: $pack_time
#EOL
#
#cat $PROFILE