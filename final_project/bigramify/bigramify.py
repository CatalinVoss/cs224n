#!/usr/bin/env python

import numpy as np

def find_all(a_str, sub):
    start = 0
    while True:
        start = a_str.find(sub, start)
        if start == -1: return
        yield start
        start += len(sub) # use start += 1 to find overlapping matches

if __name__ == '__main__':
	# Read bigrams hash
	print 'Loading bigrams...'
	bigrams = {}
	with open('data/bigrams.txt') as f:  # TODO: don't hardcode this; pass as arg
		for line in f:
			arr = line.split()
			bigrams[arr[0]+'_'+arr[1]] = 0 # tokenize

	# Compute corpus occurences and modify corpus
	# TODO: use nice python counters for this stuff (John, you python wiz, feel free to rewrite any of this)
	print 'Computing corpus occurences and creating modified corpus...'
	data = ""
	with open('../glove/text8') as f:
		data = f.read()

		for t in bigrams:
			arr = t.split('_');
			l = list(find_all(data, arr[0]+' '+arr[1])) # array of indices

			# Count
			bigrams[t] += len(l)

			# Adjust string: add bigram in the middle
			data = data.replace(arr[0]+' '+arr[1], arr[0]+' '+t+' '+arr[1])

			# If we need just the counts:
			# bigrams[t] += data.count(t.split('_')[0]+' '+t.split('_')[1])

	# Write updated data
	print 'Writing updated corpus: ../glove/text8_bi'
	with open("../glove/text8_bi", "w") as f:
		f.write(data)

	# Write bigram vocab
	print 'Writing bigram vocabulary: ../glove/vocab_bi.txt'
	with open("../glove/vocab_bi.txt", "w") as f:
		for t in bigrams:
			f.write(t+' '+str(bigrams[t])+'\n')

	# TODO: Augment vocab files if required

	print 'Done processing corpus'

	# Run GloVe

