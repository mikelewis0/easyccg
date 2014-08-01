require 'torch'
require 'nn'
require 'string'

local Features = torch.class('nn.Features')

function Features:normalize(word) 
   
   special = self.specialWords[word]
   if special ~= nil then
     -- Normalize brackets
     return special
   end 
   
   local lemma = self.lemmas[word]
   if lemma then
     word = lemma
   end

   word = string.gsub(word, "[0-9]", "#")
   word = string.gsub(word, "\\/", "/")
   word = string.lower(word)

  return word
end


function Features:fileToTable(path, minIndex, normalize, errorIfFileNotFound)
   local file = io.open(path)

   if file==nil then 
     if errorIfFileNotFound then
       error("Unable to load file: " .. path)
     else
       return 0, {}, {}
     end
   end


   local result = {}
   local reverse = {}
   local count = minIndex
   for line in file:lines() do 
      if (normalize) then
        line = self:normalize(line)
      end
      if (result[line] == nil) then
        result[line] = count
        reverse[count] = line
      end
      count = count + 1
   end

   return count - 1, result, reverse
end

function Features:__init(wordFilePath, lemmasFilePath, suffixFilePath, posFilePath, categoryFilePath, frequentWordFile)

   self.leftPaddingIndex = 1
   self.rightPaddingIndex = 2
   self.specialCharIndex = 3
   self.unknownUpperIndex = 4
   self.unknownLowerIndex = 5

   self.paddingIndex = 1
   self.unknownSuffixIndex = 2

   self.specialWords = {}
   self.specialWords["-LRB-"] = "("
   self.specialWords["-RRB-"] = ")"
   self.specialWords["-LCB-"] = "("
   self.specialWords["-RCB-"] = ")"

   self.numberOfFeatures = 3

   self.lemmas = {}
   if lemmasFilePath then
     local lemmasFile = io.open(lemmasFilePath)
     if lemmasFile then     
       for line in lemmasFile:lines() do
           local delim = string.find(line, ' ', 1, true)
           local lemma = string.sub(line, delim + 1, string.len(line))
           local word = string.sub(line, 1, delim - 1)
           self.lemmas[word] = lemma
       end
     end
   end

   self.numPOS, self.posToIndex, _ = self:fileToTable(posFilePath, 1, false, false)  

   self.numWords, self.wordToIndex, self.indexToWord = self:fileToTable(wordFilePath, self.unknownLowerIndex, true, true)
   self.numSuffixes, self.suffixToIndex, _ = self:fileToTable(suffixFilePath, self.unknownSuffixIndex + 1, false, true)
   self.numCats, self.categoryToIndex, self.indexToCategory = self:fileToTable(categoryFilePath, 1, false, true)

   self.numberOfSpecificWordFeatures, self.frequentWordIndex, _ = self:fileToTable(frequentWordFile, 1, false, false)
   self.extraWords = 4
end

function Features:getNumberOfSparseFeatures() 
  return self.numberOfSpecificWordFeatures + self.numPOS
end

function Features:getFeatures(words, posTags, index, windowBackward, windowForward) 
  window = (windowBackward + windowForward + 1)
  inputLine = torch.Tensor(window * (self.numberOfFeatures + self:getNumberOfSparseFeatures()))
  inputLine:fill(0)
  j = 1
  for i = index - windowBackward, index + windowForward do
    if i < 1 then
      inputLine[j] = self.leftPaddingIndex
      inputLine[window + j] = 1
      inputLine[2 * window + j] = 1
    elseif words[i]==nil then
      inputLine[j] = self.rightPaddingIndex
      inputLine[window + j] = 1
      inputLine[2 * window + j] = 1
    else 
      word = words[i]
      inputLine[j] = self:getIndex(word)
      inputLine[window + j] = self:getSuffix(word)
      inputLine[2 * window + j] = self:getCapitalized(word)
    end
    j = j + 1
  end

  
  -- Add on one-hot features.
  --TODO
  j = (3 * window) + 1
  if (self.numberOfSpecificWordFeatures > 0) then
    for i = index - windowBackward, index + windowForward do
      if i > 0 and words[i] ~= nil then
         --Words are ordered by frequency, except for some special words at the start. Include a feature if the word is one of the N most frequent words.
         local wordIndex = self.frequentWordIndex[words[i]]
         if wordIndex then
           inputLine[j + wordIndex - 1] = 1
         end
      end
      j = j + self.numberOfSpecificWordFeatures
    end
  end

  if (self.numPOS > 0) then
    for i = index - windowBackward, index + windowForward do
      if i > 0 and posTags[i] ~= nil then

        posIndex = self.posToIndex[posTags[i]]
        inputLine[j + posIndex - 1] = 1
      end
      j = j + self.numPOS
    end
  end

  return inputLine
end

function Features:getIndex(word) 
  normalized = self:normalize(word)
  index = self.wordToIndex[normalized]

  if index == nil then
    hyphenIndex = normalized:match'^.*()-'

    if hyphenIndex ~= nil and hyphenIndex < string.len(normalized) then
      return self:getIndex(string.sub(normalized, -(string.len(normalized) - hyphenIndex)))
    end

    isUpper = word:gsub("^%l", string.upper) == word
    isLower = word:gsub("^%u", string.lower) == word
    if isUpper and isLower then
       index = self.specialCharIndex
    elseif isLower then
       index = self.unknownLowerIndex
    else 
       index = self.unknownUpperIndex
    end
  end

  return index
end

function Features:getCategoryIndex(cat) 
  local result = self.categoryToIndex[cat]

  if result == nil then
    return 0
  else
    return result
  end
end

function Features:getCategoryForIndex(catIndex) 
  local result = self.indexToCategory[catIndex]
  return result
end

function Features:getWordForIndex(wordIndex) 
  local result = self.indexToWord[wordIndex]
  return result
end


function Features:getSuffix(word) 
  length = string.len(word)
  if length == 0 then
    suffix = "__"
  elseif length == 1 then
    suffix = "_" .. string.sub(word, -1)
  else 
    suffix = string.sub(word, -2)
  end
  index = self.suffixToIndex[suffix]
  if index == nil then
    return self.unknownSuffixIndex
  else
    return index
  end
end

function Features:getCapitalized(word) 
  local i = string.find(word, "^%u");
  if i ~= nil then
    return 3
  else 
    return 2
  end
end





