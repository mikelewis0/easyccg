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

features = nn.Features(embeddingsFolder .. '/embeddings.words', nil, modelFolder .. '/suffixes', modelFolder .. '/postags', 
                       modelFolder .. '/categories', modelFolder .. '/frequentwords')

train = nn.Train()

-- TODO: tidy this up.
-- devFile appears twice, to allow seprate validation files with gold/auto POS tags. 
-- It turns out that POS features don't help, so we'll default to just using the dataset with gold POS tags. 
train:trainModel(trainFile, devFile, devFile, embeddingsFolder, features, windowBackward, windowForward, hidden, name)
