#!/usr/bin/env python
import numpy as np

reference = 'reference_unigrams.txt';
word_vectors = {};

def get_word_vectors ():
	print "Constructing dictionary of word vectors..."
	f = open (reference);
	for line in f:
		arr = line.split ();
		word = arr[0];
		vector = [float(x) for x in arr[1:]];
		word_vectors[word] = np.array(vector);

def build_vectors (filename):
	print "Building matrix representation of ", filename, "by averaging..."
	vector_representations = []
	f = open (filename);
	for line in f:
		sentence = line.split()[1:];

		avg = np.zeros(50);
		num_vectors = 0;
		for word in sentence:
			if word in word_vectors: ##we ignore words that don't have vector representations
				avg = avg + word_vectors[word];
				num_vectors += 1;


		if num_vectors != 0:
			avg = avg / float(num_vectors);

		vector_representations.append (avg);

	return vector_representations;



if __name__ == "__main__":
	get_word_vectors ();
	files = ['trainSentences.txt', 'testSentences.txt', 'devSentences.txt'];

	for f in files:
		vectors = build_vectors (f);
		mat = np.matrix(vectors);
		np.save("unigram_avg_" + f[:-4] + 'Matrix', mat);
