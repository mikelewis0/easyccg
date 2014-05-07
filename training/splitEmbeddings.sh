cut -d' ' -f1 $1/embeddings.raw > $1/embeddings.words
cut -d' ' -f2- $1/embeddings.raw > $1/embeddings.vectors
