EVAL_FOLDER=$1
MODEL=$2
NAME=$3

POSFILE=$EVAL_FOLDER/candc.pos
cat $POSFILE | ../embeddings_supertagger/candc.rebuilt/bin/msuper --model ../embeddings_supertagger/candc.rebuilt/working/super -ofmt "%w\t%p\t%S|\n" --beta 0.00001 > $EVAL_FOLDER/candc.mtagged

java -jar easyccg.jar -m $MODEL -f $EVAL_FOLDER/candc.mtagged -l 200 -i supertagged --supertaggerbeam 0.00001 > $EVAL_FOLDER/$NAME.auto
$CANDC/src/scripts/ccg/./get_deps_from_auto $EVAL_FOLDER/$NAME.auto $EVAL_FOLDER/$NAME.deps 
sed -i -e 's/^$/<c>\n/' $EVAL_FOLDER/$NAME.deps
$CANDC/src/scripts/ccg/./evaluate2 -r $EVAL_FOLDER/gold.stagged $EVAL_FOLDER/gold.deps $EVAL_FOLDER/$NAME.deps > $EVAL_FOLDER/$NAME.eval
grep lf $EVAL_FOLDER/$NAME.eval

