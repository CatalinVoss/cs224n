#!/usr/bin/env python
import numpy as np
import sys
sys.path.insert(0, '../../')
sys.path.insert(0, '../../evaluate')
sys.path.insert(0, 'evaluate/')
sys.path.insert(0, 'data/')
import train_bigram_approximation as ap
import test_bigrams as tb
import argparse
import linecache
import train_neural_network as tnn
import os

parser = argparse.ArgumentParser();
parser.add_argument ("--batch", type = bool, default = False) #specify for batch construction on barley
parser.add_argument ("--start_idx", type = int)
parser.add_argument ("--end_idx", type = int)
parser.add_argument ("--sentence_file", type = str)

path = "10m_norm_bigramBatch/"
VOCAB_FILENAME = 'data/vocab_bi.txt'
VOCAB_THRESH = 5 # needs to be at least that of GloVe (otherwise the vocab bigrams won't occur in the vectors file)
VECTORS_FILENAME = 'data/vectors.txt'


word_vectors = {}

def get_word_vectors ():
	print "Constructing dictionary of word vectors..."
	f = open (VECTORS_FILENAME);
	for line in f:
		arr = line.split ();
		word = arr[0];
		vector = [float(x) for x in arr[1:]];
		word_vectors[word] = np.array(vector);


#least squares prediction
# def predict (word1, word2):
# 	return ap.predict(word_vectors[word1], word_vectors[word2])

#local nearest neighbors prediction with norm
def predict (word1, word2):
	return tb.local_knn_pred(word_vectors, word1, word2, "norm")

#cosine similarity prediction
# def predict (word1, word2):
# 	return tb.local_knn_pred(word_vectors, word1, word2, "cosine")

# # svr prediction
# def predict (word1, word2):
# 	return tnn.predict (word_vectors[word1], word_vectors[word2])

def build_vectors (filename):
	print "Building matrix representation of ", filename, "using bigrams..."
	vector_representations = []
	f = open (filename);
	sentence_number = 1
	for line in f:
		sentence = line.split()[1:];

		#stupidly construct representation of ABCD as AB BC CD.. for now (things to try: remove stopwords, AB CD, etc)
		avg = np.zeros(300);
		bigrams = [b for b in zip(sentence[:-1], sentence[1:])]
		
		num_vectors = 0.0;
		for b in bigrams:
			word1 = str(b[0])
			word2 = str(b[1])
			if word1 + "_" + word2 in word_vectors:
				avg += word_vectors[str(b[0] + "_" + b[1])] #use the glove bigram
				num_vectors += 1;
			elif word1 in word_vectors and word2 in word_vectors:
				# avg += predict(word_vectors[word1], word_vectors[word2]) #use our predicted bigram
				avg += predict(word1, word2)
				num_vectors += 1;
			if word1 in word_vectors:
				avg += word_vectors[word1]
				num_vectors += 1;
			elif word2 in word_vectors:
				avg += word_vectors[word2]
				num_vectors += 1;

			
		if num_vectors != 0:
			avg = avg / float(num_vectors);

		print avg

		vector_representations.append (avg);

		print "Finished test sentence number", sentence_number
		sentence_number += 1

	return vector_representations;


def build_vector_subset (filename, start, end):
	print "Building matrix representation of ", filename, "using bigrams..."
	vector_representations = []
	for line_num in range(start, end):
		line = linecache.getline(filename, line_num)
		sentence = line.split()[1:];

		#stupidly construct representation of ABCD as AB BC CD.. for now (things to try: remove stopwords, AB CD, etc)
		avg = np.zeros(300);
		bigrams = [b for b in zip(sentence[:-1], sentence[1:])]
		
		num_vectors = 0.0;
		for b in bigrams:
			word1 = str(b[0])
			word2 = str(b[1])
			if word1 + "_" + word2 in word_vectors:
				avg += word_vectors[str(b[0] + "_" + b[1])] #use the glove bigram
				num_vectors += 1;
			elif word1 in word_vectors and word2 in word_vectors:
				# avg += predict(word_vectors[word1], word_vectors[word2]) 
				avg += predict(word1, word2) #use our predicted bigram
				num_vectors += 1;
			if word1 in word_vectors:
				avg += word_vectors[word1]
				num_vectors += 1;
			elif word2 in word_vectors:
				avg += word_vectors[word2]
				num_vectors += 1;

			
		if num_vectors != 0:
			avg = avg / float(num_vectors);

		vector_representations.append (avg);

		print "Finished ", filename, "number", line_num

	return vector_representations;


if __name__ == "__main__":
	args = parser.parse_args()
	batch = args.batch

	get_word_vectors ();

	if batch:
		start_idx = args.start_idx
		end_idx = args.end_idx
		sentence_file = args.sentence_file
		vectors = build_vector_subset (sentence_file, start_idx, end_idx);
		mat = np.matrix(vectors);
		np.save(path + str(start_idx) + "_" + str(end_idx) + "_" + "pred_bigrams_" + sentence_file[:-4] + 'Matrix', mat);
	else:
		files = ['trainSentences.txt', 'testSentences.txt', 'devSentences.txt'];
		# ap.train(VECTORS_FILENAME, VOCAB_FILENAME, VOCAB_THRESH)
		for f in files:
			vectors = build_vectors (f);
			mat = np.matrix(vectors);
			np.save("tb_bigrams_" + f[:-4] + 'Matrix', mat);