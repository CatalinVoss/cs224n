#!/usr/bin/env python

import numpy as np
import load_word_vecs as word_vecs
from sklearn.decomposition import PCA
# import matplotlib.pyplot as plt
# from mpl_toolkits.mplot3d import Axes3D

import matplotlib
import pylab as pl



def get_matrix (vecs):
	wordVecsMatrix = [];
	labels = [];
	for key in vecs:
		labels.append(key);
		wordVecsMatrix.append(vecs[key])

	return labels, np.matrix(wordVecsMatrix)


if __name__ == '__main__':
	print "Loading word vectors"
	vecs = word_vecs.load("../data/vectors.txt")

	print "Constructing data matrix"
	labels, wordVecsMatrix = get_matrix(vecs)

	print "Running PCA"
	pca = PCA(n_components=2)
	pca.fit(wordVecsMatrix);
	reduced_X = pca.transform(wordVecsMatrix)
	# fig = pl.figure()
	# ax = fig.add_subplot(111, projection='3d')

	#plot full data
	# ax.scatter(reduced_X[:, 0], reduced_X[:, 1], reduced_X[:, 2])
	# plt.show()

	#words to plot
	words = ["civil_war", "civil", "war", "death_penalty", "death", "penalty", "united_states", "united", "states", "comic_book", "comic", "book"]

	B = []
	for word in words:
		B.append(reduced_X[labels.index(word), :])

	B = np.matrix(B)
	# pca.fit(B)
	# B = pca.transform(B)

	print "PCA finished. Plotting data..."
	X = list(B[:, 0])
	Y = list(B[:, 1])
	pl.scatter(X, Y, 20)

	for i, word in enumerate(words):
		pl.annotate("%s" % word, xy = (float(X[i]), float(Y[i])), size = 10)

	pl.show()









