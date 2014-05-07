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
$CANDC/src/scripts/ccg/./get_grs_from_auto $EVAL_FOLDER/$NAME.auto experiments/genia/markedup_sd-1.00 $EVAL_FOLDER/$NAME.grs
python $EVAL_FOLDER/grs2sd-1.00 --ccgbank $EVAL_FOLDER/$NAME.grs> $EVAL_FOLDER/$NAME.depbank 
cat $EVAL_FOLDER/$NAME.depbank | grep -v "delme" > $EVAL_FOLDER/$NAME.depbank2
mv $EVAL_FOLDER/$NAME.depbank2 $EVAL_FOLDER/$NAME.depbank
python2.7 $EVAL_FOLDER/eval_test.py $EVAL_FOLDER/gold.raw $EVAL_FOLDER/gold.deps $EVAL_FOLDER/$NAME.depbank > $EVAL_FOLDER/$NAME.eval
