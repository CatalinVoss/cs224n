#!/usr/bin/env python
import numpy as np
import math
from random import randrange


epsilon = 10e-4;
def check_gradients (gradient, X, Theta, Y, decay_param):
	# for i in range (Theta.shape[0]):
	# 	for j in range (Theta.shape[1]):
	#pick a random i and a random j
	i = randrange (Theta.shape[0])
	j = randrange (Theta.shape[1])

	theta_plus = np.matrix(Theta);
	theta_plus[i, j] += epsilon;
	theta_minus = np.matrix(Theta);
	theta_minus[i, j] -= epsilon;
	
	plus = cost_function (X, theta_plus, Y, decay_param);
	minus = cost_function (X, theta_minus, Y, decay_param);

	numerical  = (plus - minus) / float(2*epsilon);

	if abs(gradient[i, j] - numerical) > 10e-2:
		print "We fucked up... Gradient: ", gradient[i, j], " numerical: ", numerical, "\n"

def cost_function (X, theta, Y, decay_param):
	M = theta * X.transpose();
	M = np.exp(M - np.max(M))
	M = M / sum (M);
	cost = np.sum ( np.multiply(Y, np.log(M)) );

	return (-1/X.shape[0]) * cost + (decay_param/2)* np.sum(np.power(theta, 2));


#returns a num_labels by num_classes matrix Y such that 
# Y_{ij} = 1 if y^(j) = i and 0 otherwise
def compute_ground_truth_matrix (labels, num_classes):
	Y = np.matrix(np.zeros ( (num_classes, labels.shape[0]) ));
	for i in range(labels.shape[0]):
		Y[int(labels[i]), i] = 1;
	return Y;

#compute current hypothesis H_ij = e^(\theta_j^T x^(i)) / sum_k e^(\theta_k^T x^(i))
def compute_hypothesis (X, Theta):
	H = Theta * X.transpose();
	H = np.exp(H - np.max(H)); #avoid overflow by scaling each term by - the max elem.
	H = H / sum(H);
	return H;

fudge_factor = 1e-6; #numerical stability
def softmax_regression (A, b, num_classes, decay_param, max_iters, alpha, converg_tol, verbose):
	X = np.matrix(A);
	Y = compute_ground_truth_matrix(np.matrix(b), num_classes);

	m = X.shape[0];
	n = X.shape[1];

	#add a column of ones for the intercept term
	X = np.append(np.ones( (m, 1) ) , X, 1);
	Theta = np.matrix(np.zeros ( (num_classes, n+1) ));

	#stores sum of squares of i-th dimension of j-th 
	#theta of historical gradients
	g = np.matrix(np.zeros ( (num_classes, n+1) )); 

	k = 0
	converged = False;

	while not converged and k < max_iters:
		if verbose:
			print "Starting iteration ", k

		H = compute_hypothesis(X, Theta); #batch gradient descent
		grad = (-1/m) * (Y - H) * X + (decay_param * Theta);
		# check_gradients (grad, X, Theta, Y, decay_param) #comment out during training/testing...

		g += np.power(grad, 2);#update sum of the squares of historical gradient at step t for j
		adjusted_gradient = np.divide(grad, fudge_factor + np.power(g, 0.5))

		#Theta = Theta - alpha * grad;
		prev_theta = np.matrix(Theta);
		Theta = Theta - alpha * adjusted_gradient;

		
		if np.sum(np.power(Theta - prev_theta, 2)) < converg_tol:
			converged = True;

		
		k += 1
		if verbose:
			print np.sum(np.power(Theta - prev_theta, 2))

	return Theta;




