#!/usr/bin/env python

import heapq as hq
import numpy as np


def nearest_neighbor_query(X, labels, word, k):
	n_dict = nearest_neighbor_search(X, [labels.index(word)], k)
	return [labels[idx] for idx in n_dict[labels.index(word)]]

def nearest_neighbor_average (X, labels, word1, word2, k):
	target = (X[labels.index(word1), :] + X[labels.index(word2), :]) / 2.0;
	
	nn = []
	for j in range(0, X.shape[0]):
			dist = np.linalg.norm(target - X[j, :])
			hq.heappush(nn, (dist, j))

	neighbors = []
	for j in range(0, k):
		distance, idx = hq.heappop(nn)
		neighbors.append(labels[idx]);

	return neighbors


# Returns the K nearest neigbors for each of the INDICES of X, 
# where each row of X corresponds to an n-gram vector.
# 
# Parameters: matrix X of vectors, INDICES list of candidates, K number of nearest neighbors
#
# Returns: Dictionary with keys given by the indices and values being a list of the indices
#					 of nearest neighbors 
#
def nearest_neighbor_search (X, indices, k):
	m = X.shape[0];

	neighbors = {}

	#Uses brute force for now...
	for target in indices:
		nn = []
		targ = X[target, :]
		for j in range(0, m):
			dist = np.linalg.norm(targ - X[j, :])
			hq.heappush(nn, (dist, j))

		arr = []
		for j in range(0, k):
			distance, idx = hq.heappop(nn)
			arr.append(idx);

		neighbors[target] = arr

	return neighbors