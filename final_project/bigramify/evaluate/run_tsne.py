#!/usr/bin/env python
import numpy as np
import load_word_vecs as word_vecs

if __name__ == '__main__':
	labels, wordVecsMatrix = word_vecs.get_matrix(word_vecs.load("../data/vectors.txt"))

	#runs tsne on wordVecsMatrix (change if we want to just look at some subset of the bigrams)
	points = tsne.tsne(wordVecsMatrix, 2, wordVecsMatrix.shape[0], 25);
	np.save("../data/tsne_coordinates", points);