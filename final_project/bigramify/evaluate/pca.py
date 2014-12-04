#!/usr/bin/env python

import numpy as np
import load_word_vecs as word_vecs
from sklearn.decomposition import PCA
# import matplotlib.pyplot as plt
# from mpl_toolkits.mplot3d import Axes3D
import test_bigrams as tb
import matplotlib
import pylab as pl


if __name__ == '__main__':
	vecs = word_vecs.load("../data/vectors.txt")
	pca = PCA(n_components=2)


	# pca.fit(wordVecsMatrix);
	# reduced_X = pca.transform(wordVecsMatrix)
	# labels, wordVecsMatrix = word_vecs.get_matrix(vecs)

	# print "Running PCA"
	# pca = PCA(n_components=2)
	# pca.fit(wordVecsMatrix);
	# reduced_X = pca.transform(wordVecsMatrix)
	# fig = pl.figure()
	# ax = fig.add_subplot(111, projection='3d')

	#plot full data
	# ax.scatter(reduced_X[:, 0], reduced_X[:, 1], reduced_X[:, 2])
	# plt.show()

	#words to plot
	# words = ["great", "decent", "good", "horrible", "bad", "movie", "book", "friend", "great_"]
	adj = ["small", "big", "fast"]
	noun =  ["car", "dog"]
	words = adj + noun
	for a in adj:
		for n in noun:
			words.append(a+"_"+n+"_avg")
			words.append(a+"_"+n+"_knn")

	#runs pca on the smaller matrix we construct....
	####################
	B = []
	for word in words:
		if word in vecs:
			B.append(vecs[word])
		elif "avg" in word:
			tokens = word.split('_')
			avg = (vecs[tokens[0]] + vecs[tokens[1]]) / 2.0
			B.append(avg)
		elif "knn" in word:
			tokens = word.split('_')
			B.append(tb.local_knn_pred(vecs, tokens[0], tokens[1]))

	B = np.matrix(B)
	print "Running PCA"
	pca.fit(B)
	B = pca.transform(B)
	#######################

	#########################
	#concatenates vectors onto the larger matrix and runs PCA on the result
	# for word in words:
	# 	if "avg" in word:
	# 		tokens = word.split('_')
	# 		avg = (vecs[tokens[0]] + vecs[tokens[1]]) / 2.0
	# 		vecs[word] = avg
	# 	elif "knn" in word:
	# 		tokens = word.split('_')
	# 		knn_vec = tb.local_knn_pred(vecs, tokens[0], tokens[1]);
	# 		vecs[word] = knn_vec

	# labels, wordVecsMatrix = word_vecs.get_matrix(vecs)

	# print "Running PCA"
	# pca.fit(wordVecsMatrix);
	# reduced_X = pca.transform(wordVecsMatrix)


	# B = []
	# for word in words:
	# 	B.append(reduced_X[labels.index(word), :])

	# B = np.matrix(B)


	#########################

	print "PCA finished. Plotting data..."
	X = list(B[:, 0])
	Y = list(B[:, 1])
	pl.scatter(X, Y, 20)
	pl.xlabel("First Principal Component")
	pl.ylabel ("Second Principal Component")
	pl.title("Naive Averaging versus Local Nearest Neighbors Algorithm")


	for i, word in enumerate(words):
		pl.annotate("%s" % word, xy = (float(X[i]), float(Y[i])), size = 10)

	pl.show()









