#!/bin/bash -x

wget -O def_original.html http://www.rossvyaz.ru/docs/articles/DEF-9x.html
iconv --f windows-1251 --t utf-8 def_original.html | grep '^<tr>' > def.html

sed -r 's/>\s*</></g' def.html | sed -r 's/<tr><td>//' | sed -r 's/<\/td><\/tr>//' | sed -r 's/^\s*//' | sed -r 's/\s*<\/td><td>\s*/;/g'> def2.html

awk -F';' '{print "7" $1 $2 ";7" $1 $3 ";" $5 ";" $6 }' def2.html > def3.csv

rm def_original.html def.html def2.html
