#!/usr/bin/env python

import numpy as np

CORPUS_INPUT_FILENAME = '../glove/data/text8'
CORPUS_OUTPUT_FILENAME = '../glove/data/text8_bi'
VOCAB_OUTPUT_FILENAME = '../glove/data/vocab_bi.txt'
BIGRAMS_FILENAME = 'data/bigrams.txt' 

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
	with open(BIGRAMS_FILENAME) as f:  # TODO: don't hardcode this; pass as arg
		for line in f:
			arr = line.split()
			bigrams[arr[0]+'_'+arr[1]] = 0 # tokenize

	# Compute corpus occurences and modify corpus
	print 'Computing corpus occurences and creating modified corpus...'
	newdata = []
	with open(CORPUS_INPUT_FILENAME) as f:
		data = f.read().split() # split string by spaces

		prev = ""
		for w in data:
			if prev == "":
				prev = w
				newdata.append(w)
			else:
				# Candidate
				t = prev+"_"+w
				if t in bigrams:
					newdata.append(t)
					bigrams[t] += 1
				newdata.append(w)
				prev = w

	# Write updated data
	print 'Writing updated corpus: '+CORPUS_OUTPUT_FILENAME
	with open(CORPUS_OUTPUT_FILENAME, "w") as f:
		# for w in newdata:
		# 	f.write(w)
		# 	f.write(" ")
		f.write(" ".join(newdata))

	# Write bigram vocab
	print 'Writing bigram vocabulary: '+VOCAB_OUTPUT_FILENAME
	with open(VOCAB_OUTPUT_FILENAME, "w") as f:
		for t in bigrams:
			f.write(t+' '+str(bigrams[t])+'\n')

	print 'Done processing corpus'
