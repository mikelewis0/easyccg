WORKING_FOLDER=$1
MODEL=$2
NAME=$3

CANDC=candc.distrib/
CANDC_BIN=$CANDC/bin
CANDC_MODELS=candc.distrib/models/

POSFILE=$WORKING_FOLDER/candc.pos
cat $POSFILE | java -cp "$EASYCCG/bin:$EASYCCG/lib/*" uk.ac.ed.easyccg.syntax.Tagger -m $MODEL -l 250 --supertaggerbeam 0.001 > $WORKING_FOLDER/$NAME.mtagged
cat $WORKING_FOLDER/$NAME.mtagged | $CANDC_BIN/parser --model $CANDC_MODELS/parser --super $CANDC_MODELS/super --ext_super --ifmt "%w\t%p\t%S|\n" --parser-markedup $WORKING_FOLDER/markedup_sd-1.00 --parser-maxsupercats 1000000 --printer grs --parser-beam_ratio 0.0001 --force_words false > $WORKING_FOLDER/$NAME.parsed
python $WORKING_FOLDER/grs2sd-1.00 --ccgbank $WORKING_FOLDER/$NAME.parsed > $WORKING_FOLDER/$NAME.depbank 
python2.7 $WORKING_FOLDER/eval_test.py $WORKING_FOLDER/gold.raw $WORKING_FOLDER/gold.deps $WORKING_FOLDER/$NAME.depbank > $WORKING_FOLDER/$NAME.eval

