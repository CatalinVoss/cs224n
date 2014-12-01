#!/usr/bin/env python
import numpy as np
import load_word_vecs as word_vecs
from sklearn.cluster import KMeans

if __name__ == '__main__':
	vecs = word_vecs.load("../data/vectors.txt")
	labels, wordVecsMatrix = word_vecs.get_matrix(vecs)

	print "Running K means clustering..."
	kmeans = KMeans(n_clusters = 500)
	kmeans.fit(wordVecsMatrix)
	clusters = kmeans.predict(wordVecsMatrix)

	clusterMap = {}
	for i in range(0, len(labels)):
		cluster = clusters[i]		
		if cluster not in clusterMap.keys():
			clusterMap[cluster] = [labels[i]]
		else:
			clusterMap[cluster].append(labels[i])

	#output the clusters...
	for key in clusterMap.keys():
		print clusterMap[key]



