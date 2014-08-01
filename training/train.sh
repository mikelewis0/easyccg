# embeddings hiddenLayerSize contextWindowBackward contextWindowForward trainingData devData name

FOLDER=$1/train.$7
TRAINING_DATA=$5
DEV_DATA=$6
mkdir $FOLDER
TRAINING_FILE=$TRAINING_DATA/gold.stagged

if [[ -f $TRAINING_DATA/categories ]]; then
	# Use supplied list of categories
	cp $TRAINING_DATA/categories $FOLDER/categories
else
	# Finds categories occuring at least 10 times
	cat $TRAINING_FILE | grep -v "^#.*" | grep -v "^$" | tr -s " " "\n" | cut -d "|" -f 3 | sort | uniq -c | grep "[1-9][0-9]" | grep -o "[^ ]*$" > $FOLDER/categories
fi

# Find all 2-character suffixes
cat $TRAINING_FILE | grep -v "^#.*" | grep -v "^$" | tr -s " " "\n" | cut -d "|" -f 1 | awk '{print "_"tolower($1)}' | grep -o ..$ | sort | uniq > $FOLDER/suffixes

# Train the model
$TORCH do_training.lua $TRAINING_DATA/gold.stagged $DEV_DATA/candc.pos $DEV_DATA/gold.stagged $1 $2 $3 $4 $7
