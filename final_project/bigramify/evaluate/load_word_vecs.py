#!/usr/bin/env python

import numpy as np

def load(filename):
	word_vectors = {};
	f = open (filename)

	for line in f:
		arr = line.split()
		word = arr[0]
		vector = [float(x) for x in arr[1:]]
		word_vectors[word] = np.array(vector)

	return word_vectors
