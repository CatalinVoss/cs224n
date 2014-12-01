#!/usr/bin/env python

import heapq as hq
import numpy as np


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
			neighbors.append(idx);

		neighbors[target] = arr

	return neighbors