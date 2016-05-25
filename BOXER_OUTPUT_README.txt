The Boxer output (-o boxer) is a modification of the Prolog output, with the following changes:

- Terminal nodes are printed directly inside the tree, rather than just referenced
- Made terminal nodes a ternary predicate with its category, string and list containing all other features
- Input no longer needs to be POS & NER tagged, but these features will still appear in the output if available
- Changed Prolog file header
- Changed several rule names (lex --> lx, bx --> bxc, gbx --> gbxc)
- Escaped single quotes within words and lemmas

Additionally, some minor format changes were made:
- all categories are printed in lowercase and no longer appear within single quotes
- changed the "." category to "period"
- grammatical features are expressed with colons rather than brackets (s[dcl] --> s:dcl)