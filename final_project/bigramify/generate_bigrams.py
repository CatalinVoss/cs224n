#!/usr/bin/env python

import numpy as np
import random
import collections
import itertools

MAX_ITER = 1000000
CORPUS_FILENAME = '../glove/data/text8'
STOPWORDS_FILENAME = 'data/stopwords.txt'
OUTPUT_FILENAME = 'data/sampled_bigrams.txt'
<<<<<<< HEAD
N_BIGRAMS = 50000
=======
N_BIGRAMS = 30000
>>>>>>> d0194d0759abf763d4a1ac88821f8eea00b22713
EXCLUDE_STOPWORDS = True
PICK_TOP = True # whether we want the top bigrams or just any we can find...

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

		random.seed()
		data = f.read().split() # split string by spaces
		print 'Done. Beginning sampling...'
		N = len(data)

		for _ in itertools.repeat(None, MAX_ITER):
			x = random.randint(0,N-1)
			if (not EXCLUDE_STOPWORDS) or (data[x-1] not in stopwords and data[x] not in stopwords) :
				bigrams[data[x-1]+" "+data[x]] += 1

	print 'Found top bigrams. Extracting...'
	top_bigrams = []
	if PICK_TOP:
		top_bigrams = [b[0] for b in bigrams.most_common(N_BIGRAMS)]
	else:
		top_bigrams = [b[0] for b in bigrams.items()]
		random.shuffle(top_bigrams)
		top_bigrams = top_bigrams[:N_BIGRAMS] # keep arbitrary N_BIGRAMS only
	
	print 'Writing... '+OUTPUT_FILENAME
	with open(OUTPUT_FILENAME, 'w') as f:
		for item in top_bigrams:
			f.write("%s\n" % item)
	print 'Done.'