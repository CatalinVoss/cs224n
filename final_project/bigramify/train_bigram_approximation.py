#!/usr/bin/env python

import sys
sys.path.insert(0, 'evaluate')

import numpy as np
import random
import collections
import itertools
import load_word_vecs as word_vecs

# Vars
A = np.matrix([])
n = 0

# Load vocab bigrams
def load_bigrams(vocab_filename, vocab_thresh):
	bigrams = []
	for line in open(vocab_filename):
		if int(line.split()[1]) >= vocab_thresh:
			bigrams.append(line.split()[0])
	return bigrams

def train(vectors_filename, vocab_filename, vocab_thresh):
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
	B = np.matrix(B)

	print 'Computing pseudoinverse'
	B_inv = np.linalg.pinv(B)

	# Construct Y Matrix (an individual column from this will make for a y-vector)
	print 'Constructing matrix Y row-wise'
	Y = np.zeros(shape=(N,n))
	for idx, label in enumerate(bigrams_vocab):
		row = word_vectors[label]
		Y[idx] = row
	Y = np.matrix(Y)

	# Recover linear approximation matrix A row-wise
	print 'Recovering linear approximation matrix A row-wise'
	global A # mark A as global -- we are gonna change it (John: how am I supposed to do this with nice class variables?!)
	A = np.zeros(shape=(n,2*n))
	for i in range(n): # 0..n-1
		y = Y[:,i]
		a_i = B_inv*y
		A[i] = np.transpose(a_i)

	# Test prediction
	# print predict(word_vectors['civil'], word_vectors['union'])

# Returns a predicted bigram vector
def predict(v1, v2):
	# TODO: John help me out here -- not so pretty with those transposes flying around
	b = np.matrix(np.concatenate((v1,v2)))
	return np.array(b*np.transpose(A))[0]

if __name__ == '__main__':
	# Constants
	VOCAB_FILENAME = '../glove/data/vocab_bi.txt'
	VOCAB_THRESH = 5 # needs to be at least that of GloVe (otherwise the vocab bigrams won't occur in the vectors file)
	VECTORS_FILENAME = '../glove/data/vectors.txt'

	# Tain:
	print 'Training bigram approximation function'
	train(VECTORS_FILENAME, VOCAB_FILENAME, VOCAB_THRESH)
