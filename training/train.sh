# embeddings hiddenLayerSize contextWindowBackward contextWindowForward trainingData devData name

FOLDER=$1/train.$7
mkdir $FOLDER

# Finds categories occuring at least 10 times
cat $5 | grep -v "^#.*" | grep -v "^$" | tr -s " " "\n" | cut -d "|" -f 3 | sort | uniq -c | grep "[1-9][0-9]" | grep -o "[^ ]*$" > $FOLDER/categories 
# Find all 2-character suffixes
cat $5 | grep -v "^#.*" | grep -v "^$" | tr -s " " "\n" | cut -d "|" -f 1 | awk '{print "_"tolower($1)}' | grep -o ..$ | sort | uniq > $FOLDER/suffixes

# Train the model
$TORCH do_training.lua $5 $6 $1 $2 $3 $4 $7
