#!/bin/bash

if [ "$#" -ne 4 ]; then
    echo "Wrong number of parameters. Expected: NAME EMBEDDINGS_FOLDER TRAINING_DATA DEV_DATA "
    exit 1;
fi

if [ -z "$TORCH" ]; then
    echo "Must set path for \$TORCH"
    exit 1;
fi


NAME=$1
EMBEDDINGS=$2
TRAINING_DATA=$3
DEV_DATA=$4
PARSING_MODEL=$EMBEDDINGS/model.$NAME
TRAINING_FOLDER=$EMBEDDINGS/train.$NAME

if [[ ! -f $TRAINING_DATA/gold.stagged ]]; then
    echo "Missing training data: $TRAINING_DATA/gold.stagged"
    exit 1;
fi

if [[ ! -f $DEV_DATA/gold.stagged ]]; then
    echo "Missing development data: $DEV_DATA/gold.stagged"
    exit 1;
fi

if [[ ! -f $TRAINING_DATA/seenRules ]]; then
    echo "Missing file: $TRAINING_DATA/seenRules"
    echo "This file should contain all pairs of categories that combine in the labelled data. If not available, add a \"-s\" flag to the parser command in do_experiments.sh"
    exit 1;
fi

# Do training. The supertagging model is saved in: $EMBEDDINGS/train.$NAME
./train.sh $EMBEDDINGS 0 3 3 $TRAINING_DATA/gold.stagged $DEV_DATA/gold.stagged $NAME

# Convert the output to a parser model, saved in: $EMBEDDINGS/model.$NAME
mkdir $PARSING_MODEL
cp unaryRules $PARSING_MODEL
cp $TRAINING_DATA/seenRules $PARSING_MODEL
cp $TRAINING_FOLDER/categories $PARSING_MODEL
cp $TRAINING_FOLDER/suffixes $PARSING_MODEL
$TORCH dump_model.lua $EMBEDDINGS $TRAINING_FOLDER $PARSING_MODEL

echo "Model created in: $PARSING_MODEL"

if [ -z "$CANDC" ]; then
    echo "Need to set CANDC variable to point at the C&C parser folder"
    exit 1
fi  

if [[ ! -f $DEV_DATA/gold.stagged ]]; then
    echo "Missing gold-standard dependencies file: $DEV_DATA/gold.deps"
    exit 1;
else
    # Evaluate the parser, saving the results in: $DEV_DATA/$NAME.eval
    echo "Accuracy on $DEV_DATA :"
    ./do_experiments.sh $DEV_DATA $PARSING_MODEL $NAME
    echo "Detailed results in: $DEV_DATA/$NAME.eval"
fi



