#!/bin/bash
LIFERAY_605_SRC_ZIP=/data/linux/tmp/liferay-portal-src-6.0.5.zip
LIFERAY_606_SRC_ZIP=/data/linux/tmp/liferay-portal-src-6.0.6-20110225.zip
LIFERAY_TRUNK_DIR=/opt/liferay.git

# pre-create directories
for d in `find ../LPS-14221_plus_LPS-9442_trunk_rev_80606/ -type d | sed 's/.*80606\///'`
do 
    mkdir -p $d
    mkdir -p liferay-trunk/$d
done

# extract sources for 6.0.5 and 6.0.6
for f in `find ../LPS-14221_plus_LPS-9442_trunk_rev_80606/ -type f | sed 's/.*80606\///'`
do
    unzip -o $LIFERAY_605_SRC_ZIP liferay-portal-src-6.0.5/$f
    unzip -o $LIFERAY_606_SRC_ZIP liferay-portal-src-6.0.6/$f
    cp $LIFERAY_TRUNK_DIR/$f liferay-trunk/$f

    if [ -e liferay-portal-src-6.0.5/$f ]; then
        sed -i 's/Copyright (c) 2000-[0-9]\+ //' liferay-portal-src-6.0.5/$f
    fi
    if [ -e liferay-portal-src-6.0.6/$f ]; then
        sed -i 's/Copyright (c) 2000-[0-9]\+ //' liferay-portal-src-6.0.6/$f
    fi
    if [ -e liferay-trunk/$f ]; then
        sed -i 's/Copyright (c) 2000-[0-9]\+ //' liferay-trunk/$f
    fi
done

# manually diff changes and merge
for f in `find liferay-portal-src-6.0.6 -type f | sed 's/.*6.0.6\///'`
do

    diff liferay-portal-src-6.0.5/$f liferay-portal-src-6.0.6/$f > /dev/null
    if [ $? -eq 0 ]
    then
        # there are no changes between 6.0.5 and 6.0.6 -> we can use 6.0.5 patch
        cp ../liferay-6.0.5-patch/$f $f
    else
        diff liferay-trunk/$f liferay-portal-src-6.0.6/$f > /dev/null
        if [ $? -eq 0 ]
        then
            # there are no changes between 6.0.6 and trunk -> we can use trunk patch
            cp ../LPS-14221_plus_LPS-9442_trunk_rev_80606/$f $f
        else 
            # we need to manually merge
            cp liferay-portal-src-6.0.6/$f $f
            gedit $f &
            meld ../LPS-14221_plus_LPS-9442_trunk_rev_80606/$f $LIFERAY_TRUNK_DIR/$f
        fi
    fi
done

# remove unzipped sources
rm -r liferay-portal-src-6.0.5
rm -r liferay-portal-src-6.0.6
rm -r liferay-trunk

