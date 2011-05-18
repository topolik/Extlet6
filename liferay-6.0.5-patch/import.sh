#!/bin/bash
LIFERAY_605_SRC_ZIP=/tmp/liferay-portal-src-6.0.5.zip
LIFERAY_TRUNK_DIR=/opt/liferay.git

# pre-create directories
for d in `find ../LPS-14221_plus_LPS-9442_trunk_rev_80606/ -type d | sed 's/.*80606\///'`
do 
    mkdir -p $d
done

# extract sources for 6.0.5
for f in `find ../LPS-14221_plus_LPS-9442_trunk_rev_80606/ -type f | sed 's/.*80606\///'`
do
    unzip -o $LIFERAY_605_SRC_ZIP liferay-portal-src-6.0.5/$f
    cp liferay-portal-src-6.0.5/$f $f
done

# manually diff changes and merge
for f in `find liferay-portal-src-6.0.5 -type f | sed 's/.*6.0.5\///'`
do
    meld ../LPS-14221_plus_LPS-9442_trunk_rev_80606/$f $LIFERAY_TRUNK_DIR/$f &
    gedit $f
done

# remove unzipped sources
rm -r liferay-portal-src-6.0.5

