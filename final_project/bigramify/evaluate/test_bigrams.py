#!/usr/bin/env python

##
# This file is unfortunately poorly named, but is the core of our prediction routine 
# for estimate a bigram representation from constituent unigrams
#
import numpy as np
import heapq as hq
import nearest_neighbors as nn
import Queue as q
import os

best_token_number = 5
#path to the list of seed set bigrams
seed_bigrams = os.path.expanduser('~/cs224n/cs224n/final_project/bigramify/data/vocab_bi.txt')

#read the seed set bigrams into a list
referenceBigrams = [line.split()[0] for line in open(seed_bigrams) if int(line.split()[1]) >= 5]

######
#Helpers
######
def optimal_blending_objective (vecs, b, v1, v2, objective):
	if objective == "cosine":
		return (np.dot(v1, vecs[b[0]]) / (np.linalg.norm(v1) * np.linalg.norm(vecs[b[0]]))) + \
					 (np.dot(v2, vecs[b[1]]) / (np.linalg.norm(v2) * np.linalg.norm(vecs[b[1]])))
	else: #default is the \ell_2 norm
		#negative since this is a minimization problem
		return - (np.linalg.norm(v1 - vecs[b[0]]) + np.linalg.norm(v2 - vecs[b[1]]))


def get_top_bigrams (vecs, v1, v2, objective):
	best_tokens = [] 
	for bigram in referenceBigrams:
		b = bigram.split("_")
		if b[0] in vecs and b[1] in vecs and bigram in vecs:
			obj_val =  optimal_blending_objective(vecs, b, v1, v2, objective)

			if len(best_tokens) < best_token_number:
				hq.heappush (best_tokens, (obj_val, b))
			elif min(best_tokens)[0] < (obj_val):
				hq.heapreplace(best_tokens, (obj_val, b)) #removes the minimum and pushes the new token
		
	return [hq.heappop(best_tokens) for i in range(best_token_number)]

#Optimal Bigram Blend Prediction. OBJECTIVE should be either 'cosine' or 'norm' (norm by default)
def local_knn_pred (vecs, w1, w2, objective):
	#if the token is a trained_bigram, return that token
	if w1 + "_" + w2 in vecs:
		return vecs[w1 + "_" + w2]

	v1 = vecs[w1]
	v2 = vecs[w2]
	
	best_tokens = get_top_bigrams(vecs, v1, v2, objective)

	weights = [token[0] for token in best_tokens]
	best_tokens = [token[1] for token in best_tokens]

	max_weight = max(weights) + 0.01 #avoid division by 0

	#average over nearest bigrams
	avg = np.zeros(300)
	for idx, token in enumerate(best_tokens):
		avg += (weights[idx] / max_weight)*(vecs[token[0] + "_" + token[1]] - vecs[token[0]] - vecs[token[1]] + v1 + v2)

	return avg / float (np.linalg.norm(weights))

#computes the objective for a single nearest neighbor and returns the nearest neighbors
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


#local least squares prediction... Empirical results are quite poor
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



