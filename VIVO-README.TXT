1. git svn remote (add to .git/config):

[svn-remote "svn"]
        url = http://orient.googlecode.com/svn
        fetch = trunk:refs/remotes/trunk
        branches = branches/*:refs/remotes/*
        tags = tags/*:refs/remotes/tags/*

2. Replace @BUILD@:

for x in $(grep -l -R "@BUILD@" *); do sed -i -e 's/@BUILD@/5146/g' $x; done;

3. Replace @VERSION@:

for x in $(grep -l -R "@VERSION@" *); do sed -i -e 's/@VERSION@/1.0.vivo_1/g' $x; done;

4. Change project version:

for x in $(grep -l -R 1.0-SNAPSHOT *); do sed -i -e 's/1.0-SNAPSHOT/1.0.vivo_1/g' $x; done;
