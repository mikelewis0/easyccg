require 'torch'
require 'nn'
require 'features'
require 'SGD'
require 'train'

io.stdout:setvbuf("no") 

trainFile=arg[1];
devAutoFile=arg[2];
devGoldFile=arg[3];
embeddingsFolder=arg[4];
hidden=tonumber(arg[5]);
windowBackward=tonumber(arg[6]);
windowForward=tonumber(arg[7]);
name=arg[8]
numberOfSpecificWordFeatures=250 --arg[8]

modelFolder=embeddingsFolder .. '/train.' .. name

features = nn.Features(embeddingsFolder .. '/embeddings.words', nil, modelFolder .. '/suffixes', modelFolder .. '/postags', 
                       modelFolder .. '/categories', modelFolder .. '/frequentwords')


train = nn.Train()
train:trainModel(trainFile, devAutoFile, devGoldFile, embeddingsFolder, features, windowBackward, windowForward, hidden, name)
