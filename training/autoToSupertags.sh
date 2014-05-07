cat $1 | grep -o "\(<L[^>]*\)\|\(^ID=.*\)" | grep -o "\([^ ]* [^ ]*$\)\|\(^ID=.*\)" |  sed 's/ /||/g' | awk '/ID/ {printf "\n%s\n",$0;next} {printf "%s ",$0}' | grep -v "^$"
