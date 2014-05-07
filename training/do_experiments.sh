EVAL_FOLDER=$1
MODEL=$2
NAME=$3

if [ "$#" -ne 3 ]; then
    echo "Wrong number of parameters. Expected: EVAL_FOLDER MODEL_FOLDER MODEL_NAME "
    exit 1;
fi

if [ -z "$CANDC" ]; then
    echo "Need to set CANDC variable to point at the C&C parser folder"
    exit 1
fi  

java -jar easyccg.jar -m $MODEL -f $EVAL_FOLDER/gold.raw > $EVAL_FOLDER/$NAME.auto -l 200 -i tokenized
$CANDC/src/scripts/ccg/./get_deps_from_auto $EVAL_FOLDER/$NAME.auto $EVAL_FOLDER/$NAME.deps 
sed -i -e 's/^$/<c>\n/' $EVAL_FOLDER/$NAME.deps
$CANDC/src/scripts/ccg/./evaluate2 $EVAL_FOLDER/gold.stagged $EVAL_FOLDER/gold.deps $EVAL_FOLDER/$NAME.deps > $EVAL_FOLDER/$NAME.eval
grep lf $EVAL_FOLDER/$NAME.eval
