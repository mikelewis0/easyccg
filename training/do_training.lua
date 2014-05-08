require 'torch'
require 'nn'
require 'features'
require 'SGD'
require 'train'

io.stdout:setvbuf("no") 

trainFile=arg[1];
devFile=arg[2];
embeddingsFolder=arg[3];
hidden=tonumber(arg[4]);
windowBackward=tonumber(arg[5]);
windowForward=tonumber(arg[6]);
name=arg[7]

modelFolder=embeddingsFolder .. '/train.' .. name

features = nn.Features(embeddingsFolder .. '/embeddings.words', modelFolder .. '/suffixes', modelFolder .. '/categories')

train = nn.Train()
train:trainModel(trainFile, devFile, embeddingsFolder, features, windowBackward, windowForward, hidden, name)
