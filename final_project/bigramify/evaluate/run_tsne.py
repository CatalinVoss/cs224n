#!/usr/bin/env python
import numpy as np
import load_word_vecs as word_vecs
import bh_tsne.bhtsne as tsne

if __name__ == '__main__':
	labels, wordVecsMatrix = word_vecs.get_matrix(word_vecs.load("../data/vectors.txt"))

	# #runs tsne on wordVecsMatrix (change if we want to just look at some subset of the bigrams)
	points = tsne.bh_tsne(wordVecsMatrix);
	# np.save("../data/tsne_coordinates", points);