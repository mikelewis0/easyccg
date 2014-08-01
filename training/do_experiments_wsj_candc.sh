WORKING_FOLDER=$1
MODEL=$2
NAME=$3

CANDC=../embeddings_supertagger/candc.rebuilt/
CANDC_BIN=$CANDC/bin
CANDC_MODELS=../embeddings_supertagger/candc.rebuilt/working/

POSFILE=$WORKING_FOLDER/candc.pos
cat $POSFILE | java -cp "$EASYCCG/bin:$EASYCCG/lib/*" uk.ac.ed.easyccg.syntax.Tagger -m $MODEL -l 250 > $WORKING_FOLDER/$NAME.mtagged
cat $WORKING_FOLDER/$NAME.mtagged | $CANDC_BIN/parser --model $CANDC_MODELS/model_hybrid --super $CANDC_MODELS/super --ext_super --ifmt "%w\t%p\t%S|\n" --printer deps --parser-maxsupercats 1000000 --decoder deps --force_words false --super-forward_beam_ratio 0.1 > $WORKING_FOLDER/$NAME.parsed
$CANDC/src/scripts/ccg/./evaluate $WORKING_FOLDER/gold.stagged $WORKING_FOLDER/gold.deps $WORKING_FOLDER/$NAME.parsed > $WORKING_FOLDER/$NAME.parsed.eval
grep lf $WORKING_FOLDER/$NAME.parsed.eval
