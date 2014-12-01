#!/usr/bin/env python

import numpy as np
import random
import collections
import itertools

MAX_ITER = 1000000
CORPUS_FILENAME = '../glove/data/text8'
STOPWORDS_FILENAME = 'data/stopwords.txt'
N_BIGRAMS = 2000
EXCLUDE_STOPWORDS = True

# Randomized algorithm to identify k most common bigrams:
#	Pick bigrams uniformly at random
#	Sort by # of occurences in random pick
#
# Note that this is super unoptimized and kinda stupid if EXCLUDE_STOPWORDS is turned on,
# because the probability that we add stuff a sufficient number of times is pretty low.
# Note that this also doesn't guarantee that we actually return N_BIGRAMS (although in our
# experiments that has mostly been the case).

if __name__ == '__main__':
	stopwords = [line.strip() for line in open(STOPWORDS_FILENAME)]

	# Generate bigrams by picking uniformly random ones from the corpus
	bigrams = collections.Counter()
	with open(CORPUS_FILENAME) as f:
		print 'Reading corpus...'
		data = f.read().split() # split string by spaces
		print 'Done. Beginning sampling...'
		N = len(data)

		random.seed()

		for _ in itertools.repeat(None, MAX_ITER):
			x = random.randint(0,N-1)
			if (not EXCLUDE_STOPWORDS) or (data[x-1] not in stopwords and data[x] not in stopwords) :
				bigrams[data[x-1]+"_"+data[x]] += 1

	print bigrams.most_common(N_BIGRAMS)