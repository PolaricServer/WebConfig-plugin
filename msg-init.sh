#!/bin/bash 

if [ -z "$1" ]; then
  echo "Please specify which language_country suffix(es) you want to translate to"
  exit 1
fi

# Java/Scala translations
for lang in $*
do
  echo "Initializing template file for $lang (Java/Scala code)"
  if [ -f src/i18n/msgs/$lang.po ]; then
    echo "src/i18n/msgs/$lang.po exists! You probably wanted merge"
    break
  fi
  msginit --no-translator -l $lang --no-wrap -o src/i18n/msgs/${lang}.po -i src/i18n/msgs/messages.pot
done


# Javascript translations
for lang in $*
do
  echo "Initializing template file for $lang (Javascript code)"
  if [ -f www/i18n/msgs/$lang.po ]; then
    echo "www/i18n/msgs/$lang.po exists! You probably wanted merge"
    exit
  fi
  msginit --no-translator -l $lang --no-wrap -o www/i18n/msgs/${lang}.po -i www/i18n/msgs/messages.pot
done