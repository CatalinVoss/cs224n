#!/usr/bin/env python

import numpy as np

def get_matrix(vecs):
	print "Constructing data matrix"
	wordVecsMatrix = [];
	labels = [];
	for key in vecs:
		labels.append(key);
		wordVecsMatrix.append(vecs[key])

	return labels, np.matrix(wordVecsMatrix)


def load(filename):
	print "Loading word vectors"
	word_vectors = {};
	f = open (filename)

	for line in f:
		arr = line.split()
		word = arr[0]
		vector = [float(x) for x in arr[1:]]
		word_vectors[word] = np.array(vector)

	return word_vectors
