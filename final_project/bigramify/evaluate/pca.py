#!/usr/bin/env python

import numpy as np
import load_word_vecs as word_vecs
from sklearn.decomposition import PCA
# import matplotlib.pyplot as plt
# from mpl_toolkits.mplot3d import Axes3D
import test_bigrams as tb
import matplotlib
import pylab as pl

import train_neural_network as tnn


if __name__ == '__main__':
	vecs = word_vecs.load("../data/vectors.txt")
	labels, wordVecsMatrix = word_vecs.get_matrix(vecs)
	pca = PCA(n_components=2)


	# pca.fit(wordVecsMatrix);
	# reduced_X = pca.transform(wordVecsMatrix)

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
	words = ["red_car_l2", "green_car_l2", "red", "green", "rocket", "red_rocket_avg", "red_rocket_l2", "green_rocket_avg", "green_rocket_l2", "car", "red_car_avg", "green_car_avg"] 
	# adj = ["", "big", "fast"]
	# noun =  ["car", "dog"]
	# words = adj + noun
	# for a in adj:
	# 	for n in noun:
	# 		words.append(a+"_"+n+"_avg")
	# 		words.append(a+"_"+n+"_cos")
	# 		words.append(a+"_"+n+"_l2")


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
		elif "l2" in word:
			tokens = word.split('_')
			B.append(tb.local_knn_pred(vecs, tokens[0], tokens[1], "norm"))
		elif "cos" in word:
			tokens = word.split('_')
			B.append(tb.local_knn_pred(vecs, tokens[0], tokens[1], "cos"))
		elif "svr" in word:
			tokens = word.split('_')
			B.append(tnn.pred(vecs[tokens[0]], vecs[tokens[1]]))

	B = np.matrix(B)

	# add other non-plotted bigrams to the matrix before running PCA
	B = np.concatenate((B, wordVecsMatrix[1:500, :]))
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
	X = list(B[0:len(words), 0])
	Y = list(B[0:len(words), 1])


	pl.scatter(X, Y, 20)
	pl.xlabel("First Principal Component")
	pl.ylabel ("Second Principal Component")
	pl.title("Unigram Averaging versus Optimal Bigram Blending (L2 Distance)")


	for i, word in enumerate(words):
		pl.annotate("%s" % word, xy = (float(X[i]), float(Y[i])), size = 10)

	pl.show()









