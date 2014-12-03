#!/usr/bin/env python
import numpy as np
import heapq as hq
import nearest_neighbors as nn
import Queue as q
import os

bestTokenNum = 3
vocabPath = os.path.expanduser('~/cs224n/cs224n/final_project/bigramify/data/vocab_bi.txt')
# threshold = 1 could be tuned so that we only look at neighbor with an objective \leq threshold
referenceBigrams = [line.split()[0] for line in open(vocabPath) if int(line.split()[1]) >= 5]

def simple_prediction(wordMatrix, labels, vecs, w1, w2):
	v1 = vecs[w1]
	v2 = vecs[w2]	
	best_tok = ""
	best_val = 100000000
	for bigram in referenceBigrams:
		b = bigram.split("_")
		obj_val = np.linalg.norm(v1 - vecs[b[0]]) + np.linalg.norm(v2 - vecs[b[1]])
		if obj_val < best_val:
			best_tok = bigram
			best_val = obj_val

	tok = best_tok.split("_")
	test = vecs[best_tok] - vecs[tok[0]] - vecs[tok[1]] + v1 + v2
	return nn.get_nns (wordMatrix, labels, test, 5)


def get_top_bigrams (vecs, v1, v2):
	best_tokens = []
	for bigram in referenceBigrams:
		b = bigram.split("_")
		if b[0] in vecs and b[1] in vecs and bigram in vecs:
			obj_val = np.linalg.norm(v1 - vecs[b[0]]) + np.linalg.norm(v2 - vecs[b[1]]) # todo!!+ frequency term

			#-obj_value ensures the smallest elements are those w/ largest objective
			if len(best_tokens) < bestTokenNum:
				hq.heappush (best_tokens, (-obj_val, b))
			elif min(best_tokens)[0] < (-obj_val):
				hq.heapreplace(best_tokens, (-obj_val, b)) #removes the mininmum (i..e largest obj_value) and pushes the new token
		
	return [hq.heappop(best_tokens)[1] for i in range(bestTokenNum)]

def local_knn_pred (vecs, w1, w2):
	v1 = vecs[w1]
	v2 = vecs[w2]
	#find the NUM_BEST_TOKENS best tokens
	best_tokens = get_top_bigrams(vecs, v1, v2)

	#average over nearest bigrams
	avg = np.zeros(50)
	for token in best_tokens:
		avg += vecs[token[0] + "_" + token[1]] - vecs[token[0]] - vecs[token[1]] + v1 + v2

	return avg / float (len(best_tokens))

def local_ls_pred(vecs, w1, w2):
	v1 = vecs[w1]
	v2 = vecs[w2]

	#find the NUM_BEST_TOKENS best tokens
	best_tokens = get_top_bigrams(vecs, w1, w2)

	#train a least-norm matrix approximation for this cluster
	vecDimension = vecs[best_tokens[0][0]].shape[0]
	numExamples = len(best_tokens)
	#construct data matrix for B
	B = np.zeros( (numExamples, 2 * vecDimension) )
	for idx, token in enumerate(best_tokens):
		row = np.concatenate ((vecs[token[0]], vecs[token[1]]))
		B[idx] = row
	B = np.matrix(B)

	#construct matrix Y with true labels
	Y = np.zeros ( (numExamples, vecDimension) )
	for idx, token in enumerate(best_tokens):
		ref = vecs[token[0] + "_" + token[1]]
		Y[idx] = ref
	Y = np.matrix(Y)

	#form A by a sequence of least-norm problems... one for each column
	#A is a num_examples x 2*vector dimension matrix
	A = np.zeros( (vecDimension, 2 * vecDimension) )
	pinvB = np.linalg.pinv(B)
	for idx in range(A.shape[0]):
		ref_i = Y[:, i] #get the i-th coordinate
		row = pinvB * ref_i; #estimate the column via psuedo-inverse
		A[idx] = np.transpose(row)

	A_hat = np.matrix(A)

	unigrams = np.matrix(np.concatenate((v1, v2))).T
	bigram_vec = (A_hat * unigrams)

	return np.array(bigram_vec.T)[0]



