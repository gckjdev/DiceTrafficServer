#!/bin/bash

line=' ========= '
echo ""
echo $line' add files (*.java *.js) '$line
git add *.java commit.sh
echo ''

echo $line' oh, commit files. comment: '"$1"" "$line
git commit -m "$1"
echo ''

echo $line' hey, pull code from server '$line
git pull
echo ''

echo $line' wow, push code to server '$line
git push

echo ''
echo $line' congratulations! hope there is no conflict! '$line
echo ''
