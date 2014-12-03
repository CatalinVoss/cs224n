#!/usr/bin/env python

import numpy as np
import random
import collections
import itertools
import load_word_vecs as word_vecs
import neurolab as nl
from sklearn import linear_model, datasets

# Load vocab bigrams
def load_bigrams(vocab_filename, vocab_thresh):
	bigrams = []
	for line in open(vocab_filename):
		if int(line.split()[1]) >= vocab_thresh:
			bigrams.append(line.split()[0])
	return bigrams

def get_training_data(vectors_filename, vocab_filename, vocab_thresh):
	bigrams_vocab = load_bigrams(vocab_filename, vocab_thresh);
	word_vectors = word_vecs.load(vectors_filename);
	n = 50
	N = len(bigrams_vocab)

	# Construct matrix B row-wise
	print 'Constructing matrix B row-wise'
	B = np.zeros(shape=(N,2*n))
	for idx, label in enumerate(bigrams_vocab):
		b_1 = word_vectors[label.split('_')[0]]
		b_2 = word_vectors[label.split('_')[1]]
		row = np.concatenate((b_1,b_2))
		B[idx] = row
	# B = np.matrix(B)

	# Construct Y Matrix (an individual column from this will make for a y-vector)
	print 'Constructing matrix Y row-wise'
	Y = np.zeros(shape=(N,n))
	for idx, label in enumerate(bigrams_vocab):
		row = word_vectors[label]
		Y[idx] = row

	# Cut off some rows...
	B = B[0:10]
	Y = Y[0:10]

	return B, Y

def train(vectors_filename, vocab_filename, vocab_thresh):
	B, Y = get_training_data(vectors_filename, vocab_filename, vocab_thresh)

	# # Neural Network
	# n = 50
	# min_input = 0
	# max_input = 10
	# hidden_layers = [70, n] # 105
	# # Create feed forward perceptron network with 2 inputs
	# print 'Creating feed forward perceptron network with 2 inputs'
	# net = nl.net.newff( [[min_input, max_input]]*(n*2), hidden_layers )
	# print 'Training network'
	# err = net.train(B, Y, show=1, epochs=10, goal=0.1)
	# print 'Error: '+err



if __name__ == '__main__':
	# Constants
	VOCAB_FILENAME = '../../glove/data/vocab_bi.txt'
	VOCAB_THRESH = 5 # needs to be at least that of GloVe (otherwise the vocab bigrams won't occur in the vectors file)
	VECTORS_FILENAME = '../../glove/data/vectors.txt'

	# Tain:
	print 'Training bigram net'
	train(VECTORS_FILENAME, VOCAB_FILENAME, VOCAB_THRESH)
