1. git svn remote (add to .git/config):

[svn-remote "svn"]
        url = http://orient.googlecode.com/svn
        fetch = trunk:refs/remotes/trunk
        branches = branches/*:refs/remotes/*
        tags = tags/*:refs/remotes/tags/*

2. Replace 5371:

for x in $(grep -l -R "5371" *); do sed -i -e 's/5371/5371/g' $x; done;

3. Replace 1.0.vivo_2:

for x in $(grep -l -R "1.0.vivo_2" *); do sed -i -e 's/1.0.vivo_2/1.0.vivo_2/g' $x; done;

4. Change project version:

for x in $(grep -l -R 1.0-SNAPSHOT *); do sed -i -e 's/1.0-SNAPSHOT/1.0.vivo_2/g' $x; done;
