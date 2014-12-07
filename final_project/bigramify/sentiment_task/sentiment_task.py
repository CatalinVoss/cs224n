#!/usr/bin/env python
import numpy as np
import softmax_regression as softmax
import argparse

datapath = ""

parser = argparse.ArgumentParser();
parser.add_argument ("--verbose", type = bool, default = False);
parser.add_argument ("--trainMatrix", type = str, default = 'unigram_avg_trainSentencesMatrix.npy')
parser.add_argument ("--trainLabels", type = str, default = '3classTrainLabels.npy')
parser.add_argument ("--testMatrix", type = str, default = 'unigram_avg_testSentencesMatrix.npy')
parser.add_argument ("--testLabels", type = str, default = '3classDevLabels.npy')
parser.add_argument ("--num_classes", type = int, default = 3) #note we only handle 3 and 5 class for now...

#default values found by cross validation on dev set for reference unigrams
parser.add_argument ("--converg_tol", type = float, default = 1e-7);
parser.add_argument ("--reg_param", type = float, default = 1e-4);
parser.add_argument ("--max_iters", type = int, default = 10000);
parser.add_argument ("--alpha", type = float, default =  0.05);


def get3class(pred):
	if pred == 0 or pred == 1:
		return 0;
	elif pred == 2:
		return 1;
	else:
		return 2;

def make_predictions (Theta, testMat):
	H = softmax.compute_hypothesis (testMat, Theta);

	preds = [];
	for i in range (testMat.shape[0]):
		pred = np.argmax(H[:, i]);
		preds.append(pred);

	return np.matrix(preds).transpose();

def evaluate_predictions (predictions, groundTruth, num_classes):
	num_correct = 0.;
	for i in range(groundTruth.shape[0]):
		if num_classes == 3:
			if (get3class(predictions[i]) == get3class(groundTruth[i])):
				num_correct += 1;
		else:
			if (predictions[i] == groundTruth[i]):
				num_correct += 1;

	return float(num_correct)/groundTruth.shape[0]; #percent total accuracy



if __name__ == "__main__":
	args = parser.parse_args()
	trainMatrix = datapath + args.trainMatrix
	trainLabels = datapath + args.trainLabels
	testMatrix = datapath + args.testMatrix
	testLabels = datapath + args.testLabels
	num_classes = args.num_classes

	#load training data
	X = np.load(trainMatrix);
	y = np.load(trainLabels);

	#train softmax regression
	Theta = softmax.softmax_regression (X, y, num_classes, args.reg_param, args.max_iters, \
																		 args.alpha, args.converg_tol, args.verbose);

	#load test data
	testMatrix = np.load(testMatrix)
	testMatrix = np.append(np.ones( (testMatrix.shape[0],1) ), testMatrix, 1) #append column of 1s for intercept term
	testLabels = np.load(testLabels)	


	predictions = make_predictions (Theta, testMatrix)
	print "Accuracy is ", evaluate_predictions (predictions, testLabels, num_classes)