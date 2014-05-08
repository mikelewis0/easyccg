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
   
   word = string.gsub(word, "[0-9]", "#")
   word = string.gsub(word, "\\/", "/")
   word = string.lower(word)

  return word
end


function Features:fileToTable(path, minIndex, normalize)
   local file = io.open(path)

   if not file then error("Unable to load file: " .. file) end

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

function Features:__init(wordFilePath, suffixFilePath, categoryFilePath)

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

   self.numWords, self.wordToIndex, _ = self:fileToTable(wordFilePath, self.unknownLowerIndex, true)
   self.numSuffixes, self.suffixToIndex, _ = self:fileToTable(suffixFilePath, self.unknownSuffixIndex + 1, false)
   self.numCats, self.categoryToIndex, self.indexToCategory = self:fileToTable(categoryFilePath, 1, false)
   self.extraWords = 4
end

function Features:getFeatures(words, index, windowBackward, windowForward) 
  window = (windowBackward + windowForward + 1)
  inputLine = torch.Tensor(window * 3)
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
--  if word:gsub("^%l", string.lower) == word then
  if i ~= nil then
    return 3
  else 
    --local j = string.find(word, "^%u");
    return 2
  end
end





