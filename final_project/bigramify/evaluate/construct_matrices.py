#!/usr/bin/env python

import numpy as np

word2vec_file = 'vectors.840B.300d.txt';

word_vectors = {};

def get_word_vectors ():
	f = open (word2vec_file);
	for line in f:
		arr = line.split ();
		word = arr[0];
		vector = [float(x) for x in arr[1:]];
		word_vectors[word] = np.array(vector);

def build_vectors (filename):
	vector_representations = []
	f = open (filename);
	for line in f:
		sentence = line.split()[1:];

		avg = np.zeros(300);

		num_vectors = 0;

		for word in sentence:
			if word in word_vectors: ##we ignore words that don't have vector representations
				avg = avg + word_vectors[word];
				num_vectors += 1;

		# avg = avg / num_vectors;
		vector_representations.append (avg);

	return vector_representations;



if __name__ == "__main__":
	get_word_vectors ();
	files = ['trainSentences.txt', 'testSentences.txt', 'devSentences.txt'];

	for f in files:
		vectors = build_vectors (f);
		mat = np.matrix(vectors);
		np.save("summed_" + f[:-4] + 'Matrix', mat);
