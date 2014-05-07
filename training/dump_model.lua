require 'torch'
require 'nn'
require 'features'

io.stdout:setvbuf("no") 

embeddingsFolder=arg[1];
--name=arg[2]
output=arg[3];
modelFolder=arg[2] --embeddingsFolder .. '/' .. name

mlp = torch.load(modelFolder .. '/bestModel')
features = nn.Features(embeddingsFolder .. '/embeddings.words', modelFolder .. '/suffixes', modelFolder .. '/categories')
wordTable = mlp:get(1):get(1).weight

suffixTable = mlp:get(1):get(2).weight
capsTable = mlp:get(1):get(3).weight
-- CLASSIFIER
--local file = io.open(output . "classifier", "w")
io.output(output .. "/classifier")

  linear = mlp:get(4)
  outputDimension = linear.weight:size()[1]
  featureDimension = linear.weight:size()[2]
  for i = 1,outputDimension do
    for j=1,featureDimension do
      if (j > 1) then
        io.write(' ')
      end

      io.write(linear.weight[i][j])
    end
    io.write('\n')
  end
io.flush()

io.output(output .. "/bias")
  for i = 1,outputDimension do
    io.write(linear.bias[i] .. '\n')
  end
io.flush()

-- SUFFIX 
io.output(output .. "/suffix")
io.write("*suffix_pad*")
  embeddings = suffixTable[features.paddingIndex]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*unknown_suffix*")
  embeddings = suffixTable[features.unknownSuffixIndex]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

for word,index in pairs(features.suffixToIndex) do 
  io.write(word)
  embeddings = suffixTable[index]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')
end

-- CAPS 
io.output(output .. "/capitals")
io.write("*caps_pad*")
  embeddings = capsTable[features.paddingIndex]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*lower_case*")
  embeddings = capsTable[2]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*upper_case*")
  embeddings = capsTable[3]
  for i = 1,embeddings:size()[1] do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

-- EMBEDDINGS
io.output(output .. "/embeddings")
io.write("*unknown_lower*")
  embeddings = wordTable[features.unknownLowerIndex]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*unknown_upper*")
  embeddings = wordTable[features.unknownUpperIndex]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')


io.write("*unknown_special*")
  embeddings = wordTable[features.specialCharIndex]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*left_pad*")
  embeddings = wordTable[features.leftPaddingIndex]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

io.write("*right_pad*")
  embeddings = wordTable[features.rightPaddingIndex]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')

for word,index in pairs(features.wordToIndex) do 
  io.write(word)
  embeddings = wordTable[index]
  for i = 1,50 do
    io.write(' ' .. embeddings[i])
  end
  io.write('\n')
end

io.flush()

